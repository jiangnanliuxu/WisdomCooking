#!/usr/bin/env bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${LOG_DIR:-$APP_HOME/logs}"

GATEWAY_PORT="${GATEWAY_PORT:-8080}"
MONITOR_PORT="${MONITOR_PORT:-9100}"
AUTH_PORT="${AUTH_PORT:-9200}"
SYSTEM_PORT="${SYSTEM_PORT:-9201}"
GEN_PORT="${GEN_PORT:-9202}"
JOB_PORT="${JOB_PORT:-9203}"
COOK_PORT="${COOK_PORT:-9210}"
FILE_PORT="${FILE_PORT:-9300}"

NACOS_DISCOVERY_IP="${NACOS_DISCOVERY_IP:-127.0.0.1}"
NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-public}"
NACOS_USERNAME="${NACOS_USERNAME:-371}"
NACOS_PASSWORD="${NACOS_PASSWORD:-change_me}"
SENTINEL_DASHBOARD="${SENTINEL_DASHBOARD:-127.0.0.1:8718}"
SENTINEL_NACOS_ADDR="${SENTINEL_NACOS_ADDR:-$NACOS_ADDR}"
SENTINEL_NACOS_NAMESPACE="${SENTINEL_NACOS_NAMESPACE:-$NACOS_NAMESPACE}"
REDIS_ADDR="${REDIS_ADDR:-127.0.0.1:6379}"
ELASTICSEARCH_URIS="${ELASTICSEARCH_URIS:-http://127.0.0.1:19200}"

WAIT_SECONDS="${WAIT_SECONDS:-90}"
WAIT_HEALTH="${WAIT_HEALTH:-1}"
LOG_TAIL_LINES="${LOG_TAIL_LINES:-200}"
export ELASTICSEARCH_URIS

ACTION="start"
LOG_SERVICE=""

SERVICES=(
  "ruoyi-system|ruoyi-system|$SYSTEM_PORT|ruoyi-modules-system.jar|http://127.0.0.1:$SYSTEM_PORT/actuator/health"
  "ruoyi-gen|ruoyi-gen|$GEN_PORT|ruoyi-modules-gen.jar|http://127.0.0.1:$GEN_PORT/actuator/health"
  "ruoyi-job|ruoyi-job|$JOB_PORT|ruoyi-modules-job.jar|http://127.0.0.1:$JOB_PORT/actuator/health"
  "ruoyi-file|ruoyi-file|$FILE_PORT|ruoyi-modules-file.jar|http://127.0.0.1:$FILE_PORT/actuator/health"
  "ruoyi-cook|ruoyi-cook|$COOK_PORT|ruoyi-modules-cook.jar|http://127.0.0.1:$COOK_PORT/actuator/health"
  "ruoyi-monitor|ruoyi-monitor|$MONITOR_PORT|ruoyi-visual-monitor.jar|http://127.0.0.1:$MONITOR_PORT/actuator/health"
  "ruoyi-auth|ruoyi-auth|$AUTH_PORT|ruoyi-auth.jar|http://127.0.0.1:$AUTH_PORT/actuator/health"
  "ruoyi-gateway|ruoyi-gateway|$GATEWAY_PORT|ruoyi-gateway.jar|http://127.0.0.1:$GATEWAY_PORT/actuator/health"
)

usage() {
  cat <<EOF
Usage:
  bash start-all.sh [start|restart|stop|status|logs] [--no-wait]
  bash start-all.sh logs <service>

Examples:
  bash start-all.sh start
  bash start-all.sh restart
  bash start-all.sh stop
  bash start-all.sh status
  bash start-all.sh logs ruoyi-cook

Services:
  ruoyi-system ruoyi-gen ruoyi-job ruoyi-file ruoyi-cook ruoyi-monitor ruoyi-auth ruoyi-gateway
EOF
}

log() {
  printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    start|restart|stop|status|logs)
      ACTION="$1"
      if [[ "$ACTION" == "logs" ]]; then
        shift || true
        LOG_SERVICE="${1:-}"
        [[ -n "$LOG_SERVICE" ]] || die "Usage: bash start-all.sh logs <service>"
      fi
      ;;
    --no-wait)
      WAIT_HEALTH=0
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage
      die "Unknown argument: $1"
      ;;
  esac
  shift || true
