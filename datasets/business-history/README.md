# QuincaiSoft synthetic operating history

A deterministic, relational test dataset covering 2024-01-01 through 2026-11-18. All people, suppliers, documents and transactions are fictional. It is designed to exercise real Java workflows and real calculations, not to represent actual customer transactions or statutory records.

## Generate on the Windows PC

From the repository root with Node.js 22 or newer:

```powershell
node datasets/business-history/generate.mjs datasets/business-history/generated
node datasets/business-history/validate.mjs datasets/business-history/generated
```

No external dependencies, API keys, Python, database connection or network calls. Allow approximately 300 MB disk and 1–2 GB available RAM. The generated directory is deliberately excluded from Git: the reproducible generator and reviewed manifest are versioned instead of hundreds of thousands of redundant rows. Money and quantities are decimal strings on disk. Amounts use integer XAF and half-even rounding; quantities are thousandths of the SKU's base unit. IDs and content hashes are deterministic.

There are 1,000 product families and 3,000 sellable SKU variants. A family is not an additional stock item: only variants own quantities and valuation. Three variants per family have explicit color, size and price. Meter-based cable supports fractional quantities. Units, categories, price lists and supplier relations accompany the catalog.

## Included data

- Six Cameroon branch scenarios, six warehouses represented by branch warehouse IDs, one isolated synthetic company/tenant.
- 1,000 customers, 80 suppliers, 72 employees with role distribution in every branch, employment dates/contracts, daily attendance and monthly payslips.
- Historical sale lines, line discounts, taxes, frozen issue costs, credit sales, split tenders, cash/bank and simulated mobile-money collections, provider fees, credit notes, returns and refunds.
- Purchase orders, goods receipts, matched supplier invoices, partial/full payments, outstanding payables, customer/supplier advances and advance refunds.
- WAC inventory movements, transfers through in-transit and branch clearing, physical damage write-offs and ending stock control values.
- Rent, utilities, delivery expenses, payroll, profitable and loss-making sales, branch/month profit and loss, trial balances, tax summaries and running customer/supplier/bank/cash account statements.
- 35 financial periods, quote states, source-linked document snapshot digests, pending/unknown/failed provider cases, and explicit unimplemented-workflow acceptance cases.

Source accounting records are authoritative; monthly analytics, tax summaries, trial balances, ending stock and statements are independent import comparison targets, not rows to load as pretend application analytics.

Tax and payroll values are synthetic scenario parameters. Product zero-rating is not a legal classification; the flat payroll deduction is not Cameroon payroll law. Tax summaries are not filed returns. Profit is operating profit, before statutory income tax and unmodeled depreciation/finance adjustments. Provider evidence is simulated and must never be submitted to live MTN/Orange or relabeled provider-verified. Names/emails are unmistakably synthetic, email domains are example.invalid, and phones are intentionally null to prevent contact with real people.

## Dataset checks versus application acceptance

`validate.mjs` checks checksums, counts, foreign keys, unique operation IDs, transaction balance, stock replay with no negative/residual stock, inventory control, customer AR/supplier AP controls, original return costs, statement running balances, monthly P&L, employment dates, document hashes and non-posting provider exceptions. `evidence/validation.json` records the actual run. Passing these checks does not certify Java ingestion, legal compliance, UI completeness or all business edge cases.

Read `COVERAGE.md` before claiming gap-free coverage. Unsupported workflows are explicitly identified and must be completed in Java rather than silently skipped or fabricated. See `DESKTOP_IMPORT.md` for integration responsibilities.

## Manufacturer and supplier identity

The 1,000 product families are 20 product types × five model lines × ten distinct manufacturers. Each family has three sellable variants (3,000 SKUs total). Manufacturer is a first-class reference, not a free-text supplier name. Every variant preserves manufacturer/brand; each has three supplier offers, giving 9,000 purchasing options. A supplier can distribute multiple manufacturers; a manufacturer can have multiple suppliers. Never merge two manufacturers' SKU stock just because display name, size or color matches. Supplier-offer quotes do not rewrite posted acquisition cost or stock valuation.
