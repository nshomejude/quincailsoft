# Assignment 08 — sales fulfillment remaining quantities and replay

## Preflight / scope

Read AGENTS.md, migration summary, canonical coverage and Python parity registers; inspected actual `server/inventory_workflows.py` custody/fulfillment handlers, `tests/test_inventory_workflows.py` custody partial/excess assertions, `tests/test_finance_workflows.py` sales recognition assertions, and `tests/test_boundary_audit.py` cached replay scope assertions. Relevant authority: ARCHITECTURE §8.9 partial warehouse fulfillment; §13.3 partial shipping/pickup; §32 recognition rules; §§33–34 operation identity/replay security. This is a bounded correctness increment, not complete sales migration acceptance.

Owned changes:
- `backend/business-modules/sales/src/main/java/com/quincaisoft/sales/application/DefaultFulfillmentService.java`
- `backend/business-modules/sales/src/test/java/com/quincaisoft/sales/PostgresSaleIntegrationTest.java`
- This assignment evidence.

No schema change; V48 reservation unused. No contract shape, shared configuration, stock/finance implementation or lifecycle exemption changed. Shared module APIs remain the only stock/finance access.

## Fixed behavior

The original pick list returned the full sold quantity after partial dispatch. It now subtracts immutable dispatch facts using exact checked arithmetic, rejects inconsistent historical quantities, excludes zero remaining lines and returns no picks for dispatched/completed tasks. A read lock holds the task state stable while dispatch facts are read. Dispatch uses the same remaining-quantity calculation; it does not change stock or revenue recognition.

Fulfillment receipt lookup now also requires `FULFILLMENT` source and `ACCEPTED` status, in addition to existing request/actor/company/branch checks. An unrelated or rejected receipt with a matching hash cannot be decoded as successful fulfillment replay.

## Assertions and execution

Root must execute `PostgresSaleIntegrationTest.stockJournalsOutboxAndReplayCommitOrRollbackTogether` using Java25/PostgreSQL18:

`-pl business-modules/sales -am -Dtest=PostgresSaleIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`

Added assertions cover:
- Original 2000-unit-milli pick list → 1500 after first dispatch → 1000 after second → empty after final dispatch/completion.
- Excess dispatch leaves exactly one dispatch fact and 1500 remaining, preserving transaction rollback.
- Exact retries preserve the result; a different actor or branch cannot replay it.
- Matching hashes on wrong-source/rejected receipts fail and create no preparation task.
- Existing financial/stock counts remain unchanged by fulfillment; scope concealment and custody proof remain covered by the same real PostgreSQL test.

Execution status at handoff: NOT_RUN by worker; root owns serialized Maven execution. Source-only evidence does not mark any legacy requirement accepted.

## Legacy mapping / limits

`test_custody_partial_and_excess` establishes bounded partial quantities and overcollection denial; the Java test preserves these existing custody assertions and extends outstanding quantity semantics to post-sale fulfillment. `test_cached_command_replay_rechecks_costs_and_scope` requires replay scope revalidation; the added fulfillment assertions cover scope and actor ownership but do not claim all legacy cached record redaction is migrated. `test_sale_posting_and_unsupported_recognition` remains covered by existing sale recognition/custody policy tests; no new full parity claim is made here.

Unfinished sales scope includes tax/non-stock/serial/expiry sale lines, richer pricing/discount approvals, original UI/action/report fidelity, and broader sale/return/tender/provider acceptance. The old whole-task dispatch path still represents all remaining goods without per-line dispatch evidence; this patch does not retroactively fabricate line history for it. Full parity/release gates remain unsatisfied.
