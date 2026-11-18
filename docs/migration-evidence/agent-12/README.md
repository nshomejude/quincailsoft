# Assignment 12 — customer master replay isolation

## Preflight and ownership

Read AGENTS.md, architecture §17 security, §34.2 scope revocation and §34.3 canonical operation identity; inspected migration summary, canonical coverage and Python parity inventories. Read actual `tests/test_security.py` scope and idempotency assertions and `server/policy.py` scope guards. This patch owns only DefaultCustomerDirectory and MasterDataPostgresTest in customer-accounts. No SQL or API changes; V52 remains unused. Finance keeps ownership of customer balances; stock keeps custody ownership.

## Concrete gap and implementation

The old customer directory validated the actor/command hash but did not validate the stored receipt source type, status or provenance fields. Replay now also requires exact company, branch, actor, CUSTOMER source type and ACCEPTED status. Existing forced RLS already hides receipts from another company/branch; a cross-scope duplicate reaches the tenant-wide operation unique key and rolls back. This patch does not claim a pre-existing cross-scope disclosure was demonstrated. It fails closed when receipt metadata does not prove a successful matching command. It does not silently create a second customer for a conflicting operation.

## Acceptance mapping

The scoped denial principle in `tests/test_security.py:test_branch_scope`, enforced by legacy `server/policy.py:scoped`, and stable response semantics in `test_version_and_idempotency` motivate the explicit new customer-master cases. These related legacy tests also contain sale workflow assertions, so this patch does not mark those entire requirements accepted.

`MasterDataPostgresTest.mastersPreserveScopeReplayMetadataAndVersionedUnitIdentity` now uses actual second company/branch records and the same actor plus original operation bytes to require exact SQLSTATE 23505 on idempotency_result_pkey; checks customer, receipt and outbox counts before/after each denied retry to establish full rollback; verifies no second-company customer was created, original data remain readable only in company scope, and the original scope still receives the exact durable original response. It also seeds matching-hash REJECTED and different-source receipts and requires denial without customer creation. Existing concurrent retry, changed payload conflict, actor crossing, exact XAF above JavaScript safe integer, stale-version rollback, tenant isolation and SQL write guards remain covered.

The initial root run exposed an incorrect expected exception for RLS-hidden cross-scope receipts. Assertions now require the exact unique-key SQLSTATE/constraint and unchanged table counts; root rerun is pending. Tests are not executed by this worker. Root runs PostgreSQL18 acceptance using `-pl business-modules/customer-accounts -am -Dtest=MasterDataPostgresTest -Dsurefire.failIfNoSpecifiedTests=false test`. `git diff --check` passes for owned files.

## Remaining gaps

This is bounded customer master receipt isolation, not full customer-account migration. Cross-branch aggregate credit enforcement, deposits/allocations/refunds, source-bound acceptance for customer portals, custody workflows, expired response reconstruction, action-bound step-up and live/UI acceptance require their owning modules and further tests. Existing HTTP permission and common membership/epoch enforcement are preserved; this patch does not certify every internal caller's authorization. No production acceptance or migration-completion percentage is claimed.
