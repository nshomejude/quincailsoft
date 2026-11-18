# Coverage and outstanding acceptance

This is a substantive history fixture, not a claim that every specification workflow is accepted. The generator covers the economic paths listed below; remaining paths are explicit so they cannot disappear behind a completion percentage.

| Area | Dataset coverage | Remaining acceptance |
|---|---|---|
| Company/branches | One synthetic company, six branches and warehouses | Add a second isolated tenant for adversarial reads; intercompany trading is distinct from current interbranch transfers |
| Customers | 1,000 identities, credit terms/limits, statements, receivables | Java must enforce limits; current generated sales do not model credit-limit approval decisions |
| Catalog | 1,000 families, 3,000 priced/color/size variants, 20 categories, seven base units, yearly prices | Rational alternate packs, serial lifecycle, lots/expiry/recall and CUT/REPACK need dedicated accepted fixtures |
| Personnel | 72 staff, contracts, attendance, 2024–2026 payslips and withholding liabilities | Statutory payroll, leave balances, benefits and full HR lifecycle require specification mapping; synthetic flat deduction is not statutory |
| Procurement | POs, full receipts, invoices, partial payment, AP and advances | Partial receipts, supplier returns/credits, approved price/quantity exceptions and late landed cost allocation require additional typed cases |
| Sales | Discounted, credit, split tender, profitable/loss-making sales, quotations | Quote conversion linkage is reference-only; orders/custody/partial fulfillment are explicit pending cases |
| Returns/refunds | Linked full first-line returns, original cost reversal, AR credit application and paid-balance refund | Multiple partial return rounding, damaged quarantine and chargebacks need separate accepted cases |
| Stock | Exact WAC issue allocation, restocking, write-offs, interbranch transfers, balances | Reservations/exclusive device grants, inventory counts with variances and hardware scans require workflow acceptance |
| Cash/bank | Cash and bank posting streams, running statements, provider net fees | Till open/close denominations and differences, bank-statement matching, outstanding cheques and cash deposits are not yet independent workflow fixtures |
| Accounting | Balanced journals, AR/AP controls, branch/month P&L, trial balances, operating losses | Statutory chart mapping, depreciation/fixed assets, income tax, foreign currencies and full statutory financial statements are not asserted |
| Taxes | Frozen line test rates and monthly output/input/net summaries | Statutory exemptions, current legal rates, filing/payment and fiscal-provider certification not asserted |
| Payments | Synthetic MTN/Orange collections, fees/settlements, non-posting ambiguity cases | Live provider verification, callbacks, retries, disputes and provider-specific acceptance remain Java/external tests |
| Documents | 56k+ linked immutable source digest fixtures, EN/FR, A4/80mm cases | Rendering all canonical templates/fields/58mm/reprints and original numbering must be tested in application |
| Security/recovery | Explicit negative scenario register and scoped identifiers | Isolation, actor epochs, idempotent replay, restore and offline fencing must execute in Java; no security claim from data generation |
| Analytics | 35 months × six branches, profit/loss, stock and statement reference totals | Real screens must compute matching results from the imported Java records |

`workflowCases.ndjson` entries marked REQUIRES_JAVA_ACCEPTANCE are scenarios, not completed transactions. Do not merge them into posted sales/stock or treat them as passing test evidence. The remaining register is intentionally honest: the user's requested gap-free end state requires implementation, ingestion and observed acceptance, not simply a large row count.