done

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

split_host() {
  printf '%s' "${1%:*}"
}

split_port() {
  printf '%s' "${1##*:}"
}

tcp_open() {
  local addr="$1"
  nc -z "$(split_host "$addr")" "$(split_port "$addr")" >/dev/null 2>&1
}

screen_exists() {
  local session="$1"
  { screen -ls 2>/dev/null || true; } | grep -Eq "[[:space:]][0-9]+\\.${session}[[:space:]]"
}

service_field() {
  local service_name="$1"
  local field="$2"
  local row
  for row in "${SERVICES[@]}"; do
    IFS='|' read -r name session port jar health <<<"$row"
    if [[ "$name" == "$service_name" ]]; then
      case "$field" in
        session) printf '%s\n' "$session" ;;
        port) printf '%s\n' "$port" ;;
        jar) printf '%s\n' "$jar" ;;
        health) printf '%s\n' "$health" ;;
        *) return 1 ;;
      esac
      return 0
    fi
  done
  return 1
}

service_log_path() {
  printf '%s/%s.log\n' "$LOG_DIR" "$1"
}

stop_session() {
  local session="$1"
  if screen_exists "$session"; then
    log "Stopping screen session: $session"
    screen -S "$session" -X quit || true
    local tries=0
    while screen_exists "$session" && [[ $tries -lt 20 ]]; do
      sleep 0.5
      tries=$((tries + 1))
    done
  fi
}

start_screen() {
  local session="$1"
  local workdir="$2"
  local logfile="$3"
  shift 3

  if screen_exists "$session"; then
    log "$session is already running; skip."
    return 0
  fi

  mkdir -p "$LOG_DIR"
  : > "$logfile"

  log "Starting $session; log: $logfile"
  screen -dmS "$session" bash -lc '
    cd "$1" || exit 1
    logfile="$2"
    shift 2
    unset http_proxy https_proxy all_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY
    unset JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS _JAVA_OPTIONS
    export NO_PROXY="${NO_PROXY:-127.0.0.1,localhost,::1}"
    export no_proxy="${no_proxy:-127.0.0.1,localhost,::1}"
    exec "$@" > "$logfile" 2>&1
  ' _ "$workdir" "$logfile" "$@"
}

wait_url() {
  local name="$1"
  local url="$2"
  local logfile="${3:-}"

  [[ "$WAIT_HEALTH" == "1" ]] || return 0

  local deadline=$((SECONDS + WAIT_SECONDS))
  while [[ $SECONDS -lt $deadline ]]; do
    if curl -fsS --max-time 2 "$url" >/dev/null 2>&1; then
      log "$name is ready: $url"
      return 0
    fi
    sleep 2
  done

  log "$name did not become ready in ${WAIT_SECONDS}s: $url"
  if [[ -n "$logfile" && -f "$logfile" ]]; then
    log "Last log lines from $logfile:"
    tail -n 40 "$logfile" || true
  fi
  return 1
}

print_port_owner() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null || true
}

