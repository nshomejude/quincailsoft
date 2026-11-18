#!/usr/bin/env bash
# Disposable real-PostgreSQL18 acceptance check. No production connection used.
set -euo pipefail
umask 077
root=$(cd "$(dirname "$0")/../.." && pwd)
tool="$root/infrastructure/recovery/rehearse-postgres.sh"
work=$(mktemp -d /tmp/quincai-recovery-test.XXXXXXXX)
started=false
cleanup() {
  code=$?
  if $started; then pg_ctl -D "$work/source" -m immediate -w stop >/dev/null 2>&1 || code=1; fi
  if [[ $code == 0 ]]; then rm -rf -- "$work"; else echo "Failed test artifacts: $work" >&2; fi
  exit "$code"
}
trap cleanup EXIT
initdb -D "$work/source" --auth-local=trust --auth-host=reject --no-locale -E UTF8 >"$work/init.log" 2>&1
# Synthetic source only; isolated restore still mandates a private Unix socket.
printf 'host all all 127.0.0.1/32 trust\n' >"$work/source/pg_hba.conf"
pg_ctl -D "$work/source" -o "-h 127.0.0.1 -k '' -p 55448" -l "$work/server.log" -w start >/dev/null
started=true
unset PGHOSTADDR PGSERVICE PGSERVICEFILE PGPASSWORD PGPASSFILE PGOPTIONS
export PGHOST=127.0.0.1 PGPORT=55448 PGDATABASE=postgres PGUSER="$(id -un)"
psql -X -v ON_ERROR_STOP=1 -c 'CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS' >"$work/schema.log" 2>&1
# Immutable Desktop baseline, independent of concurrently authored forward migrations.
while IFS= read -r migration; do
  version=${migration##*/V}; version=${version%%__*}
  if (( version <= 22 )); then psql -X -v ON_ERROR_STOP=1 -f "$migration" >>"$work/schema.log" 2>&1; fi
done < <(printf '%s\n' "$root"/backend/db-migrations/src/main/resources/db/migration/V*.sql | sort -V)
psql -X -v ON_ERROR_STOP=1 <<'SQL' >>"$work/schema.log" 2>&1
INSERT INTO tenant(id,name) VALUES ('00000000-0000-0000-0000-000000000001','Synthetic recovery fixture');
INSERT INTO company(tenant_id,id,name) VALUES ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','Recovery company');
INSERT INTO branch(tenant_id,company_id,id,name) VALUES ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000003','Recovery branch');
SQL
pg_dump --format=custom --compress=0 --file="$work/source.dump"
digest=$(sha256sum "$work/source.dump"); digest=${digest%% *}
"$tool" "$work/source.dump" "$digest" "$work/evidence" >"$work/drill.log" 2>&1
grep -qx 'PASS: isolated whole-database restore rehearsal' "$work/evidence/result.txt"
grep -qx $'public.tenant\t1' "$work/evidence/table-counts.tsv"
grep -qx $'public.company\t1' "$work/evidence/table-counts.tsv"
grep -qx $'public.branch\t1' "$work/evidence/table-counts.tsv"
[[ $(wc -l <"$work/evidence/network-routes.txt") == 1 ]]
if "$tool" "$work/source.dump" "$digest" "$work/evidence" >"$work/overwrite.log" 2>&1; then echo 'Unexpected evidence overwrite' >&2; exit 1; fi
printf 'damaged archive' >"$work/damaged.dump"
if "$tool" "$work/damaged.dump" "$digest" "$work/bad-digest" >"$work/digest.log" 2>&1; then echo 'Unexpected checksum acceptance' >&2; exit 1; fi
[[ ! -e "$work/bad-digest" ]]
bad=$(sha256sum "$work/damaged.dump"); bad=${bad%% *}
if "$tool" "$work/damaged.dump" "$bad" "$work/bad-format" >"$work/format.log" 2>&1; then echo 'Unexpected archive acceptance' >&2; exit 1; fi
grep -qx 'FAIL: restore rehearsal; inspect logs' "$work/bad-format/result.txt"
echo 'PASS: V1–V22 restore; tenant/company/branch rows; no network routes; overwrite/digest/archive rejection'
