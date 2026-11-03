#!/usr/bin/env bash
# Runs the whole platform on this machine without Docker:
# Kafka (downloaded once), H2 databases, and every service as a local JVM.
# Emails (including OTPs) are written to .local/logs/notification-service.log.
# Stop everything with scripts/stop-local.sh.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
LOCAL="$ROOT/.local"
LOGS="$LOCAL/logs"
KAFKA_VERSION=3.8.0
KAFKA_DIR="$LOCAL/kafka_2.13-$KAFKA_VERSION"
mkdir -p "$LOGS" "$LOCAL/data" "$LOCAL/pids"

wait_for_port() { # port name
  for _ in $(seq 1 90); do
    nc -z localhost "$1" 2>/dev/null && { echo "  $2 is up on :$1"; return; }
    sleep 2
  done
  echo "  $2 did not start, see $LOGS/$2.log"; exit 1
}

start() { # name command...
  local name=$1; shift
  if [ -f "$LOCAL/pids/$name" ] && kill -0 "$(cat "$LOCAL/pids/$name")" 2>/dev/null; then
    echo "  $name already running"; return
  fi
  nohup "$@" > "$LOGS/$name.log" 2>&1 &
  echo $! > "$LOCAL/pids/$name"
}

echo "== Kafka"
if [ ! -d "$KAFKA_DIR" ]; then
  echo "  downloading Kafka $KAFKA_VERSION (one time)"
  curl -fsSL "https://archive.apache.org/dist/kafka/$KAFKA_VERSION/kafka_2.13-$KAFKA_VERSION.tgz" | tar -xz -C "$LOCAL"
fi
if [ ! -f "$LOCAL/kafka-data/meta.properties" ]; then
  sed -e "s#^log.dirs=.*#log.dirs=$LOCAL/kafka-data#" "$KAFKA_DIR/config/kraft/server.properties" > "$LOCAL/kafka.properties"
  "$KAFKA_DIR/bin/kafka-storage.sh" format -t "$("$KAFKA_DIR/bin/kafka-storage.sh" random-uuid)" \
    -c "$LOCAL/kafka.properties" > /dev/null
fi
KAFKA_HEAP_OPTS="-Xmx384m -Xms128m" start kafka "$KAFKA_DIR/bin/kafka-server-start.sh" "$LOCAL/kafka.properties"
wait_for_port 9092 kafka

echo "== building services"
(cd "$ROOT/services" && mvn -q -B install -DskipTests)

JVM_OPTS="-Xmx200m -XX:MaxMetaspaceSize=150m -XX:+UseSerialGC"
export LOCAL_DATA_DIR="$LOCAL/data" MANAGEMENT_TRACING_ENABLED=false KAFKA_BOOTSTRAP=localhost:9092
jar() { echo "$ROOT/services/$1/target/$1-1.0.0.jar"; }

echo "== services"
start discovery-server java $JVM_OPTS -jar "$(jar discovery-server)"
wait_for_port 8761 discovery-server
for svc in identity-service account-service transaction-service notification-service; do
  start "$svc" java $JVM_OPTS -Dspring.profiles.active=local -jar "$(jar $svc)"
done
start api-gateway java $JVM_OPTS -jar "$(jar api-gateway)"
wait_for_port 8081 identity-service
wait_for_port 8082 account-service
wait_for_port 8083 transaction-service
wait_for_port 8084 notification-service
wait_for_port 8080 api-gateway

echo "== web client"
cd "$ROOT/client"
[ -d node_modules ] || npm install --no-audit --no-fund --silent
BROWSER=none PORT=3000 start web npx react-scripts start
wait_for_port 3000 web

cat <<MSG

CoreBank is running.
  Web app          http://localhost:3000
  API gateway      http://localhost:8080
  Eureka           http://localhost:8761
  Admin login      admin@corebank.local / Admin@12345
  OTP emails       grep -A3 'EMAIL to=' $LOGS/notification-service.log
  Logs             $LOGS
Stop with scripts/stop-local.sh
MSG
