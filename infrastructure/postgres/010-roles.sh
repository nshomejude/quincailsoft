#!/bin/sh
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=migrator_password="$QUINCAI_MIGRATOR_PASSWORD" \
  --set=runtime_password="$QUINCAI_RUNTIME_PASSWORD" <<'SQL'
CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
-- V35 backfills need cross-tenant migration visibility. Runtime stays NOBYPASSRLS.
CREATE ROLE quincai_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE BYPASSRLS PASSWORD :'migrator_password';
CREATE ROLE quincai_runtime LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT NOBYPASSRLS PASSWORD :'runtime_password';
GRANT quincai_app TO quincai_runtime;
ALTER DATABASE quincaisoft OWNER TO quincai_migrator;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SQL
