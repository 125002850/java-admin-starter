#!/usr/bin/env bash
set -euo pipefail

verify_repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
verify_ref=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --ref)
            [[ $# -ge 2 && -n "$2" ]] || { echo '--ref 需要 Git 标签或提交。' >&2; exit 2; }
            verify_ref="$2"
            shift 2
            ;;
        -h|--help)
            echo '用法：scripts/testing/verify-flyway-mysql8.sh [--ref <标签或提交>]'
            echo '默认验证当前工作区；始终使用新建的临时 MySQL 容器，退出时清理容器和数据卷。'
            echo '默认镜像 mysql:8.0；FLYWAY_VERIFY_MYSQL_IMAGE 可覆盖镜像，DOCKER_DEFAULT_PLATFORM 可覆盖平台。'
            echo '日志保留在 target/flyway-mysql-verify/。'
            exit 0
            ;;
        *) echo "未知参数：$1" >&2; exit 2 ;;
    esac
done

verify_mysql_image="${FLYWAY_VERIFY_MYSQL_IMAGE:-mysql:8.0}"
[[ "$verify_mysql_image" =~ ^[a-zA-Z0-9][a-zA-Z0-9./:@_-]*$ ]] || { echo '无效的 MySQL 镜像引用。' >&2; exit 2; }
command -v docker >/dev/null || { echo '请先安装 Docker。' >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo 'Docker 未就绪，请先启动 Docker。' >&2; exit 1; }

mkdir -p "$verify_repo_root/target/flyway-mysql-verify"
verify_logs="$(mktemp -d "$verify_repo_root/target/flyway-mysql-verify/run.XXXXXX")"
verify_temp="$(mktemp -d "${TMPDIR:-/tmp}/oig-flyway-verify.XXXXXX")"
verify_source="$verify_temp/source"
verify_container=""
verify_password="$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')"
verify_database=oig_flyway_verify

verify_mysql() {
    MYSQL_PWD="$verify_password" docker exec --env MYSQL_PWD "$verify_container" \
        mysql --protocol=TCP -h127.0.0.1 -uroot --connect-timeout=3 --batch --skip-column-names \
        --database="$verify_database" --execute "$1"
}

verify_cleanup() {
    local verify_exit_code=$?
    trap - EXIT
    if [[ -s "$verify_temp/container.id" ]]; then
        verify_container="$(cat "$verify_temp/container.id")"
        if [[ "$verify_container" =~ ^[0-9a-f]{12,64}$ ]]; then
            docker logs "$verify_container" > "$verify_logs/mysql.log" 2>&1 || true
            verify_mysql 'SELECT version, description, type, checksum, success FROM flyway_schema_history ORDER BY installed_rank' \
                > "$verify_logs/flyway-history.tsv" 2> "$verify_logs/history-error.log" || true
            if ! docker rm -fv "$verify_container" > "$verify_logs/cleanup.log" 2>&1; then
                echo "容器清理失败：${verify_container}，请检查 $verify_logs/cleanup.log" >&2
                [[ $verify_exit_code -ne 0 ]] || verify_exit_code=1
            fi
        fi
    fi
    if [[ -d "$verify_source/boot/target/surefire-reports" ]]; then
        cp -R "$verify_source/boot/target/surefire-reports" "$verify_logs/tests" || true
    fi
    rm -rf "$verify_temp"
    echo "验证日志：$verify_logs"
    exit "$verify_exit_code"
}
trap verify_cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# 使用源码快照，避免清理或改写当前工作区的 Maven 编译产物。
mkdir -p "$verify_source"
if [[ -n "$verify_ref" ]]; then
    verify_commit="$(git -C "$verify_repo_root" rev-parse --verify "${verify_ref}^{commit}")"
    git -C "$verify_repo_root" archive "$verify_commit" | tar -xf - -C "$verify_source"
    echo "验证提交：$verify_commit ($verify_ref)"
else
    tar -C "$verify_repo_root" --exclude='.git' --exclude='target' \
        --exclude='logs' --exclude='tmp' --exclude='maven_repo' \
        --exclude='.idea' --exclude='.vscode' --exclude='*.log' -cf - . \
        | tar -xf - -C "$verify_source"
    echo '验证当前工作区源码快照。'
fi
for verify_sql in "$verify_source"/boot/src/main/resources/db/migration/V*__*.sql; do
    basename "$verify_sql" | sed -E 's/^V([0-9]+)__.*$/\1/'
done | LC_ALL=C sort -n > "$verify_logs/expected-versions.txt"

