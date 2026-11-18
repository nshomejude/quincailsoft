# Assignment 03 — finance replay actor provenance

## Preflight and bounded scope

Read AGENTS.md, ARCHITECTURE §§37.3–37.4 and the migration summary/canonical and Python parity registers. The summary is historical evidence, not certification of this change. Read `server/finance_workflows.py` journal/period dispatch and `tests/test_finance_workflows.py` in full. Finance owns manual journals and financial periods; tender, purchasing, cash and return subledgers remain with their owning modules.

The concrete gap was that manual-journal and period command replay validated request hash/company/branch but omitted actor identity. A different actor with an otherwise identical operation could obtain a committed response and skip the original independent-approval check. The fix validates the immutable corresponding outbox event actor, joined to the receipt source ID, operation, scope and event type. This preserves valid existing manual-journal retries because their events already contain `actorId`.

Period events now add `actorId` without changing existing result fields. Historical period requests/decisions can use their recorded requestedBy/decidedBy provenance. Historical create events without actor provenance deliberately fail closed: inspect the original period rather than issuing a fresh creation. No actor is guessed and no accepted ledger fact is rewritten.

## Changed ownership

- finance infrastructure `FinanceReplayEvidence.java` and `FinanceOperations.java`
- finance application `DefaultPeriodService.java`
- finance test `FinanceReplayPostgresTest.java`
- this assignment evidence directory

No schema migration, API shape, shared configuration, acceptance register or foreign-module ledger is changed. V43 is not needed/released. Root owns Maven, generated inventory, integration evidence and publishing.

## Assertion mapping and execution

Primary legacy mapping: `test:tests/test_finance_workflows.py:test_journal_balances_reversal_and_closed_period`, and the `journal:post`/`journal:reverse` commands plus `server/finance_workflows.py` journal branches 210–222. Java intentionally uses independent submit/approve before posting instead of the legacy single-actor post. Existing balanced-entry unit tests reject one-sided/unbalanced input. New PostgreSQL test drives draft → submit → independent approve, exact integer posting above JavaScript's safe-number range, reversal of both immutable lines, balanced account totals, duplicate reversal rejection, closed-period failure with no receipt or draft residue, and immutable ledger updates.

Additional new regression cases: concurrent same-operation journal retry creates one source; request-content conflict; maker/reviewer actor crossing on draft and approval; cross-branch and cross-tenant concealment; period creation/request/rejection exact retries; cross-actor denials; verifiable historical period request retry; rejection of actorless historical create receipt. It uses a real PostgreSQL database and non-superuser/NOBYPASSRLS runtime role, applies current migrations and validates Hibernate mappings.

Execution status at handoff: **NOT_RUN by this agent** per shared-workspace rule. Root requested to execute `-pl business-modules/finance -am -Dtest=FinanceReplayPostgresTest,FinanceRulesTest -Dsurefire.failIfNoSpecifiedTests=false test` with PostgreSQL 18 environment. No test pass or parity acceptance is claimed here.

## Remaining gaps

This does not certify the entire finance module, all subledger control reconciliation, fiscal exports, live OIDC permissions, or full period-close guards. The test deliberately does not claim successful close: its fixture lacks cash/provider/expense/returns guard implementations. Root must review executed evidence before updating exact requirement acceptance. Overall migration parity remains open.
