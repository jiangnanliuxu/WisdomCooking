#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/projetc/RuoYi-Cloud"
LOG_DIR="${LOG_DIR:-$ROOT_DIR/logs/startup}"
BUNDLE_DIR="${BUNDLE_DIR:-$ROOT_DIR/release/ruoyi-cloud-backend-package}"
BUNDLE_LAUNCHER_SOURCE="$ROOT_DIR/scripts/start-all-bundle.sh"
BUNDLE_DOC_SOURCE="$ROOT_DIR/docs/RuoYi-Cloud-后端微服务一键打包与启动说明.md"

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
SKIP_BUILD="${SKIP_BUILD:-0}"
WAIT_HEALTH="${WAIT_HEALTH:-1}"
LOG_TAIL_LINES="${LOG_TAIL_LINES:-200}"
export ELASTICSEARCH_URIS

ACTION="start"
LOG_SERVICE=""

SERVICES=(
  "ruoyi-system|ruoyi-system|$SYSTEM_PORT|ruoyi-modules/ruoyi-system/target/ruoyi-modules-system.jar|http://127.0.0.1:$SYSTEM_PORT/actuator/health"
  "ruoyi-gen|ruoyi-gen|$GEN_PORT|ruoyi-modules/ruoyi-gen/target/ruoyi-modules-gen.jar|http://127.0.0.1:$GEN_PORT/actuator/health"
  "ruoyi-job|ruoyi-job|$JOB_PORT|ruoyi-modules/ruoyi-job/target/ruoyi-modules-job.jar|http://127.0.0.1:$JOB_PORT/actuator/health"
  "ruoyi-file|ruoyi-file|$FILE_PORT|ruoyi-modules/ruoyi-file/target/ruoyi-modules-file.jar|http://127.0.0.1:$FILE_PORT/actuator/health"
  "ruoyi-cook|ruoyi-cook|$COOK_PORT|ruoyi-modules/ruoyi-cook/target/ruoyi-modules-cook.jar|http://127.0.0.1:$COOK_PORT/actuator/health"
  "ruoyi-monitor|ruoyi-monitor|$MONITOR_PORT|ruoyi-visual/ruoyi-monitor/target/ruoyi-visual-monitor.jar|http://127.0.0.1:$MONITOR_PORT/actuator/health"
  "ruoyi-auth|ruoyi-auth|$AUTH_PORT|ruoyi-auth/target/ruoyi-auth.jar|http://127.0.0.1:$AUTH_PORT/actuator/health"
  "ruoyi-gateway|ruoyi-gateway|$GATEWAY_PORT|ruoyi-gateway/target/ruoyi-gateway.jar|http://127.0.0.1:$GATEWAY_PORT/actuator/health"
)

usage() {
  cat <<EOF
Usage:
  bash scripts/start-all.sh [build|start|restart|stop|status|logs] [--skip-build] [--no-wait]
  bash scripts/start-all.sh logs <service>

Examples:
  bash scripts/start-all.sh build
  bash scripts/start-all.sh start
  bash scripts/start-all.sh restart --skip-build
  bash scripts/start-all.sh status
  bash scripts/start-all.sh logs ruoyi-cook

Services:
  ruoyi-system ruoyi-gen ruoyi-job ruoyi-file ruoyi-cook ruoyi-monitor ruoyi-auth ruoyi-gateway

Environment overrides:
  GATEWAY_PORT=$GATEWAY_PORT
  MONITOR_PORT=$MONITOR_PORT
  AUTH_PORT=$AUTH_PORT
  SYSTEM_PORT=$SYSTEM_PORT
  GEN_PORT=$GEN_PORT
  JOB_PORT=$JOB_PORT
  COOK_PORT=$COOK_PORT
  FILE_PORT=$FILE_PORT
  NACOS_DISCOVERY_IP=$NACOS_DISCOVERY_IP
  NACOS_ADDR=$NACOS_ADDR
  NACOS_NAMESPACE=$NACOS_NAMESPACE
  NACOS_USERNAME=$NACOS_USERNAME
  NACOS_PASSWORD=$NACOS_PASSWORD
  SENTINEL_DASHBOARD=$SENTINEL_DASHBOARD
  REDIS_ADDR=$REDIS_ADDR
  ELASTICSEARCH_URIS=$ELASTICSEARCH_URIS
  WAIT_SECONDS=$WAIT_SECONDS
  LOG_TAIL_LINES=$LOG_TAIL_LINES
  BUNDLE_DIR=$BUNDLE_DIR
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
    build|start|restart|stop|status|logs)
      ACTION="$1"
      if [[ "$ACTION" == "logs" ]]; then
        shift || true
        LOG_SERVICE="${1:-}"
        [[ -n "$LOG_SERVICE" ]] || die "Usage: bash scripts/start-all.sh logs <service>"
      fi
      ;;
    --skip-build)
      SKIP_BUILD=1
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
    *"ruoyi-gateway/target/ruoyi-gateway.jar"*|\
    *"ruoyi-auth/target/ruoyi-auth.jar"*|\
    *"ruoyi-visual/ruoyi-monitor/target/ruoyi-visual-monitor.jar"*|\
    *"ruoyi-modules/ruoyi-system/target/ruoyi-modules-system.jar"*|\
    *"ruoyi-modules/ruoyi-gen/target/ruoyi-modules-gen.jar"*|\
    *"ruoyi-modules/ruoyi-job/target/ruoyi-modules-job.jar"*|\
    *"ruoyi-modules/ruoyi-file/target/ruoyi-modules-file.jar"*|\
    *"ruoyi-modules/ruoyi-cook/target/ruoyi-modules-cook.jar"*)
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

