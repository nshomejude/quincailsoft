#!/usr/bin/env bash
# Operator-only whole-database restore rehearsal. Never connects to a running database.
set -euo pipefail
umask 077

die() { printf '%s\n' "$*" >&2; exit 1; }
[[ $# == 3 ]] || die 'Usage: rehearse-postgres.sh TRUSTED_CUSTOM_ARCHIVE EXPECTED_SHA256 NEW_EVIDENCE_DIRECTORY'
archive=$(realpath -- "$1")
expected=$2
evidence=$(realpath -m -- "$3")
[[ -f "$archive" && "$expected" =~ ^[a-f0-9]{64}$ ]] || die 'Archive and lowercase SHA-256 required'
[[ ! -e "$evidence" ]] || die 'Evidence directory already exists; refusing overwrite'
actual=$(sha256sum -- "$archive"); actual=${actual%% *}
[[ "$actual" == "$expected" ]] || die 'Archive checksum mismatch'
[[ $(id -u) != 0 ]] || die 'Run as an unprivileged dedicated recovery operator'

# Network isolation must be established before parsing/restoring SQL from the archive.
# A namespace failure is fatal. No environment switch can skip this boundary.
command -v unshare >/dev/null || die 'Linux unshare with user/network namespaces required'
parent_network=$(readlink /proc/self/ns/net)
unshare --user --map-current-user --net bash -s -- "$archive" "$expected" "$evidence" "$parent_network" <<'ISOLATED'
set -euo pipefail
umask 077
archive=$1 expected=$2 evidence=$3 original_network=$4
[[ $(readlink /proc/self/ns/net) != "$original_network" ]] || { echo 'Network namespace isolation failed' >&2; exit 1; }
for executable in initdb pg_ctl postgres pg_restore psql; do
  command -v "$executable" >/dev/null || { echo "Missing PostgreSQL tool: $executable" >&2; exit 1; }
  [[ $("$executable" --version) =~ PostgreSQL\)\ 18\. ]] || { echo "PostgreSQL18 required: $executable" >&2; exit 1; }
done
# Never inherit credentials, service files or an external database destination.
unset PGHOST PGHOSTADDR PGPORT PGDATABASE PGUSER PGPASSWORD PGPASSFILE PGSERVICE PGSERVICEFILE PGOPTIONS
mkdir -- "$evidence"
work=$(mktemp -d /tmp/quincai-restore.XXXXXXXX)
started=false
cleanup() {
  result=$?
  if [[ -s "$work/data/postmaster.pid" ]]; then pg_ctl -D "$work/data" -m immediate -w stop >>"$evidence/server.log" 2>&1 || result=1; fi
  rm -rf -- "$work"
  if [[ $result == 0 ]]; then printf 'PASS: isolated whole-database restore rehearsal\n' >"$evidence/result.txt";
  else printf 'FAIL: restore rehearsal; inspect logs\n' >"$evidence/result.txt"; fi
  exit "$result"
}
trap cleanup EXIT
cp -- "$archive" "$work/source.dump"
copied=$(sha256sum -- "$work/source.dump"); copied=${copied%% *}
[[ "$copied" == "$expected" ]] || { echo 'Archive changed while staging' >&2; exit 1; }
archive="$work/source.dump"
mkdir "$work/socket"
initdb -D "$work/data" --auth-local=trust --auth-host=reject --no-locale -E UTF8 >"$evidence/initdb.log" 2>&1
# Private Unix socket only. New network namespace has no external interfaces/routes.
pg_ctl -D "$work/data" -o "-h '' -k '$work/socket' -p 55449" -l "$evidence/server.log" -w start
started=true
export PGHOST="$work/socket" PGPORT=55449 PGDATABASE=postgres PGUSER="$(id -un)"
psql -X -v ON_ERROR_STOP=1 -c 'CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS' >"$evidence/roles.log" 2>&1
pg_restore --list "$archive" >"$evidence/archive-toc.txt"
# Owner/grant translation is deliberate: this rehearsal proves archive restoration,
# not production runtime privileges or tenant cutover authorization.
pg_restore --exit-on-error --single-transaction --no-owner --no-acl --dbname=postgres "$archive" >"$evidence/restore.log" 2>&1
psql -X -v ON_ERROR_STOP=1 -At <<'SQL' >"$evidence/table-counts.tsv"
SELECT format('SELECT %L || E''\t'' || count(*)::text FROM %I.%I;', n.nspname || '.' || c.relname, n.nspname, c.relname)
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE c.relkind IN ('r','p') AND n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%'
ORDER BY n.nspname,c.relname
\gexec
SQL
printf '%s\n' "$expected" >"$evidence/archive.sha256"
postgres --version >"$evidence/postgresql-version.txt"
printf 'network_namespace=%s\nlisten_addresses=empty\napplication_workers=not_started\n' "$(readlink /proc/self/ns/net)" >"$evidence/isolation.txt"
cat /proc/net/route >"$evidence/network-routes.txt"
ISOLATED
