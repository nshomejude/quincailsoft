# Finance workflow implementation — v5 local integration backend

Implemented and independently tested in `server/finance_workflows.py`. Root integration must call `handle` from the authenticated command dispatcher, `validate_record` before draft saves, and `post_sale` in the sale transaction. All use the caller's SQLite transaction; exceptions must roll back the complete command.

## Commands and effects

| Entity | Commands | Effects / required payload |
|---|---|---|
| deposit | complete | Records confirmed cash/bank advance and balanced cash/bank–liability journal; cash requires `cash_session` |
| deposit | allocate | `{sale, amount}`; same customer/branch/company; no over-allocation; creates Deposit payment and liability–AR journal; creates no second cash receipt |
| deposit | refund | `{amount, cash_session}`; approval authority, remaining balance, cash-only refund; linked till debit and liability journal |
| payment | complete | Customer sale allocation or `{supplier_invoice}` supplier allocation; exact outstanding guard; cash session required; manual Mobile money and Deposit tenders blocked |
| advance | complete, allocate | Confirm supplier advance with tender/evidence, allocate against posted supplier invoice; no duplicate cash movement |
| supplier_invoice | match, complete | Exact supplier/PO/completed-receipt match; duplicate supplier reference protection; payable control journal |
| cash | open, close/complete | Unique open register; expected cash derived from opening + recorded till movements; count/variance explanation/approval; immutable close |
| journal | post/complete, reverse | Explicit balanced `entries` with account/debit/credit; immutable journal lines; compensating reversal in open period; system rows must reverse through source workflow |
| period | close/complete, reopen | Blocks closure for unresolved transactions/open tills; approval authority and reason; finance posting rejects closed periods |
| quote | issue, convert | Freezes issue total; conversion creates linked sale draft exactly once; validates expiry; nonzero delivery charges blocked pending service-line mapping |
| expense | complete | Requires approved expense and valid settlement method; till/bank debit and expense journal |
| bank | match/complete | Exact confirmed bank payment/statement/fee matching; duplicate matching blocked; fee journal |
| sale helper | post_sale | Immediate handover, zero-tax sale only: balanced AR/revenue + COGS/inventory journal with immutable cost snapshots |
| provider helper | post_verified_provider_payment | Adapter-verified live completion only: provider clearing/AR journal; never sandbox money |

`finance_postings` and `finance_cash_entries` retain immutable source-linked postings and till movements. Operational journal lines are mirrored into the existing journal entity for UI/report visibility. These tables must be backed up/restored consistently; a business-record-only restore must block if these ledgers exist.

## Executed verification

13 unittest cases pass (`python3 -m unittest discover -s tests -p test_finance_workflows.py -v`): deposit allocation/refund conservation and double-spend rejection; derived balance editing rejection; customer/branch mismatch; invented Mobile money/Deposit funding rejection; supplier matching/payables; cash derivation and closed-till rejection; balanced journal/reversal/closed-period checks; quote conversion uniqueness; approver restriction; exact bank fees/duplicate match; full transaction rollback on posting failure; sale financial postings; verified-provider gate.

## Remaining limits

- Account names are operational control accounts, not certified SYSCOHADA account mapping or tax/fiscal compliance.
- Nonzero-tax sale recognition is blocked until a fiscal calculation policy is implemented. Delivery and customer-custody sale recognition are blocked until approved handover/bill-and-hold policy and ownership journals can be proved.
- Physical return completion is implemented separately by inventory. Customer/supplier credit note valuation, proportional discount/tax allocation and refundable-balance accounting remain incomplete; no cash/provider refund is fabricated.
- Invoice matching supports an exact receipt total, not partial invoicing, tolerances, landed-cost allocation or tax recoverability classification.
- Bank postings require an approved operator and transaction reference; this is a manual recorded-bank-evidence path, not an automated bank API verification.
- Historical demo cash movements without a cash-session assignment block cash close. The legacy `expected_amount` snapshot is intentionally not treated as a ledger. A production migration must reconcile and assign historical movements, not silently infer their till.
- Full multi-company/company-currency mappings, statutory closing, formal fiscal numbering/issuance, revaluation, advanced reconciliation, age-bucket metrics, payroll and full accounting export remain outstanding.
- Whole-request idempotency is provided by the root command dispatcher; direct handler tests verify domain effects and do not replace API replay tests.
