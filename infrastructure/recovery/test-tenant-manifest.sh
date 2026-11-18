#!/usr/bin/env bash
# Disposable PostgreSQL18 tests. Uses PG* connection to an approved TEST server.
set -euo pipefail
umask 077
[[ $# == 1 && ! -e "$1" ]] || { echo 'Usage: test-tenant-manifest.sh NEW_EVIDENCE_DIRECTORY' >&2; exit 1; }
evidence=$(realpath -m -- "$1");mkdir "$evidence"
root=$(cd "$(dirname "$0")" && pwd)
db=tenant_manifest_$(date +%s)_$$
admin_db=${PGDATABASE:-postgres}
owner=${PGUSER:-$(id -un)}
role=manifest_reader_$$
psql -X -v ON_ERROR_STOP=1 -d "$admin_db" -c "CREATE DATABASE $db" >"$evidence/setup.log" 2>&1
snapshot_pid=''
cleanup(){ code=$?; if [[ -n "$snapshot_pid" ]];then kill "$snapshot_pid" 2>/dev/null || true;wait "$snapshot_pid" 2>/dev/null || true;fi; PGUSER="$owner" psql -X -d "$admin_db" -c "DROP DATABASE $db WITH (FORCE)" >>"$evidence/setup.log" 2>&1 || code=1; PGUSER="$owner" psql -X -d "$admin_db" -c "DROP ROLE IF EXISTS $role" >>"$evidence/setup.log" 2>&1 || code=1;exit "$code"; }
trap cleanup EXIT
export PGDATABASE=$db
a=00000000-0000-0000-0000-000000000001
b=00000000-0000-0000-0000-000000000002
psql -X -v ON_ERROR_STOP=1 <<SQL >>"$evidence/setup.log" 2>&1
CREATE TABLE tenant(id uuid PRIMARY KEY,name text NOT NULL);
CREATE TABLE company(tenant_id uuid NOT NULL REFERENCES tenant(id),id uuid PRIMARY KEY,name text NOT NULL);
CREATE TABLE child(tenant_id uuid NOT NULL REFERENCES tenant(id),id uuid PRIMARY KEY,company_id uuid REFERENCES company(id));
INSERT INTO tenant VALUES('$a','Target'),('$b','Unrelated');
INSERT INTO company VALUES('$a','$a','Target company'),('$b','$b','SECRET unrelated company');
INSERT INTO child VALUES('$a','$a','$a'),('$b','$b','$b');
ALTER TABLE company ENABLE ROW LEVEL SECURITY;
ALTER TABLE company FORCE ROW LEVEL SECURITY;
CREATE ROLE $role LOGIN NOSUPERUSER NOBYPASSRLS;
GRANT USAGE ON SCHEMA public TO $role;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO $role;
SQL
start_snapshot(){
 coproc SNAPSHOT { psql -X -qAt -v ON_ERROR_STOP=1; }
 snapshot_pid=$SNAPSHOT_PID;snapshot_in=${SNAPSHOT[1]};snapshot_out=${SNAPSHOT[0]}
 printf 'BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY; SELECT pg_export_snapshot();\n' >&"$snapshot_in"
 IFS= read -r -t 30 snapshot <&"$snapshot_out"
}
end_snapshot(){ printf 'COMMIT;\n\\q\n' >&"$snapshot_in";wait "$snapshot_pid";snapshot_pid=''; }
start_snapshot
"$root/tenant-manifest-postgres.sh" "$snapshot" "$a" "$evidence/initial"
# New unrelated values cannot affect the already-exported snapshot or target manifest.
psql -X -v ON_ERROR_STOP=1 -c "UPDATE company SET name='CHANGED unrelated secret' WHERE tenant_id='$b'" >>"$evidence/setup.log" 2>&1
"$root/tenant-manifest-postgres.sh" "$snapshot" "$a" "$evidence/same-snapshot"
cmp "$evidence/initial/tenant-fingerprints.tsv" "$evidence/same-snapshot/tenant-fingerprints.tsv"
if PGUSER="$role" "$root/tenant-manifest-postgres.sh" "$snapshot" "$a" "$evidence/rls-denied" >"$evidence/rls-denied.log" 2>&1;then echo 'RLS-filtered export accepted' >&2;exit 1;fi
grep -qi 'row-level security' "$evidence/rls-denied.log"
if "$root/tenant-manifest-postgres.sh" "$snapshot" "$a" "$evidence/initial" >"$evidence/overwrite.log" 2>&1;then exit 1;fi
end_snapshot
start_snapshot
"$root/tenant-manifest-postgres.sh" "$snapshot" "$a" "$evidence/new-snapshot"
cmp "$evidence/initial/tenant-fingerprints.tsv" "$evidence/new-snapshot/tenant-fingerprints.tsv"
end_snapshot
# A weak legacy FK permits cross-tenant data; preflight must detect it rather than
# assume that a syntactically valid constraint establishes tenant ownership.
psql -X -v ON_ERROR_STOP=1 -c "UPDATE child SET company_id='$b' WHERE tenant_id='$a'" >>"$evidence/setup.log" 2>&1
start_snapshot
if "$root/tenant-manifest-postgres.sh" "$snapshot" "$a" "$evidence/cross-tenant";then exit 1;else [[ $? == 2 ]];fi
grep -q $'CROSS_TENANT:child_company_id_fkey\t1' "$evidence/cross-tenant/blockers.tsv"
end_snapshot
psql -X -v ON_ERROR_STOP=1 <<SQL >>"$evidence/setup.log" 2>&1
UPDATE child SET company_id='$a' WHERE tenant_id='$a';
CREATE TABLE external_dependency(id uuid PRIMARY KEY,company_id uuid REFERENCES company(id));
INSERT INTO external_dependency VALUES('$a','$a');
SQL
start_snapshot
if "$root/tenant-manifest-postgres.sh" "$snapshot" "$a" "$evidence/unscoped";then exit 1;else [[ $? == 2 ]];fi
grep -q $'UNSCOPED_CHILD:external_dependency_company_id_fkey\t1' "$evidence/unscoped/blockers.tsv"
end_snapshot
psql -X -v ON_ERROR_STOP=1 -c 'CREATE SCHEMA unreviewed; CREATE TABLE unreviewed.data(id uuid)' >>"$evidence/setup.log" 2>&1
start_snapshot
if "$root/tenant-manifest-postgres.sh" "$snapshot" "$a" "$evidence/unknown-schema" >"$evidence/unknown-schema.log" 2>&1;then exit 1;fi
grep -q 'Unsupported schema' "$evidence/unknown-schema.log"
end_snapshot
! grep -R -E 'SECRET unrelated|CHANGED unrelated' "$evidence/initial" "$evidence/new-snapshot"
printf '%s\n' 'PASS: target-only fingerprints, snapshot consistency, unrelated-change invariance, RLS denial, evidence-overwrite denial, cross-tenant FK blocker, inbound unscoped dependency blocker, unknown-schema rejection' >"$evidence/result.txt"
cat "$evidence/result.txt"
