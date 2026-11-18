# QuincaiSoft platform audit and deployment verdict

**Verdict: NO-GO for production deployment.** Reviewed commit `adcc7e115bc7f42236fac041aed1898bb456adb8` on 18 November 2026. This is a substantive source/document audit with a complete 40-module checklist, not a claim of exhaustive executed runtime verification. No honest four-hour release guarantee can be derived from the current evidence.

## Audit basis

The user's architecture and amendments are authoritative. Java 25 / Spring Boot 4 / PostgreSQL 18 remain mandatory. Official Odoo documentation provides representative trade/inventory capability examples, OWASP ASVS supplies a security-verification framework, and PostgreSQL documentation informs RLS and recovery checks. None alone certifies a complete quincaillerie/company ERP. Source references are listed below.

### Hardware and building materials

Manufacturer vs brand vs supplier vs model/variant; alternate SKUs/barcodes and supplier quotes; unit/pack/length/area/volume/weight precision; buying in cartons and selling pieces; cable/steel/timber cuts and remnants; batch/serial/expiry and quarantine; reorder/stockouts, counts, warehouses and transfer loss; landed costs and frozen return values.

### Trade and customer operations

Counter/contractor/wholesale pricing, approvals/discounts, quote-to-order-to-sale, credit limits, deposits and split tenders, held sales, tax snapshots, partial pickup/delivery and customer-owned custody, original-linked return/credit/refund, debt collection and statements.

### Company management

Companies/branches, staff and access, HR/payroll extension, suppliers/procurement/AP, expenses, till closing/bank reconciliation, operational journals and periods, tax summaries, audit, reports and original documents. Full statutory accounting, assets/budgets/projects/CRM are scope decisions rather than assumptions.

### SaaS operations

Self-service/assisted onboarding, isolation, plans/entitlements/billing, consented support, configuration, notifications, data import/export/deletion, monitoring, backups/restores, incident response, deploy/rollback and scale.

### Cross-cutting acceptance

Real data and exact calculations; transactions/idempotency/concurrency; current memberships/roles; privacy, secrets and accessible bilingual interfaces; original screen fidelity; user-visible failures and safe retry; no placeholders represented as complete.

## Evidence and limitations

- Verified 765 local file contents against the current GitHub tree before using them; fetched targeted changed code separately. Production business source used for the direct findings is tied to the reviewed commit.
- Desktop reports owner/cashier sign-in and a small Java-backed preview. This review did not rerun its browser, full reactor, penetration test or production restore.
- Published source dataset checks passed on Windows; **zero historical records have been imported**. All 245,514 candidate records remain preflight-blocked.
- Published parity count remains 735 unresolved behaviors; do not interpret that as a percentage or module count.
- The canonical register enumerates 40 modules, 152 screens, 656 actions, 445 tabs, 1,140 screen-field placements, 523 entity fields, 76 reports, 26 document families and 190 document fields. Its status counts are historical and must be refreshed against actual acceptance.
- Later source includes custody, notification and import/restore-related components despite older unmapped entries; old NOT_MIGRATED flags cannot be treated as proof that no current code exists.

## Release blockers and closure criteria

### A01 — Onboarding approval is not tenant provisioning (BLOCKER)

Modules: M01, M35.

Evidence: backend/platform-control/src/main/java/com/quincaisoft/platformcontrol/application/DefaultPlatformService.java: approve sets APPROVED_FOR_PROVISIONING; no tenant creation in this service.

Required closure: Verified create company/branch/owner/entitlements transaction, outbox and retry; fail/rollback/retry without duplicate tenant; lifecycle suspend/restore.

### A02 — Subscription billing lifecycle is incomplete (BLOCKER)

Modules: M05.

Evidence: backend/business-modules/subscriptions/src/main/java/com/quincaisoft/subscriptions/application/DefaultEntitlementService.java: only requireCommerce query.

Required closure: Trial/active/grace/past-due/suspended/cancelled transitions, plan/seat/branch limits, billing records and independently verified payments; no data destruction on expiry.

### A03 — Manufacturer/family/variant and supplier offers block dataset ingestion (BLOCKER)

Modules: M06, M12, M33.

Evidence: docs/dataset-import-evidence.md: all 245514 candidate records blocked; zero imported; backend/business-modules/catalog/src/main/java/com/quincaisoft/catalog/api/CatalogService.java.

Required closure: Typed identity with 10 manufacturers per type, independent supplier offerings, scoped uniqueness and exact per-SKU stock; chronological importer/replay, all required source IDs accounted for.

