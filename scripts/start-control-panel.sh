#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORT="${CONTROL_PANEL_PORT:-8787}"
HOST="${CONTROL_PANEL_HOST:-127.0.0.1}"
URL="http://${HOST}:${PORT}/"
SESSION="start-control-panel"
LOG_DIR="${LOG_DIR:-$ROOT_DIR/logs/startup}"
LOG_FILE="$LOG_DIR/control-panel.log"
OPEN_BROWSER=1
BACKGROUND=0
ACTION="start"

usage() {
  cat <<EOF
Usage:
  bash scripts/start-control-panel.sh [start|stop|status] [--background] [--no-open]

Examples:
  bash scripts/start-control-panel.sh
  bash scripts/start-control-panel.sh --background
  bash scripts/start-control-panel.sh stop

Environment overrides:
  CONTROL_PANEL_PORT=8787
  CONTROL_PANEL_HOST=127.0.0.1
EOF
}

log() {
  printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

screen_exists() {
  { screen -ls 2>/dev/null || true; } | grep -Eq "[[:space:]][0-9]+\\.${SESSION}[[:space:]]"
}

port_has_panel() {
  curl -fsS --max-time 2 "${URL}api/status" >/dev/null 2>&1
}

port_is_busy() {
  lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1
}

open_browser() {
  if [[ "$OPEN_BROWSER" == "1" ]] && command -v open >/dev/null 2>&1; then
    open "$URL" >/dev/null 2>&1 || true
  fi
}

wait_for_panel() {
  local tries=0
  while [[ $tries -lt 20 ]]; do
    if port_has_panel; then
      return 0
    fi
    sleep 0.5
    tries=$((tries + 1))
  done
  return 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    start|stop|status)
      ACTION="$1"
      ;;
    --background)
      BACKGROUND=1
      ;;
    --no-open)
      OPEN_BROWSER=0
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage
      printf 'ERROR: Unknown argument: %s\n' "$1" >&2
      exit 1
      ;;
  esac
  shift
done

case "$ACTION" in
  stop)
    if screen_exists; then
      log "Stopping screen session: $SESSION"
      screen -S "$SESSION" -X quit || true
    fi
    if port_has_panel; then
      pid="$(lsof -tiTCP:"$PORT" -sTCP:LISTEN 2>/dev/null || true)"
      if [[ -n "${pid:-}" ]]; then
        log "Stopping control panel process: pid=$pid"
        kill $pid 2>/dev/null || true
      fi
    fi
    ;;
  status)
    if port_has_panel; then
      log "Control panel is running: $URL"
    else
      log "Control panel is not running on $URL"
    fi
    ;;
  start)
    command -v node >/dev/null 2>&1 || {
      printf 'ERROR: node is required.\n' >&2
      exit 1
    }
    command -v screen >/dev/null 2>&1 || {
      printf 'ERROR: screen is required.\n' >&2
      exit 1
    }

    if port_has_panel; then
      log "Control panel is already running: $URL"
      open_browser
      exit 0
    fi

    if port_is_busy; then
      lsof -nP -iTCP:"$PORT" -sTCP:LISTEN
      printf 'ERROR: Port %s is occupied by another process.\n' "$PORT" >&2
      exit 1
    fi

    mkdir -p "$LOG_DIR"

    if [[ "$BACKGROUND" == "1" ]]; then
      log "Starting control panel in screen: $SESSION"
      screen -dmS "$SESSION" bash -lc '
        cd "$1" || exit 1
        exec node scripts/control-panel/server.js >> "$2" 2>&1
      ' _ "$ROOT_DIR" "$LOG_FILE"
      if wait_for_panel; then
        log "Control panel is ready: $URL"
        open_browser
      else
        log "Control panel did not respond yet. Log: $LOG_FILE"
      fi
    else
      log "Starting control panel: $URL"
      (sleep 1 && open_browser) &
      cd "$ROOT_DIR"
      exec node scripts/control-panel/server.js
    fi
    ;;
esac
