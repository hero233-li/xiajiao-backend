#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
repository=$(cd "$(dirname "$0")/.." && pwd)
server=${1:-learning}
# Fetch only main; archive the remote revision, never uncommitted local changes.
git -C "$repository" fetch git@github.com:hero233-li/xiajiao-backend.git main
revision=$(git -C "$repository" rev-parse FETCH_HEAD)
work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT
archive="$work/backend-$revision.tar.gz"
git -C "$repository" archive --format=tar.gz -o "$archive" "$revision"
checksum=$(shasum -a 256 "$archive" | cut -d' ' -f1)
remote=/opt/projects/xiajao/.deploy/uploads
ssh "$server" "mkdir -p '$remote' && chmod 700 '$remote'"
scp "$archive" "$server:$remote/backend-$revision.tar.gz"
ssh "$server" "/bin/bash /opt/projects/xiajao/deploy.sh upload '$revision' '$remote/backend-$revision.tar.gz' '$checksum'"