### A04 — HR/payroll history is not an implemented business workflow (BLOCKER)

Modules: M03, M20, M25.

Evidence: docs/dataset-import-evidence.md: HR/payroll unfinished.

Required closure: Define extension contract, employees/attendance/contracts/payroll approval and journals, payslips and role restrictions. Synthetic rates cannot become statutory defaults.

### A05 — Current-role authorization acceptance remains unresolved (BLOCKER)

Modules: M03, M04.

Evidence: backend/api-server/src/main/java/com/quincaisoft/security/TenantScopeFilter.java: token scope/epoch context; no effective capability intersection here; docs/WORK_ALLOCATION.md: role/capability intersection unresolved.

Required closure: Inspect complete security path and test old roles/new epoch, revoked membership, cross-tenant/company/branch, report/download/job capabilities and own-customer access; source inspection alone is not an exploit proof.

### A06 — Full migration acceptance is unresolved (BLOCKER)

Modules: M01, M40.

Evidence: docs/WORK_ALLOCATION.md: 735 unresolved behaviors reported; docs/migration-evidence/summary.json: earlier 60-test baseline, full_migration INCOMPLETE.

Required closure: Fresh current-commit full reactor/PostgreSQL and module acceptance; resolve required parity cases individually. Historical tests cannot certify new code.

### A07 — Original dashboard meaning and complete screen parity are not preserved (BLOCKER)

Modules: M17, M26.

Evidence: apps/web-admin/app/page.tsx: record counts and mixed activity list replace original operational metrics; previews/QuincaiSoft_Dashboard_v4.jpg; docs/WORK_ALLOCATION.md: final visual and narrow-width verification pending.

Required closure: Original layouts/actions/tabs and authorized live Java aggregates for sales, low stock, collection and profit. Before/after visual and behavior acceptance for every canonical screen.

### A08 — Live mobile-money acceptance and Orange implementation are incomplete (BLOCKER)

Modules: M18, M32.

Evidence: backend/business-modules/integrations/web/ProviderController.java: Orange unavailable; docs/dataset-import-evidence.md: live providers BLOCKED.

Required closure: Implement authorized contracts, signature/replay/ambiguity and reconciliation safeguards; execute provider sandbox and approved live acceptance. Never mark simulated payment verified.

### A09 — Storefront checkout/order lifecycle is not exposed by reviewed API (BLOCKER)

Modules: M27, M28.

Evidence: backend/business-modules/commerce/src/main/java/com/quincaisoft/commerce/api/CommerceService.java: store/listing/public catalogue only.

Required closure: Cart/order state machine, inventory reservations through M09, payment outcome reconciliation, own-order access, delivery/returns and compensations.

### A10 — Reports and document families are partial and require semantic mapping (BLOCKER)

Modules: M24, M26.

Evidence: backend/business-modules/documents/src/main/java/com/quincaisoft/documents/application/DocumentSourceReader.java; backend/business-modules/reporting/src/main/java/com/quincaisoft/reporting/application/DefaultReportingService.java; docs/migration/canonical-migration-coverage.json: 76 reports, 26 document families, 190 document fields in register.

Required closure: Map all canonical IDs/fields to fixed sources/templates/metrics; test original snapshots, permissions, date basis, pagination totals, EN/FR, A4/58/80mm and reprint audit. 17 definitions are not 17 accepted canonical workflows.

### A11 — Backups and restore have not been demonstrated (BLOCKER)

Modules: M38, M33.

Evidence: backend/business-modules/operations/src/main/java/com/quincaisoft/operations/api/OperationsService.java: manifest/integrity/rehearsal APIs; not a database backup proof; docs/dataset-import-evidence.md: production restore NOT_RUN.

Required closure: Encrypted off-host backup, WAL/PITR retention and actual restore rehearsal; RPO/RTO measured; rekey/revoke credentials/grants and reconcile tenant/ledger after recovery.

### A12 — Production deployment target and environment acceptance are absent (BLOCKER)

Modules: M35, M38.

Evidence: infrastructure/compose.yaml: expressly local development database only; docs/dataset-import-evidence.md: local preview only, deployment/TLS/monitoring NOT_RUN.

Required closure: Established authorized host/account/domain, immutable build, runtime secrets, TLS, migrations, health/readiness, least-privilege network, monitored deploy/rollback and load checks.

### A13 — Ownership and evidence are fragmented and partially stale (HIGH)

Modules: M01, M40.

Evidence: docs/WORK_ALLOCATION.md contains older pending statements and later follow-up; docs/migration-evidence/summary.json refers to old baseline.

