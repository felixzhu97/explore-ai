#!/usr/bin/env bash
# Usage: scripts/dev-up.sh [start|stop|status]
# Starts the backend (:9000) and frontend (:4200) in the background.
# Output goes to logs/ (gitignored); pids to logs/*.pid.
set -euo pipefail

cd "$(dirname "$0")/.."
LOG_DIR=logs
BACKEND_URL=http://localhost:9000/actuator/health
FRONTEND_URL=http://localhost:4200
mkdir -p "$LOG_DIR"
# A system HTTP proxy would otherwise answer localhost with 502.
export NO_PROXY="localhost,127.0.0.1${NO_PROXY:+,$NO_PROXY}" no_proxy="localhost,127.0.0.1${no_proxy:+,$no_proxy}"

is_running() {
  local pid_file="$LOG_DIR/$1.pid"
  [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null
}

wait_for() {
  local name=$1 url=$2 tries=${3:-90}
  for ((i = 0; i < tries; i++)); do
    if curl -fsS -o /dev/null "$url"; then
      echo "$name ready: $url"
      return 0
    fi
    if ! is_running "$name"; then
      echo "$name exited during startup. Fix: read the first 'Caused by' in $LOG_DIR/$name.log" >&2
      return 1
    fi
    sleep 2
  done
  echo "$name did not answer at $url. Fix: read $LOG_DIR/$name.log" >&2
  return 1
}

start_one() {
  local name=$1
  shift
  if is_running "$name"; then
    echo "$name already running (pid $(cat "$LOG_DIR/$name.pid"))"
    return
  fi
  nohup "$@" >"$LOG_DIR/$name.log" 2>&1 &
  echo $! >"$LOG_DIR/$name.pid"
}

stop_one() {
  local name=$1
  if is_running "$name"; then
    pkill -TERM -P "$(cat "$LOG_DIR/$name.pid")" 2>/dev/null || true
    kill "$(cat "$LOG_DIR/$name.pid")" 2>/dev/null || true
    echo "$name stopped"
  fi
  rm -f "$LOG_DIR/$name.pid"
}

case "${1:-start}" in
  start)
    start_one backend ./gradlew bootRun --no-daemon
    start_one frontend pnpm start
    wait_for backend "$BACKEND_URL"
    wait_for frontend "$FRONTEND_URL"
    ;;
  stop)
    stop_one frontend
    stop_one backend
    ;;
  status)
    for name in backend frontend; do
      if is_running "$name"; then echo "$name: running"; else echo "$name: stopped"; fi
    done
    ;;
  *)
    echo "Usage: $0 [start|stop|status]" >&2
    exit 2
    ;;
esac
