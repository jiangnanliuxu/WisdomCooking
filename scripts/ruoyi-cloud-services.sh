#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${BACKEND_DIR:-$ROOT_DIR/projetc/RuoYi-Cloud}"
LOG_DIR="${LOG_DIR:-$ROOT_DIR/logs/jar-services}"
NACOS_HOME="${NACOS_HOME:-$ROOT_DIR/nacos-server-2.2.3}"
SENTINEL_HOME="${SENTINEL_HOME:-$ROOT_DIR/sentinel-1.8.9}"

ACTION="${1:-start}"
if [[ $# -gt 0 ]]; then
  shift
fi

SKIP_BUILD=0
START_INFRA=1
WAIT_TIMEOUT="${WAIT_TIMEOUT:-180}"

NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-public}"
NACOS_USERNAME="${NACOS_USERNAME:-371}"
NACOS_PASSWORD="${NACOS_PASSWORD:-change_me}"
NACOS_DISCOVERY_IP="${NACOS_DISCOVERY_IP:-127.0.0.1}"
REDIS_ADDR="${REDIS_ADDR:-127.0.0.1:6379}"
SENTINEL_DASHBOARD="${SENTINEL_DASHBOARD:-127.0.0.1:8718}"
SENTINEL_NACOS_ADDR="${SENTINEL_NACOS_ADDR:-$NACOS_ADDR}"
SENTINEL_NACOS_NAMESPACE="${SENTINEL_NACOS_NAMESPACE:-$NACOS_NAMESPACE}"

declare -a SERVICES=(
  "ruoyi-system|9201|$BACKEND_DIR/ruoyi-modules/ruoyi-system/target/ruoyi-modules-system.jar"
  "ruoyi-gen|9202|$BACKEND_DIR/ruoyi-modules/ruoyi-gen/target/ruoyi-modules-gen.jar"
  "ruoyi-job|9203|$BACKEND_DIR/ruoyi-modules/ruoyi-job/target/ruoyi-modules-job.jar"
  "ruoyi-auth|9200|$BACKEND_DIR/ruoyi-auth/target/ruoyi-auth.jar"
  "ruoyi-file|9300|$BACKEND_DIR/ruoyi-modules/ruoyi-file/target/ruoyi-modules-file.jar"
  "ruoyi-cook|9210|$BACKEND_DIR/ruoyi-modules/ruoyi-cook/target/ruoyi-modules-cook.jar"
  "ruoyi-monitor|9100|$BACKEND_DIR/ruoyi-visual/ruoyi-monitor/target/ruoyi-visual-monitor.jar"
  "ruoyi-gateway|8080|$BACKEND_DIR/ruoyi-gateway/target/ruoyi-gateway.jar"
)

usage() {
  cat <<EOF
Usage:
  bash scripts/ruoyi-cloud-services.sh [package|start|restart|stop|status] [--skip-build] [--skip-infra]

Examples:
  bash scripts/ruoyi-cloud-services.sh package
  bash scripts/ruoyi-cloud-services.sh start
  bash scripts/ruoyi-cloud-services.sh restart
  bash scripts/ruoyi-cloud-services.sh stop
  bash scripts/ruoyi-cloud-services.sh status

Environment overrides:
  NACOS_ADDR=$NACOS_ADDR
  NACOS_NAMESPACE=$NACOS_NAMESPACE
  NACOS_USERNAME=$NACOS_USERNAME
  NACOS_PASSWORD=$NACOS_PASSWORD
  NACOS_DISCOVERY_IP=$NACOS_DISCOVERY_IP
  REDIS_ADDR=$REDIS_ADDR
  SENTINEL_DASHBOARD=$SENTINEL_DASHBOARD
  WAIT_TIMEOUT=$WAIT_TIMEOUT
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
    --skip-build)
      SKIP_BUILD=1
      ;;
    --skip-infra)
      START_INFRA=0
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
  shift
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
  local host="$1"
  local port="$2"
  nc -z "$host" "$port" >/dev/null 2>&1
}

screen_exists() {
  local session="$1"
  { screen -ls 2>/dev/null || true; } | grep -Eq "[[:space:]][0-9]+\\.${session}[[:space:]]"
}

service_port() {
  local name="$1"
  local item
  for item in "${SERVICES[@]}"; do
    IFS='|' read -r service_name service_port_value _ <<<"$item"
    if [[ "$service_name" == "$name" ]]; then
      printf '%s' "$service_port_value"
      return 0
    fi
  done
  return 1
}