Required closure: One control index, exact module/file owners, acknowledged handoffs, canonical inventory and current commit-bound test results. Never equate assignment with execution.

### A14 — Specialized materials workflows need complete acceptance (HIGH)

Modules: M07, M09, M10, M11, M13, M23, M36.

Evidence: ARCHITECTURE.md §§32,37,40; datasets/business-history/COVERAGE.md: lots, packs, CUT/REPACK, reservations, custody and late costs not fully covered by fixture.

Required closure: Prove length/area/volume/weight conversions, lot/serial/expiry, cut remnants/waste, pack split, damaged custody, partial deliveries and late-cost/final-unit conservation.

### A15 — Company-wide financial and corporate features need explicit scope decisions (HIGH)

Modules: M20, M25, M26.

Evidence: ARCHITECTURE.md §37: operational journal, not statutory accounting claim; datasets/business-history/COVERAGE.md.

Required closure: Complete required operational AR/AP/cash/bank/periods/taxes/reconciliation. Explicitly classify fixed assets, depreciation, statutory payroll/accounts, budgeting, CRM/projects as required extension or out-of-scope; no hidden claim of a complete general-purpose ERP.

### A16 — Production security/performance/recovery evidence is missing (BLOCKER)

Modules: M03, M04, M34, M35, M38.

Evidence: docs/dataset-import-evidence.md: scans/capacity/recovery NOT_RUN.

Required closure: Risk-appropriate OWASP ASVS test mapping, dependency findings resolved, tenant isolation under concurrent load, agreed latency/capacity targets, alert/failure and recovery evidence.

## Complete module review register

Source/test counts are discovery only, not completion; related directories can overlap.

| ID | Module | Production Java files in related areas | Test files | Required next verification |
|---|---|---:|---:|---|
| M01 | Tenant & provisioning | 6 | 1 | Tenant onboarding/provisioning lifecycle, isolation and rollback; existing review flow is only partial. |
| M02 | Organization & branches | 10 | 0 | Legal company and branch configuration, warehouse/date scope and approval controls. |
| M03 | Identity & staff | 8 | 0 | OIDC invitations, active membership/epoch, staff access; HR/payroll extension is separately assigned to CLOUD. |
| M04 | RBAC / ABAC policy | 17 | 2 | Effective role/capability intersection, ABAC and independent approvals. |
| M05 | Subscriptions & billing | 3 | 0 | Plans, entitlements, seats/branches, subscription lifecycle, billing and enforcement. |
| M06 | Catalog / PIM | 10 | 0 | Typed manufacturer/family/model/variant/supplier-offer identity; preserve source stock pools. |
| M07 | Units & measurement | 10 | 0 | Exact rational conversion and pack/precision rules. |
| M08 | Pricing & promotions | 10 | 0 | Effective-dated pricing/discount approvals and frozen price/tax snapshots. |
| M09 | Inventory authority | 27 | 2 | Sole stock writer, custody/ownership, reservations and authoritative valuation. |
| M10 | Stock count & replenishment | 27 | 2 | Blind count, approved variances, reorder and replenishment. |
| M11 | Warehousing & transfers | 27 | 2 | Transfer dispatch/receipt/loss and value conservation. |
| M12 | Supplier master | 6 | 0 | Supplier identities, terms, duplicate handling and safe bank changes. |
| M13 | Procurement | 17 | 2 | PO/receipt/match exceptions, partials/returns and landed-cost provenance. |
| M14 | Supplier accounts | 36 | 5 | AP, supplier credits/advances, payments/ageing and control reconciliation. |
| M15 | Customer master | 8 | 1 | Customer directory, consent, scoped merges and terms. |
| M16 | Customer credit/deposits | 27 | 4 | Credit enforcement, deposits, AR allocations/refunds and exact balances. |
| M17 | POS & sales | 11 | 2 | POS posting, held/draft sales, prices/taxes, atomic original document and stock issue. |
| M18 | Payments and settlement | 29 | 5 | MTN/Orange intents, ambiguity, callbacks, settlements/refunds/chargebacks; live acceptance external. |
| M19 | Cash management | 19 | 3 | Till sessions, independent close, denominations and differences. |
| M20 | Expenses | 6 | 1 | Expense approvals, cash/bank impact and vouchers. |
| M21 | Quotations & offers | 6 | 0 | Quote revisions, expiry, acceptance and safe sale conversion. |
| M22 | Returns & credit notes | 7 | 1 | Original-line return limits, frozen cost, credit and approved refund. |
| M23 | Fulfillment & delivery | 38 | 4 | Customer custody and partial pickup/delivery without second stock deduction. |
| M24 | Document & print engine | 15 | 2 | Atomic immutable snapshots, numbering, templates, print attempts and all canonical fields. CLOUD performs independent parity audit. |
| M25 | Fiscal configuration & operational journal | 19 | 1 | Exact balanced journal, reversal, periods, fiscal policy and operational account mappings. |
| M26 | Operational reporting | 5 | 2 | Scoped real aggregates, all canonical reports, analytics and exports. CLOUD performs independent parity audit. |
| M27 | Commerce storefront | 7 | 1 | Storefront publication, entitlement, cart/checkout and customer ownership. |
| M28 | Orders & reservation orchestration | 34 | 3 | Order orchestration; M09 alone owns reservations. |
| M29 | Offline mobile/sync | 7 | 2 | PAUSED: native mobile feature work; inventory/offline authority must still fail closed in backend. |
| M30 | Windows bridge & standalone companion | 0 | 0 | PAUSED: standalone WPF and bridge feature work; preserve existing standalone behavior. |
| M31 | Notifications & alerts | 17 | 0 | Event-driven notifications, user preferences and delivery retry receipts. |
| M32 | Integrations & webhooks | 10 | 2 | Signed integration callbacks, secrets boundary, replay and retries. |
| M33 | Import / export / migration | 17 | 0 | Historical Java importer/durable mapping, scoped master imports/exports and recovery-safe replay. |
| M34 | Audit & compliance | 5 | 0 | Append-only audit, delivery workers, access/retention and legal hold. |
| M35 | Platform administration | 6 | 1 | Separate platform administration, tenant support and consented narrow access; no global merchant bypass. |
| M36 | Industry capabilities | 13 | 0 | Industry attributes/capabilities without tenant code forks. |
| M37 | Device and print management | 15 | 2 | Server-side device/print profiles; native hardware acceptance paused with clients. |
| M38 | Data lifecycle & backup | 17 | 0 | Backup/restore/PITR, deletion/export and retention. Shared operations implementation requires path-level reservation. |
| M39 | Search and indexing | 24 | 1 | Bounded scoped PostgreSQL search and reindexing. |
| M40 | Support & diagnostics | 6 | 1 | Support diagnostics, consent and expiring audited access grants. |

