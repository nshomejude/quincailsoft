# PostgreSQL authority foundation

Implements the first migration slice of architecture §§3,5,34,36–38. It is not the complete 45-module schema.

- PostgreSQL 18 typed company/branch/catalog/customer, merchant-serviceable stock, sale, journal, canonical operation identity, outbox/inbox, lease and change-publication tables.
- XAF stored as BIGINT whole FCFA; quantities BIGINT milli-base-units. No float ledger amounts.
- Composite references enforce tenant/company/branch relationships. All tenant tables ENABLE and FORCE RLS. Missing transaction context exposes no data.
- Runtime role is non-owner/NOSUPERUSER/NOBYPASSRLS and cannot create roles, grant memberships, truncate tables, update or delete ledger facts.
- `tenant_memberships` exposes only current tenant/actor memberships. The application must check active membership, exact company/branch and authorization epoch inside every transaction before business access. Identity scope settings must derive from validated OIDC claims, never request headers.
- Journal balance is checked by deferred database constraints. Immutable posted headers and lines cannot be modified; a committed batch cannot receive extra balanced pairs later. Sale lines can change only while parent is DRAFT/HELD.
- Canonical operation key is `(tenant_id,operation_id)` independent of device/user. A source result and digest cannot be changed after acceptance. Response expiration never erases operation identity.
- Outbox facts are immutable; lease/delivery state is separate. Schema does not imply a working worker, lease renewal, change-feed publisher or recovery protocol.

## Runtime roles and migrations

`infrastructure/compose.yaml` creates PostgreSQL 18.7 with separate migration/runtime roles using environment-supplied secrets. Flyway must connect as `quincai_migrator`; application datasource must use `quincai_runtime` (member of `quincai_app`). Never run the API with the bootstrap postgres account. Tenant provisioning is a separate privileged control-plane task; no public owner account or merchant is seeded.

## Executed PostgreSQL tests

`tests/MigrationAcceptance.java` executes all SQL files on an isolated database on a real PostgreSQL18 server, with a non-owner runtime login. Nineteen assertions passed against PostgreSQL18.7; see `tests/postgres18-acceptance.txt`. This tests SQL migration execution, not Flyway upgrade history or full application reconciliation.

Example (admin password should be supplied through JDBC connection properties in secured runners; the executed disposable cluster used loopback trust authentication):

```sh
java -cp "$POSTGRES_JDBC_JAR" tests/MigrationAcceptance.java \
  jdbc:postgresql://127.0.0.1:55432/postgres agent src/main/resources/db/migration
```

The runner creates and drops an isolated temporary database/login. It does not drop an existing application database. It leaves the NOLOGIN base runtime role if newly created. Requires PostgreSQL admin CREATE DATABASE/ROLE solely for isolated test setup.

## Remaining work

Full custody/ownership/lot dimensions, reservations and offline grants, fiscal snapshots, document numbering/issuance, taxes, purchasing/AP, cash shifts, payments/returns, immutable audit record schema, role-to-command permissions, control balance reconciliation, enforced period overlap policy and worker/change-feed execution are not implemented by these migrations. `stock_balance` deliberately permits only MERCHANT/SERVICEABLE rows. The baseline supports exactly one authorized company/branch context per transaction; consolidated reads need a separately authorized projection. Formal security review, production backup/PITR and performance evidence are outstanding.

V3 grants UPDATE only on non-economic `lock_version` metadata for customer, financial period and warehouse. PostgreSQL uses this narrow privilege to permit pessimistic row locking. Tests prove locks succeed while eligibility/status mutations remain denied.