service_jar() {
  local name="$1"
  local item
  for item in "${SERVICES[@]}"; do
    IFS='|' read -r service_name _ service_jar_value <<<"$item"
    if [[ "$service_name" == "$name" ]]; then
      printf '%s' "$service_jar_value"
      return 0
    fi
  done
  return 1
}

start_screen() {
  local session="$1"
  local workdir="$2"
  local logfile="$3"
  shift 3

  mkdir -p "$LOG_DIR"
  : > "$logfile"

  if screen_exists "$session"; then
    log "$session screen already exists; recreating it."
    screen -S "$session" -X quit || true
    sleep 1
  fi

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

stop_screen() {
  local session="$1"
  if screen_exists "$session"; then
    screen -S "$session" -X quit || true
  fi
}

is_our_process() {
  local cmd="$1"
  case "$cmd" in
    *"$ROOT_DIR"*|\
    *"/target/ruoyi-auth.jar"*|\
    *"/target/ruoyi-gateway.jar"*|\
    *"/target/ruoyi-modules-system.jar"*|\
    *"/target/ruoyi-modules-gen.jar"*|\
    *"/target/ruoyi-modules-job.jar"*|\
    *"/target/ruoyi-modules-file.jar"*|\
    *"/target/ruoyi-modules-cook.jar"*|\
    *"/target/ruoyi-visual-monitor.jar"*|\
    *"sentinel-dashboard-1.8.9.jar"*|\
    *"nacos-server.jar"*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

kill_listeners() {
  local item
  local port
  local pids
  local pid
  local cmd
  local killed=()

  for item in "${SERVICES[@]}"; do
    IFS='|' read -r _ port _ <<<"$item"
    pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"
    for pid in $pids; do
      cmd="$(ps -p "$pid" -o command= 2>/dev/null || true)"
      if is_our_process "$cmd"; then
        kill "$pid" 2>/dev/null || true
        killed+=("$pid")
      fi
    done
  done

  for port in 8848 9848 8718 6379; do
    pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"
    for pid in $pids; do
      cmd="$(ps -p "$pid" -o command= 2>/dev/null || true)"
      if is_our_process "$cmd"; then
        kill "$pid" 2>/dev/null || true
        killed+=("$pid")
      fi
    done
  done

  if [[ ${#killed[@]} -eq 0 ]]; then
    return 0
  fi

  sleep 2
  for pid in "${killed[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      kill -9 "$pid" 2>/dev/null || true
    fi
  done
}

wait_for_port() {
  local name="$1"
  local host="$2"
  local port="$3"
  local logfile="$4"
  local deadline=$((SECONDS + WAIT_TIMEOUT))

  while [[ $SECONDS -lt $deadline ]]; do
    if tcp_open "$host" "$port"; then
      log "$name is listening on $host:$port"
      return 0
    fi
    sleep 2
  done

  if [[ -f "$logfile" ]]; then
    tail -n 80 "$logfile" || true
  fi
  die "$name did not listen on $host:$port within ${WAIT_TIMEOUT}s"
}

start_redis() {
  if tcp_open 127.0.0.1 "$(split_port "$REDIS_ADDR")"; then
    log "Redis already listening on $REDIS_ADDR"
    return 0
  fi

  require_cmd redis-server
  start_screen smartmeal-redis "$ROOT_DIR" "$LOG_DIR/redis.log" \
    redis-server --bind 127.0.0.1 --port "$(split_port "$REDIS_ADDR")" --save "" --appendonly no
  wait_for_port "Redis" 127.0.0.1 "$(split_port "$REDIS_ADDR")" "$LOG_DIR/redis.log"
}

start_nacos() {
  if tcp_open 127.0.0.1 "$(split_port "$NACOS_ADDR")"; then
    log "Nacos already listening on $NACOS_ADDR"
    return 0
  fi

  [[ -d "$NACOS_HOME" ]] || die "Nacos directory missing: $NACOS_HOME"
  [[ -f "$NACOS_HOME/target/nacos-server.jar" ]] || die "Nacos jar missing: $NACOS_HOME/target/nacos-server.jar"

  start_screen smartmeal-nacos "$NACOS_HOME" "$NACOS_HOME/logs/start.out" bash -lc '
    base_dir="$PWD"
    java_bin="${NACOS_JAVA:-}"
    if [[ -z "$java_bin" && -x /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java ]]; then
      java_bin=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java
    fi
    if [[ -z "$java_bin" ]]; then
      java_bin="$(command -v java)"
    fi
    java_opt="-Xms512m -Xmx512m -Xmn256m"
    java_opt="$java_opt -Dnacos.standalone=true -Dnacos.member.list="
    java_opt="$java_opt -Xlog:gc*:file=$base_dir/logs/nacos_gc.log:time,tags:filecount=10,filesize=100m"
    java_opt="$java_opt -Dloader.path=$base_dir/plugins,$base_dir/plugins/health,$base_dir/plugins/cmdb,$base_dir/plugins/selector"
    java_opt="$java_opt -Dnacos.home=$base_dir -jar $base_dir/target/nacos-server.jar"
    java_opt="$java_opt --spring.config.additional-location=file:$base_dir/conf/"
    java_opt="$java_opt --logging.config=$base_dir/conf/nacos-logback.xml"
    java_opt="$java_opt --server.max-http-header-size=524288"
    exec "$java_bin" $java_opt
  '
  wait_for_port "Nacos" 127.0.0.1 "$(split_port "$NACOS_ADDR")" "$NACOS_HOME/logs/start.out"
  wait_for_port "Nacos gRPC" 127.0.0.1 9848 "$NACOS_HOME/logs/start.out"
}

start_sentinel() {
  local jar="$SENTINEL_HOME/sentinel-dashboard-1.8.9.jar"
  local port
  port="$(split_port "$SENTINEL_DASHBOARD")"

  if tcp_open 127.0.0.1 "$port"; then
    log "Sentinel already listening on $SENTINEL_DASHBOARD"
    return 0
  fi

  [[ -f "$jar" ]] || die "Sentinel jar missing: $jar"
  start_screen sentinel-dashboard "$SENTINEL_HOME" "$LOG_DIR/sentinel-dashboard.log" \
    java -Dserver.port="$port" \
      -Dcsp.sentinel.dashboard.server=127.0.0.1:"$port" \
      -Dproject.name=sentinel-dashboard \
      -Dcsp.sentinel.api.port=8719 \
      -jar "$jar"
  wait_for_port "Sentinel" 127.0.0.1 "$port" "$LOG_DIR/sentinel-dashboard.log"
}

start_infra() {
  [[ "$START_INFRA" == "1" ]] || return 0
  start_redis
  start_nacos
  start_sentinel
}

package_jars() {
  [[ -d "$BACKEND_DIR" ]] || die "Backend directory missing: $BACKEND_DIR"
  log "Packaging all RuoYi Cloud microservice jars..."
  (
    cd "$BACKEND_DIR"
    mvn -pl ruoyi-auth,ruoyi-gateway,ruoyi-visual/ruoyi-monitor,ruoyi-modules/ruoyi-system,ruoyi-modules/ruoyi-gen,ruoyi-modules/ruoyi-job,ruoyi-modules/ruoyi-file,ruoyi-modules/ruoyi-cook -am -DskipTests clean package
  )
}

ensure_jars() {
  local item
  local service_name
  local jar
  for item in "${SERVICES[@]}"; do
    IFS='|' read -r service_name _ jar <<<"$item"
    [[ -f "$jar" ]] || die "Missing jar for $service_name: $jar"
  done
}

start_one_service() {
  local name="$1"
  local port="$2"
  local jar="$3"
  local logfile="$LOG_DIR/${name}.log"
  local java_opts=(
    -Djava.net.useSystemProxies=false
    -Dhttp.proxyHost=
    -Dhttps.proxyHost=
    -DsocksProxyHost=
    '-Dhttp.nonProxyHosts=localhost|127.*|[::1]'
  )
  local app_args=(
    "--server.port=$port"
    "--spring.cloud.nacos.discovery.ip=$NACOS_DISCOVERY_IP"
    "--spring.cloud.nacos.discovery.server-addr=$NACOS_ADDR"
    "--spring.cloud.nacos.config.server-addr=$NACOS_ADDR"
    "--spring.cloud.nacos.discovery.namespace=$NACOS_NAMESPACE"
    "--spring.cloud.nacos.config.namespace=$NACOS_NAMESPACE"
    "--spring.cloud.nacos.username=$NACOS_USERNAME"
    "--spring.cloud.nacos.discovery.username=$NACOS_USERNAME"
    "--spring.cloud.nacos.config.username=$NACOS_USERNAME"
    "--spring.cloud.nacos.password=$NACOS_PASSWORD"
    "--spring.cloud.nacos.discovery.password=$NACOS_PASSWORD"
    "--spring.cloud.nacos.config.password=$NACOS_PASSWORD"
    "--spring.cloud.sentinel.transport.dashboard=$SENTINEL_DASHBOARD"
    "--spring.cloud.sentinel.datasource.ds1.nacos.server-addr=$SENTINEL_NACOS_ADDR"
    "--spring.cloud.sentinel.datasource.ds1.nacos.namespace=$SENTINEL_NACOS_NAMESPACE"
    "--spring.cloud.sentinel.datasource.ds1.nacos.username=$NACOS_USERNAME"
    "--spring.cloud.sentinel.datasource.ds1.nacos.password=$NACOS_PASSWORD"
  )

  start_screen "$name" "$BACKEND_DIR" "$logfile" java "${java_opts[@]}" -jar "$jar" "${app_args[@]}"
  wait_for_port "$name" 127.0.0.1 "$port" "$logfile"
}

start_services() {
  local item
  for item in \
    "ruoyi-system" \
    "ruoyi-gen" \
    "ruoyi-job" \
    "ruoyi-file" \
    "ruoyi-cook" \
    "ruoyi-auth" \
    "ruoyi-monitor" \
    "ruoyi-gateway"; do
    start_one_service "$item" "$(service_port "$item")" "$(service_jar "$item")"
  done
}

stop_services() {
  local item
  for item in ruoyi-gateway ruoyi-monitor ruoyi-auth ruoyi-cook ruoyi-file ruoyi-job ruoyi-gen ruoyi-system; do
    stop_screen "$item"
  done
  stop_screen sentinel-dashboard
  stop_screen smartmeal-nacos
  stop_screen smartmeal-redis

  if [[ -x "$NACOS_HOME/bin/shutdown.sh" ]]; then
    (cd "$NACOS_HOME/bin" && bash shutdown.sh) >/dev/null 2>&1 || true
  fi
  kill_listeners
}

status_one() {
  local name="$1"
  local port="$2"
  local screen_state="stopped"
  local port_state="closed"

  screen_exists "$name" && screen_state="running"
  if tcp_open 127.0.0.1 "$port"; then
    port_state="listening"
  fi
  printf '%-16s screen=%-8s port=%s\n' "$name" "$screen_state" "$port_state"
}

show_status() {
  local item
  for item in "${SERVICES[@]}"; do
    IFS='|' read -r service_name service_port_value _ <<<"$item"
    status_one "$service_name" "$service_port_value"
  done
  printf '%-16s screen=%-8s port=%s\n' "smartmeal-redis" "$(screen_exists smartmeal-redis && echo running || echo stopped)" "$(tcp_open 127.0.0.1 "$(split_port "$REDIS_ADDR")" && echo listening || echo closed)"
  printf '%-16s screen=%-8s port=%s\n' "smartmeal-nacos" "$(screen_exists smartmeal-nacos && echo running || echo stopped)" "$(tcp_open 127.0.0.1 "$(split_port "$NACOS_ADDR")" && echo listening || echo closed)"
  printf '%-16s screen=%-8s port=%s\n' "sentinel" "$(screen_exists sentinel-dashboard && echo running || echo stopped)" "$(tcp_open 127.0.0.1 "$(split_port "$SENTINEL_DASHBOARD")" && echo listening || echo closed)"
}

preflight() {
  require_cmd bash
  require_cmd java
  require_cmd mvn
  require_cmd screen
  require_cmd nc
  require_cmd lsof
}

case "$ACTION" in
  package)
    preflight
    package_jars
    ensure_jars
    ;;
  start)
    preflight
    [[ "$SKIP_BUILD" == "1" ]] || package_jars
    ensure_jars
    start_infra
    start_services
    show_status
    ;;
  restart)
    preflight
    stop_services
    [[ "$SKIP_BUILD" == "1" ]] || package_jars
    ensure_jars
    start_infra
    start_services
    show_status
    ;;
  stop)
    stop_services
    show_status
    ;;
  status)
    show_status
    ;;
  *)
    usage
    die "Unknown action: $ACTION"
    ;;
esac
