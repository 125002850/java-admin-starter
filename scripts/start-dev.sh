#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCKER_START_TIMEOUT="${DOCKER_START_TIMEOUT:-180}"
MYSQL_START_TIMEOUT="${MYSQL_START_TIMEOUT:-120}"

log() {
  printf '[start-dev] %s\n' "$*"
}

fail() {
  printf '[start-dev] ERROR: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "未找到命令 '$1'，请先安装后重试。"
}

wait_for_docker() {
  local elapsed=0

  while ! docker info >/dev/null 2>&1; do
    if (( elapsed >= DOCKER_START_TIMEOUT )); then
      fail "等待 Docker 启动超时（${DOCKER_START_TIMEOUT}s），请检查 Docker Desktop 或 docker.service。"
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
}

ensure_docker_started() {
  require_command docker

  if docker info >/dev/null 2>&1; then
    log "Docker 已启动。"
    return
  fi

  case "$(uname -s)" in
    Darwin)
      require_command open
      log "Docker 未启动，正在启动 Docker Desktop..."
      open -a Docker || fail "无法启动 Docker Desktop，请确认已经安装。"
      ;;
    Linux)
      require_command systemctl
      log "Docker 未启动，正在启动 docker.service..."
      if (( EUID == 0 )); then
        systemctl start docker
      else
        require_command sudo
        sudo systemctl start docker
      fi
      ;;
    *)
      fail "当前系统不支持自动启动 Docker，请手动启动后重试。"
      ;;
  esac

  wait_for_docker
  log "Docker 已就绪。"
}

ensure_mysql_started() {
  local container_id
  local status
  local elapsed=0

  log "正在启动本地 MySQL 容器..."
  docker compose -f "${ROOT_DIR}/compose.yaml" up -d mysql
  container_id="$(docker compose -f "${ROOT_DIR}/compose.yaml" ps -q mysql)"
  [[ -n "${container_id}" ]] || fail "MySQL 容器创建失败。"

  while true; do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}" 2>/dev/null || true)"
    case "${status}" in
      healthy)
        log "MySQL 已就绪（127.0.0.1:3300）。"
        return
        ;;
      exited|dead|unhealthy)
        docker compose -f "${ROOT_DIR}/compose.yaml" logs --tail=100 mysql >&2 || true
        fail "MySQL 容器状态异常：${status}。"
        ;;
    esac

    if (( elapsed >= MYSQL_START_TIMEOUT )); then
      docker compose -f "${ROOT_DIR}/compose.yaml" logs --tail=100 mysql >&2 || true
      fail "等待 MySQL 就绪超时（${MYSQL_START_TIMEOUT}s）。"
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
}

main() {
  require_command mvn

  if [[ -z "${JAVA_ADMIN_STARTER_DATASOURCE_URL:-}" ]]; then
    ensure_docker_started
    ensure_mysql_started
  else
    log "已配置远程数据源，跳过本地 Docker/MySQL 启动。"
  fi

  log "正在构建并安装 admin-boot 依赖模块..."
  mvn -f "${ROOT_DIR}/pom.xml" -pl admin-boot -am install -DskipTests

  log "正在以 dev profile 启动服务..."
  cd "${ROOT_DIR}/admin-boot"
  exec mvn spring-boot:run -Dspring-boot.run.profiles=dev "$@"
}

main "$@"
