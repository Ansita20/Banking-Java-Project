#!/usr/bin/env bash
# Stops everything started by run-local.sh. Data in .local/data and .local/kafka-data is kept.
ROOT=$(cd "$(dirname "$0")/.." && pwd)
for pidfile in "$ROOT"/.local/pids/*; do
  [ -f "$pidfile" ] || continue
  pid=$(cat "$pidfile")
  if kill -0 "$pid" 2>/dev/null; then
    pkill -P "$pid" 2>/dev/null
    kill "$pid" 2>/dev/null && echo "stopped $(basename "$pidfile")"
  fi
  rm -f "$pidfile"
done