preflight_common() {
  require_cmd java
  require_cmd mvn
  require_cmd curl
  require_cmd nc
  require_cmd lsof
  require_cmd cp
  require_cmd chmod
  [[ -d "$BACKEND_DIR" ]] || die "Backend directory missing: $BACKEND_DIR"
}

preflight_start() {
  preflight_common
  require_cmd screen
  tcp_open "$NACOS_ADDR" || die "Nacos is not listening on $NACOS_ADDR. Start it first."
  tcp_open "$SENTINEL_DASHBOARD" || die "Sentinel is not listening on $SENTINEL_DASHBOARD. Start it first."
  tcp_open "$REDIS_ADDR" || die "Redis is not listening on $REDIS_ADDR. Start it first."
}

build_backend() {
  if [[ "$SKIP_BUILD" == "1" ]]; then
    log "Skipping Maven package because SKIP_BUILD=1."
    return 0
  fi

  log "Packaging backend jars..."
  (
    cd "$BACKEND_DIR"
    mvn -pl ruoyi-gateway,ruoyi-auth,ruoyi-visual/ruoyi-monitor,ruoyi-modules/ruoyi-system,ruoyi-modules/ruoyi-gen,ruoyi-modules/ruoyi-job,ruoyi-modules/ruoyi-file,ruoyi-modules/ruoyi-cook -am -DskipTests package
  )
}

require_jar() {
  local jar="$1"
  [[ -f "$jar" ]] || die "Missing jar: $jar. Run without --skip-build first."
}

stage_bundle() {
  local row
  local name
  local jar_rel
  local jar_src

  [[ -f "$BUNDLE_LAUNCHER_SOURCE" ]] || die "Missing bundle launcher template: $BUNDLE_LAUNCHER_SOURCE"
  [[ -f "$BUNDLE_DOC_SOURCE" ]] || die "Missing bundle document source: $BUNDLE_DOC_SOURCE"

  mkdir -p "$BUNDLE_DIR"
  rm -f "$BUNDLE_DIR"/*.jar
  mkdir -p "$BUNDLE_DIR/logs"

  for row in "${SERVICES[@]}"; do
    IFS='|' read -r name session port jar_rel health <<<"$row"
    jar_src="$BACKEND_DIR/$jar_rel"
    require_jar "$jar_src"
    cp "$jar_src" "$BUNDLE_DIR/"
  done

  cp "$BUNDLE_LAUNCHER_SOURCE" "$BUNDLE_DIR/start-all.sh"
  chmod +x "$BUNDLE_DIR/start-all.sh"
  cp "$BUNDLE_DOC_SOURCE" "$BUNDLE_DIR/"

  log "Bundle directory is ready: $BUNDLE_DIR"
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
  local jar_rel
  local jar
  local health
  for row in "${SERVICES[@]}"; do
    IFS='|' read -r name session port jar_rel health <<<"$row"
    jar="$BACKEND_DIR/$jar_rel"
    require_jar "$jar"
    start_screen "$session" "$BACKEND_DIR" "$(service_log_path "$name")" \
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
  build_backend
  start_backend
  show_status
  log "Gateway: http://127.0.0.1:$GATEWAY_PORT/"
  log "Cook:    http://127.0.0.1:$COOK_PORT/"
}

case "$ACTION" in
  build)
    preflight_common
    build_backend
    stage_bundle
    ;;
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
