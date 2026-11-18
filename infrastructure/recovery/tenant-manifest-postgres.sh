#!/usr/bin/env bash
# Operator-only, read-only tenant reconciliation preflight. Never restores or routes.
set -euo pipefail
umask 077
die(){ printf '%s\n' "$*" >&2; exit 1; }
[[ $# == 3 ]] || die 'Usage: tenant-manifest-postgres.sh EXPORTED_SNAPSHOT TENANT_UUID NEW_EVIDENCE_DIRECTORY'
snapshot=$1 tenant=$2 evidence=$(realpath -m -- "$3")
[[ "$snapshot" =~ ^[0-9A-Fa-f-]+$ ]] || die 'Exported snapshot ID required'
[[ "$tenant" =~ ^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$ ]] || die 'Canonical tenant UUID required'
[[ ! -e "$evidence" ]] || die 'Evidence directory exists; refusing overwrite'
[[ $(psql --version) =~ PostgreSQL\)\ 18\. ]] || die 'PostgreSQL18 required'
mkdir -- "$evidence"
trap 'code=$?; if [[ $code != 0 && ! -e "$evidence/result.txt" ]]; then echo "FAIL: incomplete tenant manifest" >"$evidence/result.txt"; fi' EXIT
query(){ psql -X -qAt -F $'\t' -v ON_ERROR_STOP=1 -c "BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY; SET TRANSACTION SNAPSHOT '$snapshot'; SET LOCAL TIME ZONE 'UTC'; SET LOCAL row_security=off; $1; COMMIT;"; }
[[ $(query 'SHOW server_version_num') =~ ^18[0-9]{4}$ ]] || die 'PostgreSQL18 server required'
# Public is the explicit application schema allowlist. Partition leaves are counted
# through their parent, never twice. External data and unclassified schemas fail closed.
[[ $(query "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%' AND ((c.relkind IN ('r','p','S','f','m') AND n.nspname<>'public') OR c.relkind IN ('S','f','m'))") == 0 ]] || die 'Unsupported schema, sequence, foreign table or materialized view'
[[ $(query 'SELECT count(*) FROM pg_largeobject_metadata') == 0 ]] || die 'Large objects need separate recovery'
[[ $(query "SELECT count(*) FROM public.tenant WHERE id='$tenant'::uuid") == 1 ]] || die 'Target tenant not found'
# Metadata inventory explicitly lists unscoped tables; they are not silently exported.
query "SELECT c.relname,CASE WHEN c.relname='tenant' THEN 'TENANT_ROOT' WHEN EXISTS(SELECT FROM pg_attribute a WHERE a.attrelid=c.oid AND a.attname='tenant_id' AND NOT a.attisdropped AND a.atttypid='uuid'::regtype) THEN 'TENANT_SCOPED' ELSE 'UNSCOPED_NOT_EXPORTED' END,c.relrowsecurity,c.relforcerowsecurity FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind IN ('r','p') AND NOT c.relispartition ORDER BY c.relname COLLATE \"C\"" >"$evidence/table-classification.tsv"
: >"$evidence/tenant-fingerprints.tsv"
while IFS=$'\t' read -r table classification rls forced; do
 [[ "$table" =~ ^[a-z_][a-z0-9_]*$ ]] || die 'Unsupported table identifier'
 [[ "$classification" != UNSCOPED_NOT_EXPORTED ]] || continue
 column=tenant_id; [[ "$classification" != TENANT_ROOT ]] || column=id
 count=$(query "SELECT count(*) FROM public.\"$table\" WHERE $column='$tenant'::uuid")
 digest=$(query "SELECT to_jsonb(r)::text FROM public.\"$table\" r WHERE $column='$tenant'::uuid ORDER BY to_jsonb(r)::text COLLATE \"C\"" | sha256sum)
 printf 'public.%s\t%s\t%s\n' "$table" "$count" "${digest%% *}" >>"$evidence/tenant-fingerprints.tsv"
done <"$evidence/table-classification.tsv"
# Generate only read-only counts using quoted catalog identifiers. Both FK directions
# matter: unscoped rows referencing a selected tenant parent are dependencies too.
query "WITH tables AS (
 SELECT c.oid,c.relname,CASE WHEN c.relname='tenant' THEN 'id' WHEN EXISTS(SELECT FROM pg_attribute a WHERE a.attrelid=c.oid AND a.attname='tenant_id' AND NOT a.attisdropped AND a.atttypid='uuid'::regtype) THEN 'tenant_id' END scope
 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind IN ('r','p')),
 edges AS(SELECT f.oid,f.conname,c.relname child,p.relname parent,c.scope child_scope,p.scope parent_scope,f.convalidated,
 (SELECT string_agg(format('c.%I=p.%I',ca.attname,pa.attname),' AND ' ORDER BY k.n) FROM unnest(f.conkey,f.confkey) WITH ORDINALITY k(ca,pa,n) JOIN pg_attribute ca ON ca.attrelid=f.conrelid AND ca.attnum=k.ca JOIN pg_attribute pa ON pa.attrelid=f.confrelid AND pa.attnum=k.pa) predicate
 FROM pg_constraint f JOIN tables c ON c.oid=f.conrelid JOIN tables p ON p.oid=f.confrelid WHERE f.contype='f' AND f.conparentid=0)
 SELECT format('SELECT %L, count(*) FROM public.%I c JOIN public.%I p ON %s WHERE %s',
 CASE WHEN child_scope IS NULL THEN 'UNSCOPED_CHILD:' WHEN parent_scope IS NULL THEN 'UNSCOPED_PARENT:' ELSE 'CROSS_TENANT:' END||conname,
 child,parent,predicate,
 CASE WHEN child_scope IS NULL THEN format('p.%I=%L::uuid',parent_scope,'$tenant') WHEN parent_scope IS NULL THEN format('c.%I=%L::uuid',child_scope,'$tenant') ELSE format('((c.%I=%L::uuid AND p.%I IS DISTINCT FROM %L::uuid) OR (p.%I=%L::uuid AND c.%I IS DISTINCT FROM %L::uuid))',child_scope,'$tenant',parent_scope,'$tenant',parent_scope,'$tenant',child_scope,'$tenant') END)
 FROM edges WHERE child_scope IS NOT NULL OR parent_scope IS NOT NULL ORDER BY conname COLLATE \"C\"" >"$evidence/dependency-checks.sql"
