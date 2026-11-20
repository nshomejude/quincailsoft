# Assignment 13: catalog replay scope and receipt meaning

## Preflight and gap

Read repository AGENTS.md, architecture security/scope and exact-quantity requirements (§§31–46, especially §44.3), the historical migration summary, canonical coverage and Python parity inventories. The existing summary is historical, not certification of these edits. Legacy `server/server.py` stores command retries by user and operation, and checks role/branch scope; `server/policy.py` scopes catalog reads/writes. `tests/test_security.py` asserts stable command retry and permission filtering. The typed Java implementation must preserve those distinctions while adding tenant/company identity.

Category and unit request hashes already included the actor, but their receipt lookup omitted explicit company/branch verification. PostgreSQL RLS already conceals those other-scope receipts: identical retries safely roll back on typed uniqueness/global operation constraints rather than returning another scope's data. This increment adds defense-in-depth scope/actor checks and closes successful-source/status verification gaps in visible receipts; it does not claim that an existing cross-scope disclosure was reproduced.

## Changed behavior

`DefaultCatalogMasterService` now requires exact company, branch, actor, source type and ACCEPTED status as well as the original request hash and retained response. Each of its six typed master commands passes its real source type; no generic business store is introduced. `DefaultCatalogService` checks the same identity against the operation lookup (subject to existing receipt RLS), preserving rejection of attempts to repurpose a committed operation. Original correctly scoped hashes/responses remain compatible.

No API shape, database migration, unit-scale rule or SKU storage change is made. Existing immutable base-unit and unit-factor rules remain active. Controller authorization/current-scope enforcement is still required; this increment does not claim full identity or catalog UI acceptance.

## Acceptance request

Root runs `CatalogReplayPostgresTest` against PostgreSQL18 with a non-superuser, non-bypass runtime role and current migrations. It asserts exact original retry, same-scope actor denial plus exact SQLSTATE23505/constraint and rollback checks for RLS-concealed company/branch retries, empty other-company reads, rejected/wrong-source receipt denial, one surviving typed row, integer XAF above JavaScript safe range and immutable product/unit scales. Root must also run existing `MasterDataPostgresTest` to cover manufacturer/family/variant/offer retries.

Execution status: PENDING_ROOT. No Maven or runtime acceptance was executed by this agent. Root must add the catalog PostgreSQL test dependency. No V53 is needed. Full catalog screen/workflow parity, unit-conversion breadth and active authorization live acceptance remain unclaimed.
