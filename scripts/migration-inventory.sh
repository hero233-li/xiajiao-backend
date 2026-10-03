#!/bin/sh
set -eu
# Uses the already built application's JDBC driver; no downloads or schema writes.
task_script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
task_driver=${MYSQL_JDBC_JAR:-}
task_temporary=
if [ -z "$task_driver" ]; then
  task_backend_jar=${BACKEND_JAR:-$task_script_dir/../target/backend-0.0.1-SNAPSHOT.jar}
  if [ ! -f "$task_backend_jar" ]; then
    echo 'Build the backend, or set MYSQL_JDBC_JAR to its MySQL JDBC driver.' >&2
    exit 1
  fi
  task_entry=$(unzip -Z1 "$task_backend_jar" | sed -n '/^BOOT-INF\/lib\/mysql-connector-j-.*\.jar$/p')
  if [ -z "$task_entry" ]; then echo 'Backend jar contains no MySQL JDBC driver.' >&2; exit 1; fi
  task_temporary=$(mktemp -d)
  trap 'rm -rf "$task_temporary"' EXIT HUP INT TERM
  task_driver=$task_temporary/mysql-driver.jar
  unzip -p "$task_backend_jar" "$task_entry" > "$task_driver"
fi
if [ ! -f "$task_driver" ]; then echo 'MySQL JDBC driver not found.' >&2; exit 1; fi
java --class-path "$task_driver" "$task_script_dir/MigrationInventory.java" "$@"
