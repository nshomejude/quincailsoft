#!/usr/bin/env bash
# Exact row reconciliation under an already-exported PostgreSQL snapshot.
# Output contains table names, row counts and SHA-256 digests, never row data.
set -euo pipefail
[[ $# == 1 && "$1" =~ ^[0-9A-Fa-f-]+$ ]] || { echo 'Exported snapshot ID required' >&2; exit 1; }
snapshot=$1
query() {
  psql -X -qAt -v ON_ERROR_STOP=1 -c "BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY; SET TRANSACTION SNAPSHOT '$snapshot'; SET LOCAL TIME ZONE 'UTC'; SET LOCAL row_security=off; $1; COMMIT;"
}
unsupported=$(query "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind IN ('S','f','m') AND n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%'")
[[ "$unsupported" == 0 ]] || { echo 'Sequences, foreign tables or materialized views require a separately fenced reconciliation adapter' >&2; exit 1; }
[[ $(query 'SELECT count(*) FROM pg_largeobject_metadata') == 0 ]] || { echo 'Large objects require a separate reconciliation adapter' >&2; exit 1; }
tables=$(query "SELECT n.nspname || E'\\t' || c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind IN ('r','p') AND n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%' ORDER BY n.nspname COLLATE \"C\",c.relname COLLATE \"C\"")
while IFS=$'\t' read -r schema table; do
  [[ -n "$schema" ]] || continue
  [[ "$schema" =~ ^[a-z_][a-z0-9_]*$ && "$table" =~ ^[a-z_][a-z0-9_]*$ ]] || { echo 'Unsupported identifier in reconciliation manifest' >&2; exit 1; }
  count=$(query "SELECT count(*) FROM \"$schema\".\"$table\"")
  [[ "$count" =~ ^[0-9]+$ ]] || { echo 'Invalid row count' >&2; exit 1; }
  # Stream rows, so hashing does not aggregate the entire table into one SQL value.
  # C collation and UTC make the representation independent of host locale/timezone.
  digest=$(query "SELECT to_jsonb(r)::text FROM \"$schema\".\"$table\" r ORDER BY to_jsonb(r)::text COLLATE \"C\"" | sha256sum)
  printf '%s.%s\t%s\t%s\n' "$schema" "$table" "$count" "${digest%% *}"
done <<<"$tables"