: >"$evidence/dependency-counts.tsv"
while IFS= read -r statement; do [[ -z "$statement" ]] || query "$statement" >>"$evidence/dependency-counts.tsv"; done <"$evidence/dependency-checks.sql"
awk -F'\t' '$2!=0' "$evidence/dependency-counts.tsv" >"$evidence/blockers.tsv"
# Missing validated constraints are never treated as established dependency closure.
query "SELECT 'UNVALIDATED_FK:'||f.conname,1 FROM pg_constraint f JOIN pg_class c ON c.oid=f.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE f.contype='f' AND NOT f.convalidated AND n.nspname='public'" >>"$evidence/blockers.tsv"
printf 'tenant_id=%s\nsnapshot=%s\nmode=READ_ONLY_PREFLIGHT\nproduction_cutover_allowed=false\nexternal_objects_and_keys=NOT_VERIFIED\n' "$tenant" "$snapshot" >"$evidence/scope.txt"
sha256sum "$evidence/tenant-fingerprints.tsv" >"$evidence/tenant-fingerprints.sha256"
if [[ -s "$evidence/blockers.tsv" ]]; then echo 'BLOCKED: tenant database dependencies require review' >"$evidence/result.txt"; exit 2; fi
echo 'PASS: tenant database fingerprint and declared FK preflight only' >"$evidence/result.txt"