is_project_process() {
  local cmd="$1"
  case "$cmd" in
    *"ruoyi-gateway.jar"*|\
    *"ruoyi-auth.jar"*|\
    *"ruoyi-visual-monitor.jar"*|\
    *"ruoyi-modules-system.jar"*|\
    *"ruoyi-modules-gen.jar"*|\
    *"ruoyi-modules-job.jar"*|\
    *"ruoyi-modules-file.jar"*|\
    *"ruoyi-modules-cook.jar"*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

stop_project_listeners() {
  local row
  local pid
  local pids
  local cmd
  local killed=()

  for row in "${SERVICES[@]}"; do
    IFS='|' read -r name session port jar health <<<"$row"
    pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"
    for pid in $pids; do
      cmd="$(ps -p "$pid" -o command= 2>/dev/null || true)"
      if is_project_process "$cmd"; then
        log "Stopping project process on port $port: pid=$pid"
        kill "$pid" 2>/dev/null || true
        killed+=("$pid")
      else
        log "Leaving non-project process on port $port: pid=$pid"
      fi
    done
  done

  if [[ ${#killed[@]} -eq 0 ]]; then
    return 0
  fi

  local tries=0
  local still_running
  while [[ $tries -lt 20 ]]; do
    still_running=0
    for pid in "${killed[@]}"; do
      if kill -0 "$pid" 2>/dev/null; then
        still_running=1
      fi
    done
    [[ "$still_running" == "0" ]] && return 0
    sleep 0.5
    tries=$((tries + 1))
  done

  for pid in "${killed[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      log "Force stopping project process: pid=$pid"
      kill -9 "$pid" 2>/dev/null || true
    fi
  done
}

stop_project_processes() {
  local pid
  local cmd
  local killed=()

  while read -r pid cmd; do
    [[ -n "${pid:-}" && -n "${cmd:-}" ]] || continue
    [[ "$pid" == "$$" ]] && continue

    if is_project_process "$cmd"; then
      log "Stopping leftover project process: pid=$pid"
      kill "$pid" 2>/dev/null || true
      killed+=("$pid")
    fi
  done < <(ps -axo pid=,command=)

  if [[ ${#killed[@]} -eq 0 ]]; then
    return 0
  fi

  local tries=0
  local still_running
  while [[ $tries -lt 20 ]]; do
    still_running=0
    for pid in "${killed[@]}"; do
      if kill -0 "$pid" 2>/dev/null; then
        still_running=1
      fi
    done
    [[ "$still_running" == "0" ]] && return 0
    sleep 0.5
    tries=$((tries + 1))
  done

  for pid in "${killed[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      log "Force stopping leftover project process: pid=$pid"
      kill -9 "$pid" 2>/dev/null || true
    fi
  done
}

check_port_conflicts() {
  local conflict=0
  local row
  local port
  local session

  for row in "${SERVICES[@]}"; do
    IFS='|' read -r name session port jar health <<<"$row"
    if print_port_owner "$port" | grep -q "LISTEN" && ! screen_exists "$session"; then
      log "Port $port is currently listening:"
      print_port_owner "$port"
      conflict=1
    fi
  done
  return "$conflict"
}

preflight_start() {
  require_cmd screen
  require_cmd java
  require_cmd curl
  require_cmd nc
  require_cmd lsof

  tcp_open "$NACOS_ADDR" || die "Nacos is not listening on $NACOS_ADDR. Start it first."
  tcp_open "$SENTINEL_DASHBOARD" || die "Sentinel is not listening on $SENTINEL_DASHBOARD. Start it first."
  tcp_open "$REDIS_ADDR" || die "Redis is not listening on $REDIS_ADDR. Start it first."
}

require_jar() {
  local jar="$1"
  [[ -f "$jar" ]] || die "Missing jar: $jar"
}

build_nacos_args() {
  local args=(
    "--spring.cloud.nacos.discovery.ip=$NACOS_DISCOVERY_IP"
    "--spring.cloud.nacos.discovery.server-addr=$NACOS_ADDR"
    "--spring.cloud.nacos.config.server-addr=$NACOS_ADDR"
    "--spring.cloud.sentinel.transport.dashboard=$SENTINEL_DASHBOARD"
    "--spring.cloud.sentinel.datasource.ds1.nacos.server-addr=$SENTINEL_NACOS_ADDR"
  )

  if [[ -n "$NACOS_NAMESPACE" ]]; then
    args+=(
      "--spring.cloud.nacos.discovery.namespace=$NACOS_NAMESPACE"
      "--spring.cloud.nacos.config.namespace=$NACOS_NAMESPACE"
    )
  fi
  if [[ -n "$NACOS_USERNAME" ]]; then
    args+=(
      "--spring.cloud.nacos.username=$NACOS_USERNAME"
      "--spring.cloud.nacos.discovery.username=$NACOS_USERNAME"
      "--spring.cloud.nacos.config.username=$NACOS_USERNAME"
      "--spring.cloud.sentinel.datasource.ds1.nacos.username=$NACOS_USERNAME"
    )
  fi
  if [[ -n "$NACOS_PASSWORD" ]]; then
    args+=(
      "--spring.cloud.nacos.password=$NACOS_PASSWORD"
      "--spring.cloud.nacos.discovery.password=$NACOS_PASSWORD"
      "--spring.cloud.nacos.config.password=$NACOS_PASSWORD"
      "--spring.cloud.sentinel.datasource.ds1.nacos.password=$NACOS_PASSWORD"
    )
  fi
  if [[ -n "$SENTINEL_NACOS_NAMESPACE" ]]; then
    args+=("--spring.cloud.sentinel.datasource.ds1.nacos.namespace=$SENTINEL_NACOS_NAMESPACE")
  fi

  printf '%s\n' "${args[@]}"
}

start_backend() {
  local java_opts=(
    -Djava.net.useSystemProxies=false
    -Dhttp.proxyHost=
    -Dhttps.proxyHost=
    -DsocksProxyHost=
    '-Dhttp.nonProxyHosts=localhost|127.*|[::1]'
  )
  if [[ -n "$NACOS_USERNAME" ]]; then
    java_opts+=("-Dnacos.username=$NACOS_USERNAME")
  fi
  if [[ -n "$NACOS_PASSWORD" ]]; then
    java_opts+=("-Dnacos.password=$NACOS_PASSWORD")
  fi

  local nacos_args=()
  while IFS= read -r arg; do
    [[ -n "$arg" ]] && nacos_args+=("$arg")
  done < <(build_nacos_args)

  local row
  local name
  local session
  local port
  local jar_name
  local jar
  local health
  for row in "${SERVICES[@]}"; do
    IFS='|' read -r name session port jar_name health <<<"$row"
    jar="$APP_HOME/$jar_name"
    require_jar "$jar"
    start_screen "$session" "$APP_HOME" "$(service_log_path "$name")" \
      java "${java_opts[@]}" -jar "$jar" "${nacos_args[@]}" "--server.port=$port"
    wait_url "$name" "$health" "$(service_log_path "$name")"
  done
}

stop_all() {
  local row
  local session
  for row in "${SERVICES[@]}"; do
    IFS='|' read -r name session port jar health <<<"$row"
    stop_session "$session"
  done
  stop_project_listeners
  stop_project_processes
}

status_one() {
  local name="$1"
  local session="$2"
  local port="$3"
  local url="$4"
  local screen_state="stopped"
  local port_state="closed"
  local http_state="fail"

  screen_exists "$session" && screen_state="running"
  if print_port_owner "$port" | grep -q "LISTEN"; then
    port_state="listening"
  fi
  if curl -fsS --max-time 2 "$url" >/dev/null 2>&1; then
    http_state="ok"
  fi

  printf '%-14s screen=%-8s port=%-10s http=%s\n' "$name" "$screen_state" "$port_state" "$http_state"
}

show_status() {
  local row
  local name
  local session
  local port
  local health

  printf 'infra          nacos=%-10s sentinel=%-10s redis=%s\n' \
    "$(tcp_open "$NACOS_ADDR" && echo listening || echo closed)" \
    "$(tcp_open "$SENTINEL_DASHBOARD" && echo listening || echo closed)" \
    "$(tcp_open "$REDIS_ADDR" && echo listening || echo closed)"

  for row in "${SERVICES[@]}"; do
    IFS='|' read -r name session port jar health <<<"$row"
    status_one "$name" "$session" "$port" "$health"
  done
}

show_logs() {
  local service="$1"
  local logfile
  service_field "$service" session >/dev/null || die "Unknown service: $service"
  logfile="$(service_log_path "$service")"
  [[ -f "$logfile" ]] || die "Log file not found: $logfile"
  tail -n "$LOG_TAIL_LINES" "$logfile"
}

start_or_restart() {
  preflight_start
  if [[ "$ACTION" == "restart" ]]; then
    stop_all
    sleep 1
  fi
  check_port_conflicts || die "One or more required ports are occupied."
  start_backend
  show_status
}

case "$ACTION" in
  stop)
    stop_all
    show_status
    ;;
  status)
    show_status
    ;;
  logs)
    show_logs "$LOG_SERVICE"
    ;;
  restart|start)
    start_or_restart
    ;;
esac
