#!/usr/bin/env bash
# Operator-only complete database archive + consistent source reconciliation manifest.
# Uses standard PG* connection settings/.pgpass; never accepts credentials as arguments.
set -euo pipefail
umask 077
[[ $# == 1 && ! -e "$1" ]] || { echo 'Usage: backup-postgres.sh NEW_PRIVATE_DIRECTORY' >&2; exit 1; }
root=$(cd "$(dirname "$0")" && pwd)
output=$(realpath -m -- "$1")
command -v pg_dump >/dev/null
[[ $(pg_dump --version) =~ PostgreSQL\)\ 18\. ]] || { echo 'PostgreSQL18 pg_dump required' >&2; exit 1; }
version=$(psql -X -qAt -v ON_ERROR_STOP=1 -c 'SHOW server_version_num')
[[ "$version" =~ ^18[0-9]{4}$ ]] || { echo 'PostgreSQL18 source required' >&2; exit 1; }
mkdir -- "$output"
snapshot_pid=''
cleanup() {
  status=$?
  if [[ -n "$snapshot_pid" ]]; then kill "$snapshot_pid" 2>/dev/null || true; wait "$snapshot_pid" 2>/dev/null || true; fi
  if [[ "$status" != 0 ]]; then printf 'FAIL: backup incomplete; no accepted manifest\n' >"$output/result.txt"; fi
  exit "$status"
}
trap cleanup EXIT
coproc SNAPSHOT { psql -X -qAt -v ON_ERROR_STOP=1 2>"$output/snapshot.log"; }
snapshot_pid=$SNAPSHOT_PID
snapshot_in=${SNAPSHOT[1]}; snapshot_out=${SNAPSHOT[0]}
printf 'BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY; SELECT pg_export_snapshot();\n' >&"$snapshot_in"
IFS= read -r -t 30 snapshot <&"$snapshot_out"
[[ "$snapshot" =~ ^[0-9A-Fa-f-]+$ ]] || { echo 'Invalid exported source snapshot' >&2; exit 1; }
pg_dump --format=custom --compress=0 --snapshot="$snapshot" --file="$output/source.dump" 2>"$output/dump.log"
"$root/fingerprint-postgres.sh" "$snapshot" >"$output/fingerprints.tsv"
printf 'COMMIT;\n\\q\n' >&"$snapshot_in"
wait "$snapshot_pid"; snapshot_pid=''
archive=$(sha256sum "$output/source.dump"); archive=${archive%% *}
manifest=$(sha256sum "$output/fingerprints.tsv"); manifest=${manifest%% *}
printf '{"format":"QUINCAISOFT-PG18-BACKUP-1","source_server_version":%s,"source_snapshot":"%s","archive_sha256":"%s","fingerprints_sha256":"%s","created_at":"%s"}\n' "$version" "$snapshot" "$archive" "$manifest" "$(date -u +%FT%TZ)" >"$output/backup-manifest.json.tmp"
mv "$output/backup-manifest.json.tmp" "$output/backup-manifest.json"
printf 'PASS: consistent PostgreSQL archive and source fingerprints created\n' >"$output/result.txt"
