#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${LOG_DIR:-$ROOT_DIR/logs/startup}"
NACOS_HOME="${NACOS_HOME:-$ROOT_DIR/nacos-server-2.2.3}"
SENTINEL_HOME="${SENTINEL_HOME:-$ROOT_DIR/sentinel-1.8.9}"

GATEWAY_PORT="${GATEWAY_PORT:-8080}"
AUTH_PORT="${AUTH_PORT:-9200}"
SYSTEM_PORT="${SYSTEM_PORT:-9201}"
COOK_PORT="${COOK_PORT:-9210}"
ADMIN_PORT="${ADMIN_PORT:-5173}"
USER_PORT="${USER_PORT:-5174}"

NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-public}"
NACOS_USERNAME="${NACOS_USERNAME:-371}"
NACOS_PASSWORD="${NACOS_PASSWORD:-change_me}"
NACOS_DISCOVERY_IP="${NACOS_DISCOVERY_IP:-127.0.0.1}"

REDIS_ADDR="${REDIS_ADDR:-127.0.0.1:6379}"
SENTINEL_DASHBOARD="${SENTINEL_DASHBOARD:-127.0.0.1:8718}"
SENTINEL_NACOS_ADDR="${SENTINEL_NACOS_ADDR:-$NACOS_ADDR}"
SENTINEL_NACOS_NAMESPACE="${SENTINEL_NACOS_NAMESPACE:-$NACOS_NAMESPACE}"

ACTION="${1:-start}"
shift || true

usage() {
  cat <<EOF
Usage:
  bash scripts/start-company-dev.sh [start|restart|stop|status|infra] [--skip-build] [--no-wait]

Defaults:
  NACOS_ADDR=$NACOS_ADDR
  NACOS_NAMESPACE=$NACOS_NAMESPACE
  NACOS_USERNAME=$NACOS_USERNAME
  REDIS_ADDR=$REDIS_ADDR
  SENTINEL_DASHBOARD=$SENTINEL_DASHBOARD
  GATEWAY_PORT=$GATEWAY_PORT
  AUTH_PORT=$AUTH_PORT
  SYSTEM_PORT=$SYSTEM_PORT
  COOK_PORT=$COOK_PORT
  ADMIN_PORT=$ADMIN_PORT
  USER_PORT=$USER_PORT

Notes:
  - Local Nacos uses $NACOS_HOME/conf/application.properties, which points to the company config DB.
  - Redis stays local for now.
  - Extra flags are passed through to scripts/start-all.sh.
EOF
}

