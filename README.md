# CoreBank — Distributed Online Banking Platform

A full-stack online banking system built as a set of independently deployable Spring Boot
microservices that talk over Kafka, with a React web client.

```
                          ┌──────────────┐
  React client ──HTTP──▶  │ API Gateway  │  JWT check, routing, circuit breakers, CORS
   (port 3000)            │  (port 8080) │
                          └──────┬───────┘
                 service discovery (Eureka, 8761) · load-balanced
     ┌──────────────┬────────────┴───────┬────────────────────┐
     ▼              ▼                    ▼                    ▼
 identity-svc   account-svc ×2     transaction-svc ×2    notification-svc
 users, OTP,    accounts, ledger,  transfer saga,        email + in-app
 login, KYC     deposits, admin    limits, beneficiaries alerts
     │              │                    │                    │
 identity_db    account_db         transaction_db       notification_db   (PostgreSQL, one DB per service)
     └──────────────┴──── Kafka (outbox → topics) ───┴────────────────────┘
```

## Banking features

| Area | What customers / staff can do |
|---|---|
| Onboarding | Register, verify email with a 6-digit OTP, log in, reset a forgotten password |
| KYC | Submit phone, address, DOB (18+), PAN and Aadhaar; IDs are AES-GCM encrypted at rest and only shown masked |
| Accounts | Apply for Savings / Current accounts (one each); bank approves or rejects; close at zero balance |
| Money movement | Deposits and withdrawals; transfers to any account or a saved beneficiary, with live status |
| Safety | Per-transfer (₹2,00,000) and daily (₹5,00,000) limits, idempotency keys, account lockout after 5 failed logins |
| Beneficiaries | Add payees after verifying the account exists (masked holder name shown) |
| Statements | Date-filtered, paginated ledger with running balance; CSV export |
| Notifications | Email + in-app alerts for credits, debits, account status and failed transfers |
| Back office | Approve/reject applications, freeze/unfreeze accounts, block customers, monitor transfers, bank totals |

## Distributed-systems design

- **Database per service.** No service reads another's tables. Data that another service needs
  (for example, KYC status) is replicated from `CustomerUpdated` events into a local read model.
- **Transactional outbox.** Every state change and the event it causes are written in one DB
  transaction. A relay publishes them to Kafka afterwards, using `FOR UPDATE SKIP LOCKED` so
  replicas can share the work.
- **Saga for transfers.** transaction-service orchestrates `Debit → Credit` across accounts,
  and compensates with a `Refund` if the credit leg fails:
  `PENDING → DEBITED → COMPLETED`, or `→ COMPENSATING → REVERSED`, or `→ FAILED`.
  A recovery job re-sends commands for sagas that stall.
- **Idempotent consumers.** Kafka delivers at least once, so account-service remembers each
  command's reply and answers duplicates without re-running them. Clients send an
  `Idempotency-Key`, so a retried request never moves money twice.
- **Consistency of balances.** Every balance change takes a row lock (`SELECT … FOR UPDATE`),
  amounts are `BigDecimal`, the DB enforces `balance >= 0`, and the ledger's
  `(reference, entry_type)` uniqueness blocks replays.
- **Resilience.** The gateway and service-to-service calls use Resilience4j circuit breakers. If
  account lookup is down, transfers still proceed and the saga decides the outcome. Records that
  keep failing go to `<topic>.DLT` after 3 retries.
- **Scalability.** account-service and transaction-service run 2 replicas each. Kafka partitions
  commands by account number, so each account's operations stay ordered.
- **Security.** Stateless HS256 JWTs are verified at the gateway *and* in each service. `/internal/**`
  is never routed externally. Passwords use BCrypt(12), and OTPs are stored hashed, expire, and are
  attempt-limited.
- **Observability.** Actuator health endpoints and distributed tracing with Micrometer and Zipkin.

## Running it

Requirements: Docker Desktop with roughly 4 GB of RAM and 6 GB of free disk.

```bash
cp .env.example .env               # change the secrets
docker compose up -d --build       # whole platform + web client
docker compose --profile tools up -d   # optional: Kafka UI (:8090) and Zipkin (:9411)
./scripts/smoke-test.sh            # end-to-end check through the gateway (needs curl + jq)
```

| URL | What |
|---|---|
| http://localhost:3000 | Web app |
| http://localhost:8080 | API gateway |
| http://localhost:8025 | Mailpit inbox (OTP and alert emails land here) |
| http://localhost:8761 | Eureka dashboard (see the replicas register) |

Default back-office login: `admin@corebank.local` / `Admin@12345` (set `ADMIN_EMAIL` /
`ADMIN_PASSWORD`).

### Developing without Docker for the apps

```bash
docker compose up -d postgres kafka mailpit discovery-server
cd services && mvn -q install -DskipTests
export KAFKA_BOOTSTRAP=localhost:29092                          # Kafka's host-facing listener
mvn -pl identity-service spring-boot:run                        # one terminal per service
cd client && npm install && npm start
```

### Tests

```bash
cd services && mvn test
```

The integration tests run each service against embedded Kafka and H2. They cover the saga state
machine and compensation, duplicate and late messages, idempotency keys, limits, concurrent
deposits, KYC gating, OTP verification, login lockout and PII encryption.

## Repository layout

```
services/               Maven multi-module backend (Java 17, Spring Boot 3.3, Spring Cloud 2023)
  common/               event contracts, JWT, outbox, error handling
  discovery-server/     Eureka
  api-gateway/          Spring Cloud Gateway
  identity-service/  account-service/  transaction-service/  notification-service/
client/                 React 18 + Tailwind web app
infra/                  Postgres init
scripts/smoke-test.sh   end-to-end test against the running stack
server/onlinebanking/   original monolith (legacy, no longer used)
legacy/client-src/      original React sources before the rewrite
```

## API overview (via gateway)

| Method & path | Purpose |
|---|---|
| `POST /api/auth/register` · `verify-email` · `resend-otp` · `login` · `forgot-password` · `reset-password` | Authentication |
| `GET /api/users/me` · `PUT /api/users/me/kyc` · `POST /api/users/me/change-password` | Profile |
| `GET/POST /api/accounts` · `GET /api/accounts/{no}` · `/statement` · `POST /deposits` · `/withdrawals` · `/close` | Accounts |
| `POST /api/transfers` (Idempotency-Key) · `GET /api/transfers` · `/{id}` · `/limits` | Transfers |
| `GET/POST /api/beneficiaries` · `DELETE /{id}` · `GET /lookup/{accountNo}` | Beneficiaries |
| `GET /api/notifications` · `/unread-count` · `POST /{id}/read` · `/read-all` | Notifications |
| `/api/admin/accounts…` · `/api/admin/users…` · `/api/admin/transfers` | Back office (ADMIN role) |

Errors use one shape everywhere: `{"code": "...", "message": "...", "fieldErrors": {...}}`.
