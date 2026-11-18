# Assignment 01: tenant recovery manifest preflight

## Scope and preflight

Architecture §§41.2(3), 41.3 Q036 and 44.3 govern this increment. Read AGENTS.md, migration summary (historical 60-test run), canonical coverage and current Python parity inventory. Actual legacy `server/operations.py::_validate_backup` enforces tenant metadata and organization/relation closure; its generic business-record restore is not an acceptable replacement for the prescribed PostgreSQL tenant recovery. Legacy tests in `tests/test_operations.py` cover backup verification, tamper/stale review and provider/ledger restore denial. No legacy requirement is marked accepted by these new infrastructure tests.

Owned files: `infrastructure/recovery/tenant-manifest-postgres.sh`, `infrastructure/recovery/test-tenant-manifest.sh`, the recovery README and this evidence directory. No Java, database migration, shared contract or parity registry edits. No database writes in the production operator tool. The acceptance script creates and drops only its own disposable database and test role on an explicitly supplied test server.

## Implemented behavior

The operator tool consumes a live exported PostgreSQL18 snapshot, canonical tenant UUID and fresh evidence directory. It creates deterministic per-table count/SHA-256 fingerprints for the target tenant only, including the tenant root, under READ ONLY/REPEATABLE READ. It streams canonical rows in C collation/UTC to the hash process; row contents are never written to evidence. Unrelated tenants' rows never contribute to the target fingerprint.

Public is the explicit schema allowlist. Foreign tables, sequences, materialized views, database large objects and tables in additional schemas are rejected. Runtime RLS-filtered reads fail with row_security=off rather than silently certifying a partial tenant. Table classification includes RLS/forced-RLS metadata and explicitly identifies every unscoped table not exported. Partition roots include their leaves once.

Both directions of declared FKs are inspected. Cross-tenant parent references, target references to unscoped parents and unscoped children referencing target rows create counted blockers. Unvalidated FKs are blockers. The tool returns exit 2 and BLOCKED for these dependencies, keeps its count/hash evidence for review, and never calls these incomplete closures successful. Query/permission failures instead record FAIL. Existing evidence cannot be overwritten.

## Acceptance

`test-tenant-manifest.sh` uses real PostgreSQL18: two tenants, forced RLS, a weak FK permitting a deliberate cross-tenant reference, and an unscoped child dependency. It checks snapshot consistency, target fingerprint invariance after unrelated changes, RLS denial, overwrite denial, counted cross-tenant/inbound closure blockers and schema rejection. Root executes this script serially; no runtime PASS is claimed before results arrive.

## Exact remaining gaps

This is a database fingerprint and declared-FK preflight, not complete tenant extraction. Unscoped control-plane/identity dependencies require reviewed extraction adapters. JSON/polymorphic references, external documents, signing/IdP keys, ownership authorization, legal-hold approvals, live writer fencing, routing cutover, post-backup fact replay and staged resume are not verified by this tool. Source and candidate schemas must be frozen while fingerprinting; exported MVCC snapshots are not a DDL coordination mechanism. No production data is changed and `production_cutover_allowed=false` is always recorded. Recovery API remains receipt-review-only.
