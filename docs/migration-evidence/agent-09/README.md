# Assignment 09 — supplier invoice duplicate identity

## Preflight

Read AGENTS.md, ARCHITECTURE §37.1 Procurement state and control, §37.3 Balanced operational journal model, migration summary, canonical coverage and Python parity registers. Legacy `server/finance_workflows.py` supplier_invoice handler checks identical supplier/reference pairs, receipt relationship and receipt total, then posts GRNI/AP. `tests/test_finance_workflows.py::test_supplier_invoice_and_supplier_payment` confirms posted invoice/payable settlement. This increment preserves duplicate prevention and strengthens it to the specification's normalized invoice identity; it does not certify the complete payment workflow.

## Change

Owned files: `DefaultPurchasingService.java`, `PostgresPurchasingIntegrationTest.java`, forward migration `V49__normalized_supplier_invoice_identity.sql`, and this evidence. No old migration or shared file changed.

The old unique constraint compared raw supplier reference text; case and surrounding whitespace variants could create another payable against a different receipt. V49 adds a unique normalized company/supplier identity across branches. ASCII case and leading/trailing POSIX whitespace are normalized; punctuation, interior whitespace and non-ASCII spelling remain significant. The original supplied text is now retained in the immutable invoice, API response and receipt response rather than trimmed before storage. Existing case/whitespace collisions make the migration fail for reviewed reconciliation; it does not rewrite or delete posted facts.

This table contains supplier invoices only, so document type is implicit. Future credit/debit-note identities require their separately specified workflows. The old exact identity constraint remains as defense in depth. Database uniqueness serializes competing insertions without an application-side duplicate check leaking another branch's invoice details.

## Acceptance request

Root alone executes Maven against PostgreSQL 18:

`-pl business-modules/purchasing -am -Dtest=PostgresPurchasingIntegrationTest,PurchaseRulesTest -Dsurefire.failIfNoSpecifiedTests=false test`

New assertions: original reference is retained; case and tab/newline variants against a different receipt fail specifically with SQLSTATE 23505; invoice/payable/journal counts stay unchanged; failed operation has neither receipt nor outbox event; original command still replays exact original reference. Existing test covers independent PO approval, duplicate invoice on same receipt, overreceipt denial, closed-period rollback, supplier-return limits, scoped denial and balanced postings.

Status at handoff: IMPLEMENTED_UNVERIFIED, root execution pending. No full purchasing parity acceptance claimed. Tax/price discrepancy review, partial supplier billing, net invoices after supplier returns, invoice credit notes, full Unicode normalization policy, UI acceptance and other §37 workflows remain unfinished.
