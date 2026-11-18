# Assignment 10 — tender and bank replay authority

## Preflight and ownership

Read repository AGENTS.md, ARCHITECTURE §38 (aggregate ownership/reliable execution), §44.3 (actor-bound privileged actions and scope), the migration summary, canonical coverage, and Python parity inventory. Read actual `server/finance_workflows.py` handling and `tests/test_finance_workflows.py` payment, deposit, cash-close, bank-match, role and rollback assertions.

Owned changes are only `DefaultTenderService`, `TenderOperations`, `TenderPostgresTest`, `SettlementPostgresTest` and this evidence. No database migration, API signature, shared authentication, provider adapter or financial posting rule was changed. V50 is unused.

## Implemented correction

The cash/tender and bank/settlement receipt helpers formerly checked request hash and company/branch but ignored the receipt's recorded actor, source type and outcome. Another actor who knew the operation and request could retrieve its successful response, including an independently approved bank decision, before the normal command body checked authority.

Both helpers now require the persisted actor to equal the current actor, source type to match the command family, and status to be ACCEPTED before returning a receipt. The request digest algorithm and scope validation remain unchanged, preserving valid original-actor retries. Missing actor provenance fails closed. Expired response content still fails closed without repeating money movement.

## Assertions and execution

Root requested to run `-pl business-modules/tenders -am -Dtest=TenderPostgresTest,SettlementPostgresTest,TenderRulesTest -Dsurefire.failIfNoSpecifiedTests=false test` with PostgreSQL 18. Agent did not run Maven. Source diff whitespace check passed; runtime status remains pending root.

`TenderPostgresTest.cashDepositsAllocationsRefundsConserveAndReplay` now verifies original-actor cash-opening/receipt replay, different-actor rejection, cross-branch replay rejection, and unchanged journal/tender fact counts. Existing tests continue deposit allocation/refund conservation, over-refund denial, stale close rejection, closed-till refusal, financial balance and posting rollback.

`SettlementPostgresTest.supplierBankAndManualJournalsConserveAndReplay` now verifies cross-actor supplier-payment retry rejection with no new journal or tender facts, original-approver bank approval replay and denial to the requester, and actor-bound bank-match replay with one bank_match record. Existing assertions retain exact gross/net/fee reconciliation, independent approval, overpayment denial, bank evidence single consumption under concurrency and closed-period rollback.

Legacy mapping is deliberately bounded: `test_payment_overallocation_and_branch_escape`, `test_deposit_receive_allocate_refund_and_double_spend`, `test_finance_role_and_approval_guard`, `test_bank_match_exact_fee_and_unique_payment`, and `test_rollback_preserves_cash_on_posting_failure` provide the relevant economic/access requirements. New actor-bound replay is an architecture-strengthened safeguard; those entire legacy tests are not automatically accepted by this patch.

## Remaining gaps

No live bank/provider settlement acceptance, privileged step-up, full UI/workflow fidelity, or full finance parity certification is claimed. Existing controller/global authorization and tenant lifecycle fences remain required before service invocation. Historical expired receipt reconstruction remains unimplemented and fails safely. No unknown provider outcome was converted into success.

## Integration correction

Root's first full integration run exposed an overly specific test expectation for cross-branch replay: forced RLS already conceals the receipt and source sale, yielding `NoSuchElementException("Sale not found")`, not the same-branch actor-conflict exception. The regression now requires that exact concealed-source denial and retains unchanged journal/fact count assertions. Production scope rules were not changed. The same-branch cross-actor vulnerability remains the actual corrected behavior. Final root rerun remains pending.