log() {
  printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

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

wait_tcp() {
  local name="$1"
  local addr="$2"
  local seconds="${3:-90}"
  local deadline=$((SECONDS + seconds))
  while [[ $SECONDS -lt $deadline ]]; do
    if tcp_open "$addr"; then
      log "$name is listening on $addr"
      return 0
    fi
    sleep 2
  done
  die "$name did not listen on $addr within ${seconds}s"
}

wait_url() {
  local name="$1"
  local url="$2"
  local seconds="${3:-90}"
  local deadline=$((SECONDS + seconds))
  while [[ $SECONDS -lt $deadline ]]; do
    if curl -fsS --max-time 2 "$url" >/dev/null 2>&1; then
      log "$name is ready: $url"
      return 0
    fi
    sleep 2
  done
  die "$name did not become ready: $url"
}

start_nacos() {
  require_cmd java
  require_cmd curl
  require_cmd nc
  require_cmd screen

  [[ -d "$NACOS_HOME" ]] || die "Nacos directory missing: $NACOS_HOME"
  [[ -f "$NACOS_HOME/conf/application.properties" ]] || die "Nacos config missing: $NACOS_HOME/conf/application.properties"

  if tcp_open "$NACOS_ADDR"; then
    log "Nacos already listening on $NACOS_ADDR"
    return 0
  fi

  log "Starting local Nacos in standalone mode; config DB is defined in $NACOS_HOME/conf/application.properties"
  mkdir -p "$NACOS_HOME/logs"
  screen -dmS smartmeal-nacos bash -lc '
    cd "$1" || exit 1
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
    echo "$java_bin $java_opt" > "$base_dir/logs/start.out"
    exec "$java_bin" $java_opt >> "$base_dir/logs/start.out" 2>&1
  ' _ "$NACOS_HOME"
  wait_tcp "Nacos" "$NACOS_ADDR" 120
  wait_tcp "Nacos gRPC" "127.0.0.1:9848" 120
  wait_url "Nacos health" "http://127.0.0.1:8848/nacos/actuator/health" 120
}

start_sentinel() {
  require_cmd java
  require_cmd screen
  require_cmd nc

  local jar="$SENTINEL_HOME/sentinel-dashboard-1.8.9.jar"
  local port
  port="$(split_port "$SENTINEL_DASHBOARD")"
  [[ -f "$jar" ]] || die "Sentinel jar missing: $jar"

  if tcp_open "$SENTINEL_DASHBOARD"; then
    log "Sentinel already listening on $SENTINEL_DASHBOARD"
    return 0
  fi

  mkdir -p "$LOG_DIR"
  log "Starting Sentinel dashboard on $SENTINEL_DASHBOARD"
  screen -dmS sentinel-dashboard bash -lc '
    cd "$1" || exit 1
    exec java -Dserver.port="$2" \
      -Dcsp.sentinel.dashboard.server=127.0.0.1:"$2" \
      -Dproject.name=sentinel-dashboard \
      -Dcsp.sentinel.api.port=8719 \
      -jar sentinel-dashboard-1.8.9.jar > "$3" 2>&1
  ' _ "$SENTINEL_HOME" "$port" "$LOG_DIR/sentinel-dashboard.log"
  wait_tcp "Sentinel" "$SENTINEL_DASHBOARD" 60
}

start_redis() {
  require_cmd nc
  if tcp_open "$REDIS_ADDR"; then
    log "Redis already listening on $REDIS_ADDR"
    return 0
  fi

  require_cmd redis-server
  require_cmd screen
  mkdir -p "$LOG_DIR"
  log "Starting local Redis on $REDIS_ADDR"
  screen -dmS smartmeal-redis bash -lc '
    exec redis-server --bind 127.0.0.1 --port 6379 --save "" --appendonly no > "$1" 2>&1
  ' _ "$LOG_DIR/redis.log"
  wait_tcp "Redis" "$REDIS_ADDR" 30
}

start_infra() {
  start_redis
  start_nacos
  start_sentinel
}

stop_infra() {
  if screen_exists sentinel-dashboard; then
    log "Stopping Sentinel dashboard"
    screen -S sentinel-dashboard -X quit || true
  fi
  if screen_exists smartmeal-redis; then
    log "Stopping Redis started by this script"
    screen -S smartmeal-redis -X quit || true
  fi
  if screen_exists smartmeal-nacos; then
    log "Stopping Nacos screen session"
    screen -S smartmeal-nacos -X quit || true
  fi
  if [[ -x "$NACOS_HOME/bin/shutdown.sh" ]]; then
    log "Stopping Nacos"
    (cd "$NACOS_HOME/bin" && bash shutdown.sh) || true
  fi
}

status() {
  printf '%-18s %s\n' "Nacos" "$(tcp_open "$NACOS_ADDR" && echo listening || echo closed) $NACOS_ADDR"
  printf '%-18s %s\n' "Sentinel" "$(tcp_open "$SENTINEL_DASHBOARD" && echo listening || echo closed) $SENTINEL_DASHBOARD"
  printf '%-18s %s\n' "Redis" "$(tcp_open "$REDIS_ADDR" && echo listening || echo closed) $REDIS_ADDR"
  (
    cd "$ROOT_DIR"
    GATEWAY_PORT="$GATEWAY_PORT" \
    AUTH_PORT="$AUTH_PORT" \
    SYSTEM_PORT="$SYSTEM_PORT" \
    COOK_PORT="$COOK_PORT" \
    ADMIN_PORT="$ADMIN_PORT" \
    USER_PORT="$USER_PORT" \
    NACOS_ADDR="$NACOS_ADDR" \
    NACOS_NAMESPACE="$NACOS_NAMESPACE" \
    NACOS_USERNAME="$NACOS_USERNAME" \
    NACOS_PASSWORD="$NACOS_PASSWORD" \
    NACOS_DISCOVERY_IP="$NACOS_DISCOVERY_IP" \
    REDIS_ADDR="$REDIS_ADDR" \
    SENTINEL_DASHBOARD="$SENTINEL_DASHBOARD" \
    SENTINEL_NACOS_ADDR="$SENTINEL_NACOS_ADDR" \
    SENTINEL_NACOS_NAMESPACE="$SENTINEL_NACOS_NAMESPACE" \
    bash scripts/start-all.sh status
  )
}

start_backend() {
  (
    cd "$ROOT_DIR"
    GATEWAY_PORT="$GATEWAY_PORT" \
    AUTH_PORT="$AUTH_PORT" \
    SYSTEM_PORT="$SYSTEM_PORT" \
    COOK_PORT="$COOK_PORT" \
    ADMIN_PORT="$ADMIN_PORT" \
    USER_PORT="$USER_PORT" \
    NACOS_ADDR="$NACOS_ADDR" \
    NACOS_NAMESPACE="$NACOS_NAMESPACE" \
    NACOS_USERNAME="$NACOS_USERNAME" \
    NACOS_PASSWORD="$NACOS_PASSWORD" \
    NACOS_DISCOVERY_IP="$NACOS_DISCOVERY_IP" \
    REDIS_ADDR="$REDIS_ADDR" \
    SENTINEL_DASHBOARD="$SENTINEL_DASHBOARD" \
    SENTINEL_NACOS_ADDR="$SENTINEL_NACOS_ADDR" \
    SENTINEL_NACOS_NAMESPACE="$SENTINEL_NACOS_NAMESPACE" \
    bash scripts/start-all.sh start "$@"
  )
}

restart_backend() {
  (
    cd "$ROOT_DIR"
    GATEWAY_PORT="$GATEWAY_PORT" \
    AUTH_PORT="$AUTH_PORT" \
    SYSTEM_PORT="$SYSTEM_PORT" \
    COOK_PORT="$COOK_PORT" \
    ADMIN_PORT="$ADMIN_PORT" \
    USER_PORT="$USER_PORT" \
    NACOS_ADDR="$NACOS_ADDR" \
    NACOS_NAMESPACE="$NACOS_NAMESPACE" \
    NACOS_USERNAME="$NACOS_USERNAME" \
    NACOS_PASSWORD="$NACOS_PASSWORD" \
    NACOS_DISCOVERY_IP="$NACOS_DISCOVERY_IP" \
    REDIS_ADDR="$REDIS_ADDR" \
    SENTINEL_DASHBOARD="$SENTINEL_DASHBOARD" \
    SENTINEL_NACOS_ADDR="$SENTINEL_NACOS_ADDR" \
    SENTINEL_NACOS_NAMESPACE="$SENTINEL_NACOS_NAMESPACE" \
    bash scripts/start-all.sh restart "$@"
  )
}

case "$ACTION" in
  start)
    start_infra
    start_backend "$@"
    ;;
  restart)
    start_infra
    restart_backend "$@"
    ;;
  infra)
    start_infra
    status
    ;;
  stop)
    (cd "$ROOT_DIR" && bash scripts/start-all.sh stop)
    stop_infra
    status
    ;;
  status)
    status
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    die "Unknown action: $ACTION"
    ;;
esac
