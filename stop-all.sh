#!/usr/bin/env bash
# 防止用 sh(dash) 执行导致语法错误，自动切换回 bash
if [ -z "${BASH_VERSION:-}" ]; then
  exec bash "$0" "$@"
fi
# Careld 视力养护系统 - 一键停止脚本
# 用法: ./stop-all.sh   (按端口精确停止 7 个后端 + 3 个前端，避免 pkill 自匹配)

set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT/logs"

# 端口 -> 进程类型标记(仅用于日志展示)
declare -A PORTS=(
  [8281]="careld-auth-service"
  [8282]="careld-user-service"
  [8283]="careld-store-service"
  [8284]="careld-child-service"
  [8285]="careld-schedule-service"
  [8286]="careld-vision-service"
  [8287]="careld-sync-service"
  [5173]="parent-web"
  [5174]="admin-web"
  [5175]="store-web"
)

c_green=$'\033[32m'; c_red=$'\033[31m'; c_dim=$'\033[2m'; c_off=$'\033[0m'
ok(){ echo "${c_green}✓${c_off} $*"; }
err(){ echo "${c_red}✗${c_off} $*"; }
log(){ echo "${c_dim}[$(date +%H:%M:%S)]${c_off} $*"; }

pid_on_port() { (ss -tlnp 2>/dev/null || netstat -tlnp 2>/dev/null) | grep ":$1 " | grep -oE 'pid=[0-9]+' | head -1 | cut -d= -f2; }

echo "=== 停止 Careld 服务 ==="
# 先停前端, 再停后端
for p in 5173 5174 5175 8282 8283 8284 8285 8286 8287 8281; do
  pid=$(pid_on_port "$p")
  if [ -z "$pid" ]; then
    log ":$p ${PORTS[$p]} 未运行"
    continue
  fi
  log ":$p ${PORTS[$p]} pid=$pid -> SIGTERM"
  kill "$pid" 2>/dev/null
done

# 等待优雅退出, 仍未退出的强制结束
sleep 3
for p in 5173 5174 5175 8281 8282 8283 8284 8285 8286 8287; do
  pid=$(pid_on_port "$p")
  [ -n "$pid" ] && { log ":$p 仍存活 pid=$pid -> SIGKILL"; kill -9 "$pid" 2>/dev/null; }
done
sleep 1

echo
down=1
for p in 8281 8282 8283 8284 8285 8286 8287 5173 5174 5175; do
  pid=$(pid_on_port "$p")
  if [ -n "$pid" ]; then err ":$p ${PORTS[$p]} 仍在运行 pid=$pid"; down=0; fi
done
[ $down -eq 1 ] && ok "所有服务已停止" || { echo "${c_red}部分进程未停止,请手动检查${c_off}"; exit 1; }
