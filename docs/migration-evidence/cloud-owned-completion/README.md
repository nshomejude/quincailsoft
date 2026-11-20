# Cloud-owned implementation and integration — 18 November 2026

Base: handover `831d2faf55c65832182086be780f356cc3d3b0ad`, after all previously open PRs 5–11 were merged. This delivery is for the migration/handover branch, not a certified production release.

## Implemented

- **Onboarding / V27:** approved and verified owner configuration provisions actual tenant, company, branch, warehouse, document settings/series, fiscal period, provisional accounting mappings, trial subscription and owner membership through owning-module APIs. Durable intent survives failure; resource creation and activation commit together. Exact/concurrent retries, cross-scope denial and rollback are tested. Empty 14-day standard trials are supported.
- **Subscriptions / V28:** configurable plans, trial/paid expiry, cancellation, independently reviewed notice/grace evidence, immutable manual bank reconciliation and exact full-month paid activation. A consumed invoice cannot reactivate cancelled service or reset notices. Merchant sales/purchase commands enforce reviewed restrictions in the same transaction; retained receipts and historical reads remain available. Platform subscription payments never consume merchant tills.
- **Workforce / V29:** effective-dated contracts, itemized deductions, cancellation/replacement, independent accrual reversal, bank-evidence-backed recorded payroll payment, payslip HTML and paged CSV. A typed immutable bank-consumption registry retains deferred source/evidence integrity for both existing settlements and payroll. Payments are explicitly recorded bank payments, not provider-certified transfers.
- **Documents/reports / V30:** custody pickup/delivery notes, custody statements and approved recalls use actual scoped source records; related reports include exact supplier balances. Timestamp normalization makes issuance/capture/print/outcome replay stable across PostgreSQL roundtrips. Print outcomes serialize without granting UPDATE on immutable attempts.
- **User interfaces:** original QuincaiSoft logo/palette retained; English/French platform administration connects onboarding, subscriptions, invoice/credit/receipt printing, and independent review. Merchant workforce pages connect employee/contract/attendance/payroll workflows, print and paged export. Existing merchant navigation now links authorized workforce users.
- **Contracts:** source-generated OpenAPI and pinned TypeScript SDK updated. Qualified HTML response types and actual report-code enumeration are handled by the extractor.

## Executed evidence

`result.json` and `maven.log` record the final serialized Java 25 / PostgreSQL 18 run, individual suite results and SHA-256 source manifest. The digest covers actual Java/SQL/POM sources; it does not certify unexecuted browser flows. Frontend build/test logs preserve executed validation. The full test count includes permission-matrix cases and must not be described as that many complete workflows.

## Explicit remaining acceptance and scope boundaries

- Full parity remains blocked: 735 legacy requirements have no accepted evidence mapping. Regeneration changes candidates/hashes only. No module or screen is silently accepted.
- Real OIDC operator/owner login, deployed HTTP/browser workflows, original-versus-current desktop/narrow screenshots, and issuance of fresh scoped owner tokens need the configured environment.
- Fiscal/accounting mappings are provisional pending accountant review; payroll statutory calculations/filings, benefits/leave, partial salary payments and recovery of already-paid payroll are not certified.
- Subscription provider callbacks/refund settlement, metered usage/reserved concurrent allocation, automated notice delivery and general discounted/prorated activation remain separate work. Manual activation intentionally requires a matching configured full-month plan. Read lists currently retain their documented bounds.
- Historical opening/import remains Desktop-owned. This increment does not claim full historical dataset reconciliation.
- Deployment/recovery acceptance remains blocked as recorded in the earlier checkpoint: isolated recovery socket creation was denied. Production TLS/OIDC, restore/key/object recovery, image signing/scanning, monitoring and capacity acceptance were not executed here.

No production readiness or statutory certification is claimed by the handover merge.