## Consolidated execution order

1. **Freeze and reconcile scope/evidence.** Refresh this audit, canonical registers and active file claims against the latest head. Acknowledge one owner per module; no duplicate writers.
2. **Fix identity/configuration and catalog foundations.** Current role/epoch enforcement; manufacturer/model/variant/supplier-offer identity; exact units/pricing/tax configuration; company/branch settings.
3. **Complete economic workflows and historical importer.** Stock/custody and purchase/sale/credit/payment journals must preserve exact value under replay, concurrency, partials and reversals. Integrate HR/payroll through explicit public APIs.
4. **Complete onboarding and subscription lifecycle.** Provision actual scoped resources, invite owner, enforce entitlements and demonstrate failure/retry/suspension without data loss. Approval status alone is not activation.
5. **Restore original UI and complete reports/documents.** Use real Java aggregates and the historical dataset; compare original/current screens and every canonical field/action/template.
6. **Operationalize and verify.** Authorized destination, TLS/secrets, immutable artifacts, scans/load, monitoring, rollback and real backup/restore; external provider acceptance remains explicit.
7. **Release review.** Current commit-bound acceptance report; close every required blocker or formally exclude genuinely optional scope with user visibility. Do not quietly exclude required modules to hit a deadline.

The four-hour window is a prioritization constraint, not evidence that these gates can be completed. Deployment remains blocked if mandatory checks fail or are unrun.

## References

- [Variant-level SKU/stock identity](https://www.odoo.com/documentation/19.0/applications/sales/sales/products_prices/products/variants.html)
- [Purchase/sale/stock units and conversions](https://www.odoo.com/documentation/19.0/applications/inventory_and_mrp/inventory/product_management/configure/uom.html)
- [Replenishment example](https://www.odoo.com/documentation/master/applications/inventory_and_mrp/inventory/warehouses_storage/replenishment/reordering_rules.html)
- [Application security verification](https://owasp.org/projects/asvs)
- [Backup and recovery approaches](https://www.postgresql.org/docs/18/backup.html)
- [Database row security](https://www.postgresql.org/docs/18/ddl-rowsecurity.html)
