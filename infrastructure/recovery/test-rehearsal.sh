#!/usr/bin/env bash
# Disposable real-PostgreSQL18 acceptance check. No production connection used.
set -euo pipefail
umask 077
[[ $# -le 1 ]] || { echo 'Usage: test-rehearsal.sh [NEW_EVIDENCE_DIRECTORY]' >&2; exit 1; }
destination=''
if [[ $# == 1 ]]; then destination=$(realpath -m -- "$1"); [[ ! -e "$destination" ]] || { echo 'Evidence destination exists' >&2; exit 1; }; fi
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
# Synthetic source only; restored database uses a separate private network namespace.
printf 'host all all 127.0.0.1/32 trust\n' >"$work/source/pg_hba.conf"
pg_ctl -D "$work/source" -o "-h 127.0.0.1 -k '' -p 55448" -l "$work/server.log" -w start >/dev/null
started=true
unset PGHOSTADDR PGSERVICE PGSERVICEFILE PGPASSWORD PGPASSFILE PGOPTIONS
export PGHOST=127.0.0.1 PGPORT=55448 PGDATABASE=postgres PGUSER="$(id -un)"
psql -X -v ON_ERROR_STOP=1 -c 'CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS' >"$work/schema.log" 2>&1
# Bind evidence to exact migrations and refuse a concurrently changed schema source.
(cd "$root" && sha256sum backend/db-migrations/src/main/resources/db/migration/V*.sql) >"$work/migrations.sha256"
# Apply all current repository migrations; freeze migration writers before this test.
while IFS= read -r migration; do
  version=${migration##*/V}; version=${version%%__*}
  psql -X -v ON_ERROR_STOP=1 -f "$migration" >>"$work/schema.log" 2>&1
done < <(printf '%s\n' "$root"/backend/db-migrations/src/main/resources/db/migration/V*.sql | sort -V)
psql -X -v ON_ERROR_STOP=1 <<'SQL' >>"$work/schema.log" 2>&1
INSERT INTO tenant(id,name) VALUES ('00000000-0000-0000-0000-000000000001','Synthetic recovery fixture');
INSERT INTO company(tenant_id,id,name) VALUES ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','Recovery company');
INSERT INTO branch(tenant_id,company_id,id,name) VALUES ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000003','Recovery branch');
INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name) VALUES ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000004','Recovery warehouse');
INSERT INTO catalog_product(tenant_id,company_id,id,sku,name,base_unit,selling_price_xaf) VALUES ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000005','BOLT-01','Écrou acier','piece',3000);
INSERT INTO stock_balance(tenant_id,company_id,branch_id,warehouse_id,product_id,quantity_milli,value_xaf) VALUES ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000004','00000000-0000-0000-0000-000000000005',5000,12000);
BEGIN;
INSERT INTO journal_entry(tenant_id,company_id,branch_id,id,operation_id,source_type,source_id,business_date) VALUES ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000006','00000000-0000-0000-0000-000000000007','RECOVERY_FIXTURE','00000000-0000-0000-0000-000000000005','2024-01-01');
INSERT INTO journal_line(tenant_id,company_id,entry_id,id,account_code,debit_xaf,credit_xaf) VALUES
 ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000006','00000000-0000-0000-0000-000000000008','STOCK',12000,0),
 ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000006','00000000-0000-0000-0000-000000000009','OPENING_EQUITY',0,12000);
COMMIT;
SQL
"$root/infrastructure/recovery/backup-postgres.sh" "$work/backup" >"$work/backup.log" 2>&1
archive="$work/backup/source.dump"
fingerprints="$work/backup/fingerprints.tsv"
digest=$(sha256sum "$archive"); digest=${digest%% *}
fingerprints_digest=$(sha256sum "$fingerprints"); fingerprints_digest=${fingerprints_digest%% *}
"$tool" "$archive" "$digest" "$fingerprints" "$fingerprints_digest" "$work/evidence" >"$work/drill.log" 2>&1
grep -qx 'PASS: isolated whole-database restore rehearsal' "$work/evidence/result.txt"
grep -qx $'public.tenant\t1' "$work/evidence/table-counts.tsv"
grep -qx $'public.company\t1' "$work/evidence/table-counts.tsv"
grep -qx $'public.branch\t1' "$work/evidence/table-counts.tsv"
grep -qx $'public.stock_balance\t1' "$work/evidence/table-counts.tsv"
grep -qx $'public.journal_line\t2' "$work/evidence/table-counts.tsv"
cmp "$fingerprints" "$work/evidence/restored-fingerprints.tsv"
[[ ! -s "$work/evidence/reconciliation.diff" ]]
[[ $(wc -l <"$work/evidence/network-routes.txt") == 1 ]]
grep -q 'CapEff:.*0000000000000000' "$work/evidence/isolation.txt"
if "$tool" "$archive" "$digest" "$fingerprints" "$fingerprints_digest" "$work/evidence" >"$work/overwrite.log" 2>&1; then echo 'Unexpected evidence overwrite' >&2; exit 1; fi
printf 'damaged archive' >"$work/damaged.dump"
if "$tool" "$work/damaged.dump" "$digest" "$fingerprints" "$fingerprints_digest" "$work/bad-digest" >"$work/digest.log" 2>&1; then echo 'Unexpected checksum acceptance' >&2; exit 1; fi
[[ ! -e "$work/bad-digest" ]]
bad=$(sha256sum "$work/damaged.dump"); bad=${bad%% *}
if "$tool" "$work/damaged.dump" "$bad" "$fingerprints" "$fingerprints_digest" "$work/bad-format" >"$work/format.log" 2>&1; then echo 'Unexpected archive acceptance' >&2; exit 1; fi
grep -qx 'FAIL: restore rehearsal; inspect logs' "$work/bad-format/result.txt"
sed 's/public.tenant\t1\t/public.tenant\t2\t/' "$fingerprints" >"$work/wrong-source.tsv"
wrong=$(sha256sum "$work/wrong-source.tsv"); wrong=${wrong%% *}
if "$tool" "$archive" "$digest" "$work/wrong-source.tsv" "$wrong" "$work/bad-reconciliation" >"$work/reconciliation.log" 2>&1; then echo 'Unexpected source mismatch acceptance' >&2; exit 1; fi
grep -qx 'FAIL: restore rehearsal; inspect logs' "$work/bad-reconciliation/result.txt"
[[ -s "$work/bad-reconciliation/reconciliation.diff" ]]
(cd "$root" && sha256sum -c "$work/migrations.sha256") >"$work/migration-source-check.log"
if [[ -n "$destination" ]]; then
  cp -R "$work/evidence" "$destination"
  cp "$work/backup/backup-manifest.json" "$destination/backup-manifest.json"
  cp "$fingerprints" "$destination/source-fingerprints.tsv"
  cp "$work/schema.log" "$destination/source-schema.log"
  cp "$work/migrations.sha256" "$work/migration-source-check.log" "$destination/"
  cp "$work/bad-reconciliation/reconciliation.diff" "$destination/rejected-mismatch.diff"
  cp "$work/overwrite.log" "$work/digest.log" "$work/format.log" "$destination/"
fi
echo 'PASS: full-schema restore; exact source row fingerprints; stock/journal fixtures; isolated network/capability drop; overwrite/digest/archive/reconciliation rejection'
