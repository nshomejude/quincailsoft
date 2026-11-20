#!/usr/bin/env bash
# Operator-only whole-database restore rehearsal. Never connects to a running database.
set -euo pipefail
umask 077

die() { printf '%s\n' "$*" >&2; exit 1; }
[[ $# == 5 ]] || die 'Usage: rehearse-postgres.sh TRUSTED_CUSTOM_ARCHIVE ARCHIVE_SHA256 SOURCE_FINGERPRINTS FINGERPRINTS_SHA256 NEW_EVIDENCE_DIRECTORY'
archive=$(realpath -- "$1")
expected=$2
fingerprints=$(realpath -- "$3")
fingerprints_expected=$4
evidence=$(realpath -m -- "$5")
root=$(cd "$(dirname "$0")" && pwd)
[[ -f "$archive" && -f "$fingerprints" && "$expected" =~ ^[a-f0-9]{64}$ && "$fingerprints_expected" =~ ^[a-f0-9]{64}$ ]] || die 'Archive, source fingerprints and lowercase SHA-256 digests required'
[[ ! -e "$evidence" ]] || die 'Evidence directory already exists; refusing overwrite'
actual=$(sha256sum -- "$archive"); actual=${actual%% *}
[[ "$actual" == "$expected" ]] || die 'Archive checksum mismatch'
actual=$(sha256sum -- "$fingerprints"); actual=${actual%% *}
[[ "$actual" == "$fingerprints_expected" ]] || die 'Source fingerprint checksum mismatch'
[[ $(id -u) != 0 ]] || die 'Run as an unprivileged dedicated recovery operator'

# Network isolation must be established before parsing/restoring SQL from the archive.
# A namespace failure is fatal. No environment switch can skip this boundary.
command -v unshare >/dev/null || die 'Linux unshare with user/network namespaces required'
command -v setpriv >/dev/null || die 'setpriv required to drop recovery setup capabilities'
command -v cc >/dev/null || die 'C compiler required for the fixed loopback setup helper'
helperdir=$(mktemp -d /tmp/quincai-recovery-net.XXXXXXXX)
trap 'rm -rf -- "$helperdir"' EXIT
cc -O2 -Wall -Wextra -Werror "$root/isolated-loopback.c" -o "$helperdir/isolated-loopback"
parent_network=$(readlink /proc/self/ns/net)
unshare --user --map-current-user --keep-caps --net bash -s -- "$archive" "$expected" "$fingerprints" "$fingerprints_expected" "$evidence" "$parent_network" "$helperdir/isolated-loopback" "$root" <<'ISOLATED'
set -euo pipefail
umask 077
[[ $(readlink /proc/self/ns/net) != "$6" ]] || { echo 'Network namespace isolation failed' >&2; exit 1; }
"$7" "$6"
# Capabilities exist only while bringing up the isolated loopback device. The
# database and archive SQL run with no capabilities or privilege escalation.
exec setpriv --bounding-set=-all --inh-caps=-all --ambient-caps=-all --no-new-privs bash -s -- "$1" "$2" "$3" "$4" "$5" "$6" "$8" <<'RESTORE'
set -euo pipefail
umask 077
archive=$1 expected=$2 fingerprints=$3 fingerprints_expected=$4 evidence=$5 original_network=$6 root=$7
[[ $(readlink /proc/self/ns/net) != "$original_network" ]] || { echo 'Network namespace isolation failed' >&2; exit 1; }
[[ $(awk '/^Cap(Inh|Prm|Eff|Bnd|Amb):/{if($2!="0000000000000000")bad=1} END{print bad+0}' /proc/self/status) == 0 ]] || { echo 'Recovery capabilities were not dropped' >&2; exit 1; }
[[ $(awk '/^NoNewPrivs:/{print $2}' /proc/self/status) == 1 ]] || { echo 'Privilege escalation is not disabled' >&2; exit 1; }
[[ $(wc -l </proc/net/route) == 1 ]] || { echo 'Unexpected external network route' >&2; exit 1; }
[[ $(awk -F: 'NR>2{gsub(/ /,"",$1);print $1}' /proc/net/dev) == lo ]] || { echo 'Unexpected non-loopback network interface' >&2; exit 1; }
for executable in initdb pg_ctl postgres pg_restore psql; do
  command -v "$executable" >/dev/null || { echo "Missing PostgreSQL tool: $executable" >&2; exit 1; }
  [[ $("$executable" --version) =~ PostgreSQL\)\ 18\. ]] || { echo "PostgreSQL18 required: $executable" >&2; exit 1; }
done
# Never inherit credentials, service files or an external database destination.
unset PGHOST PGHOSTADDR PGPORT PGDATABASE PGUSER PGPASSWORD PGPASSFILE PGSERVICE PGSERVICEFILE PGOPTIONS
mkdir -- "$evidence"
work=$(mktemp -d /tmp/quincai-restore.XXXXXXXX)
snapshot_pid=''
cleanup() {
  result=$?
  if [[ -n "$snapshot_pid" ]]; then kill "$snapshot_pid" 2>/dev/null || true; wait "$snapshot_pid" 2>/dev/null || true; fi
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
cp -- "$fingerprints" "$work/expected.tsv"
copied=$(sha256sum -- "$work/expected.tsv"); copied=${copied%% *}
[[ "$copied" == "$fingerprints_expected" ]] || { echo 'Source fingerprints changed while staging' >&2; exit 1; }
initdb -D "$work/data" --auth-local=trust --auth-host=reject --no-locale -E UTF8 >"$evidence/initdb.log" 2>&1
# This loopback is INSIDE the private namespace, not the host network. No external
# route/interface exists and no capabilities remain to add one. No Unix sockets.
printf 'host all all 127.0.0.1/32 trust\n' >"$work/data/pg_hba.conf"
pg_ctl -D "$work/data" -o "-h 127.0.0.1 -k '' -p 55449" -l "$evidence/server.log" -w start
export PGHOST=127.0.0.1 PGPORT=55449 PGDATABASE=postgres PGUSER="$(id -un)"
psql -X -v ON_ERROR_STOP=1 -c 'CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS' >"$evidence/roles.log" 2>&1
pg_restore --list "$archive" >"$evidence/archive-toc.txt"
# Owner/grant translation is deliberate: this rehearsal proves archive restoration,
# not production runtime privileges or tenant cutover authorization.
pg_restore --exit-on-error --single-transaction --no-owner --no-acl --dbname=postgres "$archive" >"$evidence/restore.log" 2>&1
coproc SNAPSHOT { psql -X -qAt -v ON_ERROR_STOP=1 2>"$evidence/snapshot.log"; }
snapshot_pid=$SNAPSHOT_PID
snapshot_in=${SNAPSHOT[1]}; snapshot_out=${SNAPSHOT[0]}
printf 'BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY; SELECT pg_export_snapshot();\n' >&"$snapshot_in"
IFS= read -r -t 30 snapshot <&"$snapshot_out"
"$root/fingerprint-postgres.sh" "$snapshot" >"$evidence/restored-fingerprints.tsv"
printf 'COMMIT;\n\\q\n' >&"$snapshot_in"
wait "$snapshot_pid"; snapshot_pid=''
cut -f1,2 "$evidence/restored-fingerprints.tsv" >"$evidence/table-counts.tsv"
diff -u "$work/expected.tsv" "$evidence/restored-fingerprints.tsv" >"$evidence/reconciliation.diff"
printf '%s\n' "$expected" >"$evidence/archive.sha256"
postgres --version >"$evidence/postgresql-version.txt"
printf '%s\n' "$fingerprints_expected" >"$evidence/source-fingerprints.sha256"
printf 'network_namespace=%s\nlisten_addresses=isolated-loopback-only\napplication_workers=not_started\n' "$(readlink /proc/self/ns/net)" >"$evidence/isolation.txt"
awk '/^Cap(Inh|Prm|Eff|Bnd|Amb):|^NoNewPrivs:/' /proc/self/status >>"$evidence/isolation.txt"
cat /proc/net/route >"$evidence/network-routes.txt"
RESTORE
ISOLATED
