#!/usr/bin/env bash
# End-to-end check against a running stack (docker compose up -d --build).
# Registers two customers, completes KYC, opens and approves accounts, deposits,
# and runs a transfer through the Kafka saga across services.
set -euo pipefail

API=${API:-http://localhost:8080}
MAILPIT=${MAILPIT:-http://localhost:8025}
ADMIN_EMAIL=${ADMIN_EMAIL:-admin@corebank.local}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-Admin@12345}
RUN=$RANDOM

need() { command -v "$1" >/dev/null || { echo "missing $1"; exit 1; }; }
need curl; need jq

call() { # method path token [body] [extra-header]
  curl -sf -X "$1" "$API$2" -H "Content-Type: application/json" \
    ${3:+-H "Authorization: Bearer $3"} ${5:+-H "$5"} ${4:+-d "$4"}
}

otp_for() { # latest OTP emailed to $1: from the log in local mode, else from Mailpit
  if [ -n "${OTP_LOG:-}" ]; then
    for _ in $(seq 1 30); do
      code=$(grep -A3 "EMAIL to=$1 " "$OTP_LOG" 2>/dev/null | grep -oE 'code is: [0-9]{6}' | grep -oE '[0-9]{6}' | tail -1 || true)
      [ -n "$code" ] && { echo "$code"; return; }
      sleep 1
    done
    echo "no OTP for $1" >&2; exit 1
  fi
  for _ in $(seq 1 30); do
    id=$(curl -sf "$MAILPIT/api/v1/search?query=to:$1" | jq -r '.messages[0].ID // empty')
    if [ -n "$id" ]; then
      curl -sf "$MAILPIT/api/v1/message/$id" | jq -r '.Text' | grep -oE '[0-9]{6}' | head -1
      return
    fi
    sleep 1
  done
  echo "no OTP for $1" >&2; exit 1
}

customer() { # name -> prints "token accountNumber"
  local email="$1.$RUN@example.com"
  call POST /api/auth/register "" "{\"firstName\":\"$1\",\"lastName\":\"Test\",\"email\":\"$email\",\"password\":\"Passw0rd!\"}" >/dev/null
  call POST /api/auth/verify-email "" "{\"email\":\"$email\",\"code\":\"$(otp_for "$email")\"}" >/dev/null
  local token
  token=$(call POST /api/auth/login "" "{\"email\":\"$email\",\"password\":\"Passw0rd!\"}" | jq -r .accessToken)
  call PUT /api/users/me/kyc "$token" '{"phone":"9876543210","dateOfBirth":"1990-01-01","gender":"OTHER","addressLine":"1 Main St","city":"Pune","state":"MH","postalCode":"411001","panNumber":"ABCDE1234F","governmentId":"123412341234"}' >/dev/null
  # account-service learns about KYC asynchronously via Kafka; retry until replicated
  local acc=""
  for _ in $(seq 1 20); do
    acc=$(call POST /api/accounts "$token" '{"type":"SAVINGS"}' 2>/dev/null | jq -r .accountNumber) && break
    sleep 1
  done
  [ -n "$acc" ] || { echo "could not open account for $1" >&2; exit 1; }
  echo "$token $acc"
}

echo "== registering customers"
read -r A_TOKEN A_ACC < <(customer alice)
read -r B_TOKEN B_ACC < <(customer bob)
echo "alice=$A_ACC bob=$B_ACC"

echo "== admin approves accounts"
ADMIN=$(call POST /api/auth/login "" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" | jq -r .accessToken)
call POST "/api/admin/accounts/$A_ACC/approve" "$ADMIN" >/dev/null
call POST "/api/admin/accounts/$B_ACC/approve" "$ADMIN" >/dev/null

echo "== deposit 5000 to alice (sent twice with the same Idempotency-Key)"
call POST "/api/accounts/$A_ACC/deposits" "$A_TOKEN" '{"amount":5000}' "Idempotency-Key: dep-$RUN" >/dev/null
call POST "/api/accounts/$A_ACC/deposits" "$A_TOKEN" '{"amount":5000}' "Idempotency-Key: dep-$RUN" >/dev/null

echo "== transfer 1200 alice -> bob via saga"
TID=$(call POST /api/transfers "$A_TOKEN" "{\"fromAccount\":\"$A_ACC\",\"toAccount\":\"$B_ACC\",\"amount\":1200,\"description\":\"smoke\"}" "Idempotency-Key: tx-$RUN" | jq -r .transferId)
for _ in $(seq 1 30); do
  STATUS=$(call GET "/api/transfers/$TID" "$A_TOKEN" | jq -r .status)
  case $STATUS in COMPLETED|FAILED|REVERSED) break;; esac
  sleep 1
done
echo "transfer $TID -> $STATUS"

A_BAL=$(call GET "/api/accounts/$A_ACC" "$A_TOKEN" | jq -r .balance)
B_BAL=$(call GET "/api/accounts/$B_ACC" "$B_TOKEN" | jq -r .balance)
echo "alice=$A_BAL bob=$B_BAL"

[ "$STATUS" = COMPLETED ] && [ "$A_BAL" = "3800.0" -o "$A_BAL" = "3800.00" -o "$A_BAL" = "3800" ] \
  && [ "$B_BAL" = "1200.0" -o "$B_BAL" = "1200.00" -o "$B_BAL" = "1200" ] \
  && echo "SMOKE TEST PASSED" || { echo "SMOKE TEST FAILED"; exit 1; }
