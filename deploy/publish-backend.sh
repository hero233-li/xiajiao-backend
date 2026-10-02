#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# 1Panel may execute as root; keep Git, backups and the lock under one user.
if [[ $(id -u) == 0 ]]; then
    exec /usr/bin/sudo -n -H -u ubuntu /bin/bash "$0" "$@"
fi
[[ $(id -un) == ubuntu ]] || { echo '请使用 ubuntu 或 root 用户执行。'; exit 1; }
case "${1:-backend}" in
    backend) mode=publish ;;
    check) mode=check ;;
    *) echo '用法：deploy.sh backend 或 deploy.sh check'; exit 2 ;;
esac

root=/opt/projects/xiajao/backend-java
state=/opt/projects/xiajao/.deploy
repository=https://github.com/hero233-li/xiajiao-backend.git
database_container=1Panel-mysql-uQU5
compose="$root/deploy/compose.yml"
config="$root/deploy/.env"
mkdir -p "$state" "$state/releases" "$root/deploy/backups"
exec 9>"$state/backend.lock"
flock -n 9 || { echo '已有发布任务正在执行，请等待它完成。'; exit 1; }
export GIT_TERMINAL_PROMPT=0
[[ -s "$config" && -s "$compose" ]] || { echo '缺少服务器运行配置。'; exit 1; }

compose_run() { docker compose --env-file "$config" -f "$compose" "$@"; }
mysql_query() {
    docker exec -i "$database_container" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N xuexizhitu'
}
schema_signature() {
    printf '%s\n' 'SELECT installed_rank,version,checksum,success FROM flyway_schema_history ORDER BY installed_rank;' | mysql_query | sha256sum | cut -d' ' -f1
}
set_image() {
    python3 - "$config" "$1" <<'PY'
from pathlib import Path
import sys, os
p=Path(sys.argv[1])
lines=[line for line in p.read_text().splitlines() if not line.startswith('BACKEND_IMAGE=')]
lines.append('BACKEND_IMAGE='+sys.argv[2])
temporary=p.with_suffix('.env.tmp')
temporary.write_text('\n'.join(lines)+'\n')
temporary.chmod(0o600)
os.replace(temporary,p)
PY
}
healthy() {
    curl --fail --silent --show-error --max-time 5 http://127.0.0.1:8080/api/v1/health 2>/dev/null |
        python3 -c 'import json,sys; r=json.load(sys.stdin); sys.exit(0 if r.get("code")==0 and r.get("data",{}).get("status")=="UP" else 1)' 2>/dev/null
}
wait_healthy() {
    for attempt in $(seq 1 45); do
        if healthy; then return 0; fi
        sleep 2
    done
    return 1
}

echo '正在读取 GitHub main 分支……'
if [[ ! -d "$state/backend.git" ]]; then
    git clone --bare "$repository" "$state/backend.git"
fi
git --git-dir="$state/backend.git" fetch --prune origin '+refs/heads/main:refs/heads/main'
revision=$(git --git-dir="$state/backend.git" rev-parse main)
echo "目标版本：$revision"
docker network inspect xiajiao-network >/dev/null
docker network inspect 1panel-network >/dev/null
printf 'SELECT 1;\n' | mysql_query >/dev/null
docker inspect xiajiao-backend >/dev/null
if [[ "$mode" == check ]]; then
    healthy
    echo '仓库、数据库、网络和当前服务检查通过；未发布。'
    exit 0
fi

stamp=$(date -u +%Y%m%dT%H%M%SZ)
release="$state/releases/$stamp-${revision:0:12}"
mkdir "$release"
git --git-dir="$state/backend.git" archive "$revision" | tar -x -C "$release"
image="xiajiao-backend:git-${revision:0:12}"
old_image_id=$(docker inspect xiajiao-backend --format '{{.Image}}')
old_image="xiajiao-backend:rollback-$stamp"
docker image tag "$old_image_id" "$old_image"
echo '正在构建新镜像并运行基础测试，现有服务继续运行……'
docker build -f "$root/deploy/Dockerfile.git" -t "$image" "$release"

backup="$root/deploy/backups/$stamp-${revision:0:12}"
mkdir "$backup"
printf '%s\n' "$revision" > "$backup/target-revision.txt"
printf '%s\n' "$old_image" > "$backup/previous-image.txt"
cp "$config" "$backup/runtime.env"
old_schema=$(schema_signature)
paused=0
switched=0
completed=0
finish() {
    rc=$?
    trap - EXIT
    if (( completed == 0 && rc != 0 )); then
        echo "发布未完成，备份目录：$backup"
        if (( switched == 1 )); then
            docker logs --tail=60 xiajiao-backend || true
            current_schema=$(schema_signature 2>/dev/null) || current_schema=unknown
            if [[ "$current_schema" == "$old_schema" ]]; then
                echo '数据库迁移版本未改变，正在恢复旧镜像……'
                set_image "$old_image"
                compose_run up -d --no-build --pull never --force-recreate backend && wait_healthy && echo '旧版本已恢复。' || echo '恢复失败，请查看容器日志。'
            else
                echo '数据库迁移版本已经变化，不自动回退程序或数据库。请依据本次备份处理。'
            fi
        elif (( paused == 1 )); then
            docker start xiajiao-backend >/dev/null || true
        fi
    fi
    exit "$rc"
}
trap finish EXIT
echo '开始备份数据库和附件，后端将短暂停止……'
paused=1
docker stop --time=30 xiajiao-backend >/dev/null
docker exec "$database_container" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers --events --hex-blob --no-tablespaces --set-gtid-purged=OFF xuexizhitu' > "$backup/database.sql"
test -s "$backup/database.sql"
sudo -n tar -czf - -C "$root/data" private > "$backup/private-files.tar.gz"
sha256sum "$backup/database.sql" "$backup/private-files.tar.gz" > "$backup/SHA256SUMS"

echo '备份完成，开始更新容器……'
switched=1
set_image "$image"
compose_run up -d --no-build --pull never --force-recreate backend
wait_healthy || { echo '新版本健康检查未通过。'; exit 1; }
# Authentication must reach Spring Security, rather than a static SPA response.
status=$(curl --silent --max-time 5 -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/v1/auth/me)
[[ "$status" == 401 ]] || { echo '认证接口检查未通过。'; exit 1; }
printf '%s\n' "$revision" > "$state/backend-deployed-revision"
completed=1
echo "发布成功：$revision"
echo "数据备份：$backup"
echo '数据库、附件及账号已保留。执行记录中的时间为 UTC。'
