#!/usr/bin/env bash
# 防止用 sh(dash) 执行导致语法错误，自动切换回 bash
if [ -z "${BASH_VERSION:-}" ]; then
  exec bash "$0" "$@"
fi
# Careld 视力养护系统 - 一键启动脚本
# 用法:
#   ./start-all.sh              直接用已构建的 jar 启动后端 + 前端
#   ./start-all.sh --build      先重新构建所有后端 jar 再启动
#   ./start-all.sh --no-frontend  只启后端
#   ./start-all.sh --build --no-frontend
# 重复执行是安全的：已在运行的服务会跳过。

set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="2.0.1"
LOG_DIR="$ROOT/logs"
ENV_FILE="$ROOT/backend/.env"

# 后端服务: 端口 -> 服务名(目录名)
declare -A SVC=(
  [8281]="careld-auth-service"
  [8282]="careld-user-service"
  [8283]="careld-store-service"
  [8284]="careld-child-service"
  [8285]="careld-schedule-service"
  [8286]="careld-vision-service"
  [8287]="careld-sync-service"
)
# auth 必须先启动，其余按此顺序(并行)
BOOT_ORDER=("8281" "8282" "8283" "8284" "8285" "8286" "8287")

# 前端: 目录 -> 端口
declare -A WEB=(
  [parent-web]="5173"
  [admin-web]="5174"
  [store-web]="5175"
)

BUILD=0
WITH_FRONTEND=1
for arg in "$@"; do
  case "$arg" in
    --build) BUILD=1 ;;
    --no-frontend) WITH_FRONTEND=0 ;;
    *) echo "未知参数: $arg"; exit 1 ;;
  esac
done

mkdir -p "$LOG_DIR"

# ---------- 工具函数 ----------
c_green=$'\033[32m'; c_red=$'\033[31m'; c_yel=$'\033[33m'; c_dim=$'\033[2m'; c_off=$'\033[0m'

port_up() { (ss -tln 2>/dev/null || netstat -tln 2>/dev/null) | grep -q ":$1 "; }
pid_on_port() { (ss -tlnp 2>/dev/null || netstat -tlnp 2>/dev/null) | grep ":$1 " | grep -oE 'pid=[0-9]+' | head -1 | cut -d= -f2; }

wait_port() { # <port> <timeout_s>
  local port=$1 timeout=${2:-60} i
  for ((i=0; i<timeout*2; i++)); do
    port_up "$port" && return 0
    sleep 0.5
  done
  return 1
}

log() { echo "${c_dim}[$(date +%H:%M:%S)]${c_off} $*"; }
ok()  { echo "${c_green}✓${c_off} $*"; }
err() { echo "${c_red}✗${c_off} $*"; }
section() { echo; echo "${c_yel}=== $* ===${c_off}"; }

# ---------- 1. 前置依赖检查 ----------
section "前置依赖检查"
command -v java   >/dev/null && ok "Java:    $(java -version 2>&1 | head -1)" || { err "未找到 java"; exit 1; }
command -v mvn    >/dev/null && ok "Maven:  $(mvn -v 2>&1 | head -1)" || { err "未找到 mvn"; exit 1; }
[ -f "$ENV_FILE" ] && ok "env:    $ENV_FILE" || { err "缺少 $ENV_FILE (MySQL/Redis/JWT 凭据)"; exit 1; }
# 加载环境变量(供 java 进程继承)
set -a; . "$ENV_FILE"; set +a

if port_up 3306; then ok "MySQL :3306 监听中"; else err "MySQL :3306 未启动，请先启动"; fi
if port_up 6379; then ok "Redis :6379 监听中"; else err "Redis :6379 未启动，请先启动"; fi

# ---------- 2. (可选) 构建 ----------
if [ $BUILD -eq 1 ]; then
  section "构建后端 jar (mvn clean package)"
  ( cd "$ROOT/backend" && mvn clean package -DskipTests -q ) || { err "构建失败"; exit 1; }
  ok "构建完成"
fi

# ---------- 3. 启动后端 ----------
section "启动后端微服务"

start_backend() { # <port>
  local port=$1
  local svc=${SVC[$port]}
  local jar="$ROOT/backend/$svc/target/$svc-$VERSION.jar"
  if [ ! -f "$jar" ]; then
    err "$jar 不存在，请用: $0 --build"
    return 1
  fi
  if port_up "$port"; then
    log "$svc 已在运行 (pid=$(pid_on_port $port))，跳过"
    return 0
  fi
  nohup java -jar "$jar" > "$LOG_DIR/$svc.log" 2>&1 &
  log "启动 $svc :$port (pid=$!)"
}

# auth 先单独启动并等待就绪
start_backend 8281 || true
if wait_port 8281 90; then ok "auth-service :8281 就绪"; else err "auth-service :8281 启动失败，见 $LOG_DIR/careld-auth-service.log"; fi

# 其余并行启动
for p in 8282 8283 8284 8285 8286 8287; do start_backend "$p" || true; done

# 等待其余端口
fail=0
for p in 8282 8283 8284 8285 8286 8287; do
  if wait_port "$p" 90; then ok "${SVC[$p]} :$p 就绪"; else err "${SVC[$p]} :$p 失败"; fail=1; fi
done

# ---------- 4. 启动前端 ----------
if [ $WITH_FRONTEND -eq 1 ]; then
  section "启动前端 (Vite dev)"
  for w in "${!WEB[@]}"; do
    dir="$ROOT/frontend/$w"
    [ -d "$dir/node_modules" ] || { log "$w 缺 node_modules，运行: (cd $dir && npm install)"; }
    if port_up "${WEB[$w]}"; then
      log "$w :${WEB[$w]} 已在运行，跳过"
      continue
    fi
    nohup npm run dev --prefix "$dir" > "$LOG_DIR/$w.log" 2>&1 &
    log "启动 $w (pid=$!)"
  done
  for w in "${!WEB[@]}"; do
    if wait_port "${WEB[$w]}" 60; then ok "$w :${WEB[$w]} 就绪"; else err "$w :${WEB[$w]} 失败，见 $LOG_DIR/$w.log"; fi
  done
fi

# ---------- 5. 汇总 ----------
section "服务清单"
for p in 8281 8282 8283 8284 8285 8286 8287; do
  port_up "$p" && ok ":$p  ${SVC[$p]}" || err ":$p  ${SVC[$p]} 未运行"
done
if [ $WITH_FRONTEND -eq 1 ]; then
  echo
  for w in "${!WEB[@]}"; do
    port_up "${WEB[$w]}" && ok "http://localhost:${WEB[$w]}/  $w"
  done
fi

echo
ok "完成。日志目录: $LOG_DIR"
echo "${c_dim}停止服务: $ROOT/stop-all.sh${c_off}"
[ $fail -eq 0 ] || { err "部分后端服务启动失败，请查看日志"; exit 1; }
