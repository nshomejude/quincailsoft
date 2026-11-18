# Assignment 04 — Inventory replay authority

## Preflight and defect

Read AGENTS.md; ARCHITECTURE §§34.3, 37.4, 38; migration summary, canonical coverage and Python parity inventory; actual `server/inventory_workflows.py` transfer branch and `tests/test_inventory_workflows.py`, plus cached replay boundary assertions in `tests/test_boundary_audit.py`.

Java transfer and count/cut/repack receipt replay checked payload hashes but omitted the actor bound to the original operation. An actor sharing the branch could therefore return another actor's committed command result, including replaying an independent approver's operation. This violated canonical operation provenance even though it did not execute a second stock movement.

## Changes and ownership

Owned DefaultInventoryWorkflows, DefaultInventoryJobs, InventoryCommands and PostgresInventoryWorkflowTest only. Receipt replay now validates company, branch, actor, expected aggregate/source type and ACCEPTED status before decoding a stored result. Existing payload digest/operation semantics are retained, so already committed legitimate retries remain usable. Null cached bodies still fail closed without creating a second operation. No accepted migration changed; reserved V44 is unused.

All mutation API methods retain their existing TenantMutation lifecycle fence. No replay exemption or suspension bypass was introduced. HTTP authorities and authenticated epoch checks remain in existing request infrastructure; this test uses scoped JPA directly and does not certify OIDC or current-permission HTTP enforcement.

## Assertion mapping and requested execution

Root must run `PostgresInventoryWorkflowTest` in the inventory reactor against PostgreSQL 18 with QUINCAI_PG_TEST_URL set. Agent did not run Maven.

- `test_transfer_in_transit_partial_receipt` maps to the existing Java dispatch/partial/excess/final receipt assertions; extended test races identical dispatch operations and asserts one result, unchanged exact quantity/value conservation and no duplicated movements.
- `test_cached_command_replay_rechecks_costs_and_scope` is strengthened for typed inventory authority: original actor replays succeed, different actor draft/approve/dispatch/receive/count-post/reservation retries fail, cross-branch draft retry is concealed, and a changed quantity under the original operation ID fails.
- Test retains closed-period rejection, count snapshot staleness, independent approval, cut material conservation, reservation protection, recall/quarantine disposal and finality assertions.

Status: source and test authored; execution pending root. No behavioral parity is marked ACCEPTED from these edits. Remaining gaps include expired response reconstruction, complete multi-line transfer migration, cross-branch acceptance, full UI fidelity and complete legacy assertion mapping. Current transfer API supports one product per aggregate; that is not claimed equivalent to all legacy multi-line cases.
