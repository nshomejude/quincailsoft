# Wave 2 finance migration

## Preflight and ownership

Read AGENTS.md, architecture §§32,34.3,36–38, legacy server/finance_workflows.py and mobile_money.py plus their tests. Prior summary records 60 passing Java tests and incomplete parity. python-java-parity.json was absent at preflight. Canonical item coverage is not behavioral acceptance.

Own finance/tenders/expenses/integrations, forward migrations V13/V14. Root authorized purchasing-owned PayablesService and typed AP/advance entities. No V1–V12 edits. Root alone executes Maven. Required commands preserve current scoped membership, separate approval permissions, stable operation identity, exact whole XAF, period locks, typed ledger references and atomic outbox.

## Branch inventory and implementation work

| Legacy branch | Existing / wave 2 destination | Acceptance status |
|---|---|---|
| deposit complete/allocate/refund | TenderService; bank evidence funding extension | Existing cash integration tested; bank pending |
| customer payment cash/bank, exact allocation | TenderService + SettlementService | Cash tested; bank pending |
| supplier payment and outstanding | purchasing PayablesService + tender SettlementService | Implementation underway |
| supplier advance complete/allocate | purchasing advance balance + tender settlement | Implementation underway |
| supplier_invoice match/duplicate/mismatch | PurchasingService.invoice | Existing exact receipt-based invoice; full matching exception parity separate |
| expense approved cash/bank payout | ExpenseService + bank settlement | Cash tested; bank pending |
| cash open/close/variance/insufficient/late | TenderService | Existing PG test; historical orphan cannot be inserted via typed command |
| journal balanced post/reverse | ManualJournalService | Implementation underway |
| period close/reopen/pending guards | PeriodService + module guards | Existing PG test; broader draft guards being audited |
| bank match exact fees/unique payment | BankService | Implementation underway |
| quote issue/convert/expiry | quotation owner | Delegated root/sales parity |
| return valuation/physical credit | return owner | Delegated root/sales parity; cumulative rounding gap reported |
| MTN config/initiate/query/replay/unknown/sandbox | MobileMoneyService + MtnCollectionAdapter | Existing simulated transport and PG tests |
| spoofed callback only reconciliation hint | signed/scoped callback ingress | Pending; never authoritative payment evidence |
| Orange initiation/query | explicitly unavailable | Same legacy block; merchant contract external |

Legacy implements no outgoing PSP disbursement/refund, customer-credit payout or provider settlement pipeline. These remain specification scope, not a claim of legacy parity. This document will record exact new tests and remaining branches at checkpoint.