echo "启动临时 MySQL：$verify_mysql_image"
MYSQL_ROOT_PASSWORD="$verify_password" docker run -d \
    --cidfile "$verify_temp/container.id" \
    --name "oig-flyway-$(basename "$verify_logs")" \
    --publish 127.0.0.1::3306 \
    --env MYSQL_ROOT_PASSWORD --env MYSQL_DATABASE="$verify_database" --env MYSQL_ROOT_HOST=% \
    "$verify_mysql_image" --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci \
    --default-time-zone=+08:00 > "$verify_logs/container.id" 2> "$verify_logs/docker-run.log"
verify_container="$(cat "$verify_temp/container.id")"
verify_ready=false
for ((verify_attempt=0; verify_attempt<90; verify_attempt++)); do
    if verify_mysql 'SELECT 1' >/dev/null 2>&1; then
        verify_ready=true
        break
    fi
    [[ "$(docker inspect --format '{{.State.Running}}' "$verify_container")" == true ]] || break
    sleep 2
done
[[ "$verify_ready" == true ]] || { echo '临时 MySQL 启动失败或超时。' >&2; exit 1; }
[[ "$(verify_mysql 'SELECT VERSION()')" == 8.* ]] || { echo '必须使用 MySQL 8。' >&2; exit 1; }
[[ "$(verify_mysql 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()')" == 0 ]] \
    || { echo '验证库必须为空。' >&2; exit 1; }
verify_mapping="$(docker port "$verify_container" 3306/tcp)"
[[ "$verify_mapping" =~ ^127\.0\.0\.1:[0-9]+$ ]] || { echo '无法识别临时 MySQL 的本机端口。' >&2; exit 1; }
verify_port="${verify_mapping##*:}"
verify_jdbc="jdbc:mysql://127.0.0.1:$verify_port/$verify_database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia%2FShanghai"

# 不接受调用环境的数据源或外部 Spring 配置，测试库属于本次新建容器。
unset SPRING_APPLICATION_JSON SPRING_CONFIG_LOCATION SPRING_CONFIG_ADDITIONAL_LOCATION SPRING_PROFILES_ACTIVE
export JAVA_ADMIN_STARTER_DATASOURCE_URL="$verify_jdbc"
export JAVA_ADMIN_STARTER_DATASOURCE_USERNAME=root
export JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD="$verify_password"
export SPRING_DATASOURCE_URL="$verify_jdbc" SPRING_DATASOURCE_USERNAME=root SPRING_DATASOURCE_PASSWORD="$verify_password"
export SPRING_FLYWAY_URL="$verify_jdbc" SPRING_FLYWAY_USER=root SPRING_FLYWAY_PASSWORD="$verify_password"

echo "MySQL 已就绪（127.0.0.1:${verify_port}），执行全量迁移及结构验证。"
echo "Maven 日志：$verify_logs/maven.log"
if (
    cd "$verify_source"
    mvn -B -pl boot -am -Dtest=FlywaySmokeTests,ScheduleExecutionIntegrationTests \
        "-Dspring.datasource.url=$verify_jdbc" "-Dspring.datasource.hikari.jdbc-url=$verify_jdbc" -Dspring.datasource.username=root \
        "-Dspring.datasource.password=$verify_password" -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
        "-Dspring.flyway.url=$verify_jdbc" -Dspring.flyway.user=root "-Dspring.flyway.password=$verify_password" \
        -Dspring.flyway.enabled=true -Dspring.flyway.locations=classpath:db/migration -Dspring.flyway.target=latest \
        -Dspring.flyway.baseline-on-migrate=false -Dspring.flyway.out-of-order=false \
        -Dspring.config.location=classpath:/application.yml -Dspring.config.additional-location= \
        -Dspring.application.json= -Dspring.docker.compose.enabled=false test
) > "$verify_logs/maven.log" 2>&1; then
    verify_mysql 'SELECT version FROM flyway_schema_history WHERE version IS NOT NULL AND success=1 ORDER BY installed_rank' \
        > "$verify_logs/applied-versions.txt"
    diff -u "$verify_logs/expected-versions.txt" "$verify_logs/applied-versions.txt" \
        > "$verify_logs/versions.diff" || { echo '实际迁移列表与源码不一致。' >&2; exit 1; }
    echo "验证通过：从空库完整执行 $(wc -l < "$verify_logs/applied-versions.txt" | tr -d ' ') 条迁移。"
else
    verify_maven_exit=$?
    tail -n 50 "$verify_logs/maven.log" >&2
    exit "$verify_maven_exit"
fi
