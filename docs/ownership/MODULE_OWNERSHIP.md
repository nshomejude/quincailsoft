# Complete proposed module ownership register

**PROVISIONAL:** per the latest user instruction, finalize allocations only after the platform audit and current Desktop file claims are reconciled. See ../audit/PLATFORM_AUDIT.md. No row proves active work or completion.

Reviewed commit: `adcc7e115bc7f42236fac041aed1898bb456adb8`. Backend: **Java 25, Spring Boot 4, PostgreSQL 18**. No Python production implementation.

CLOUD means this ChatGPT agent. DESKTOP means local Codex, the integration lead. CLOUD accepts the listed assignments now. DESKTOP assignments require its acknowledgment; this register does not pretend to control its session. Ownership is distinct from completion. All 40 modules have one accountable owner; none is release-certified here.

## Canonical modules

| ID | Module | Owner | Reviewer | Delivery / next action |
|---|---|---|---|---|
| M01 | Tenant & provisioning | CLOUD | DESKTOP | Tenant onboarding/provisioning lifecycle, isolation and rollback; existing review flow is only partial. |
| M02 | Organization & branches | DESKTOP | CLOUD | Legal company and branch configuration, warehouse/date scope and approval controls. |
| M03 | Identity & staff | DESKTOP | CLOUD | OIDC invitations, active membership/epoch, staff access; HR/payroll extension is separately assigned to CLOUD. |
| M04 | RBAC / ABAC policy | DESKTOP | CLOUD | Effective role/capability intersection, ABAC and independent approvals. |
| M05 | Subscriptions & billing | CLOUD | DESKTOP | Plans, entitlements, seats/branches, subscription lifecycle, billing and enforcement. |
| M06 | Catalog / PIM | DESKTOP | CLOUD | Typed manufacturer/family/model/variant/supplier-offer identity; preserve source stock pools. |
| M07 | Units & measurement | DESKTOP | CLOUD | Exact rational conversion and pack/precision rules. |
| M08 | Pricing & promotions | DESKTOP | CLOUD | Effective-dated pricing/discount approvals and frozen price/tax snapshots. |
| M09 | Inventory authority | DESKTOP | CLOUD | Sole stock writer, custody/ownership, reservations and authoritative valuation. |
| M10 | Stock count & replenishment | DESKTOP | CLOUD | Blind count, approved variances, reorder and replenishment. |
| M11 | Warehousing & transfers | DESKTOP | CLOUD | Transfer dispatch/receipt/loss and value conservation. |
| M12 | Supplier master | DESKTOP | CLOUD | Supplier identities, terms, duplicate handling and safe bank changes. |
| M13 | Procurement | DESKTOP | CLOUD | PO/receipt/match exceptions, partials/returns and landed-cost provenance. |
| M14 | Supplier accounts | DESKTOP | CLOUD | AP, supplier credits/advances, payments/ageing and control reconciliation. |
| M15 | Customer master | DESKTOP | CLOUD | Customer directory, consent, scoped merges and terms. |
| M16 | Customer credit/deposits | DESKTOP | CLOUD | Credit enforcement, deposits, AR allocations/refunds and exact balances. |
| M17 | POS & sales | DESKTOP | CLOUD | POS posting, held/draft sales, prices/taxes, atomic original document and stock issue. |
| M18 | Payments and settlement | DESKTOP | CLOUD | MTN/Orange intents, ambiguity, callbacks, settlements/refunds/chargebacks; live acceptance external. |
| M19 | Cash management | DESKTOP | CLOUD | Till sessions, independent close, denominations and differences. |
| M20 | Expenses | DESKTOP | CLOUD | Expense approvals, cash/bank impact and vouchers. |
| M21 | Quotations & offers | DESKTOP | CLOUD | Quote revisions, expiry, acceptance and safe sale conversion. |
| M22 | Returns & credit notes | DESKTOP | CLOUD | Original-line return limits, frozen cost, credit and approved refund. |
| M23 | Fulfillment & delivery | DESKTOP | CLOUD | Customer custody and partial pickup/delivery without second stock deduction. |
| M24 | Document & print engine | DESKTOP | CLOUD | Atomic immutable snapshots, numbering, templates, print attempts and all canonical fields. CLOUD performs independent parity audit. |
| M25 | Fiscal configuration & operational journal | DESKTOP | CLOUD | Exact balanced journal, reversal, periods, fiscal policy and operational account mappings. |
| M26 | Operational reporting | DESKTOP | CLOUD | Scoped real aggregates, all canonical reports, analytics and exports. CLOUD performs independent parity audit. |
| M27 | Commerce storefront | DESKTOP | CLOUD | Storefront publication, entitlement, cart/checkout and customer ownership. |
| M28 | Orders & reservation orchestration | DESKTOP | CLOUD | Order orchestration; M09 alone owns reservations. |
| M29 | Offline mobile/sync | DESKTOP | CLOUD | PAUSED: native mobile feature work; inventory/offline authority must still fail closed in backend. |
| M30 | Windows bridge & standalone companion | DESKTOP | CLOUD | PAUSED: standalone WPF and bridge feature work; preserve existing standalone behavior. |
| M31 | Notifications & alerts | DESKTOP | CLOUD | Event-driven notifications, user preferences and delivery retry receipts. |
| M32 | Integrations & webhooks | DESKTOP | CLOUD | Signed integration callbacks, secrets boundary, replay and retries. |
| M33 | Import / export / migration | DESKTOP | CLOUD | Historical Java importer/durable mapping, scoped master imports/exports and recovery-safe replay. |
| M34 | Audit & compliance | DESKTOP | CLOUD | Append-only audit, delivery workers, access/retention and legal hold. |
| M35 | Platform administration | CLOUD | DESKTOP | Separate platform administration, tenant support and consented narrow access; no global merchant bypass. |
| M36 | Industry capabilities | DESKTOP | CLOUD | Industry attributes/capabilities without tenant code forks. |
| M37 | Device and print management | DESKTOP | CLOUD | Server-side device/print profiles; native hardware acceptance paused with clients. |
| M38 | Data lifecycle & backup | CLOUD | DESKTOP | Backup/restore/PITR, deletion/export and retention. Shared operations implementation requires path-level reservation. |
| M39 | Search and indexing | DESKTOP | CLOUD | Bounded scoped PostgreSQL search and reindexing. |
| M40 | Support & diagnostics | CLOUD | DESKTOP | Support diagnostics, consent and expiring audited access grants. |

## Cross-cutting work and extensions

| ID | Workstream | Owner | Boundary and deliverable |
|---|---|---|---|
| W01 | HR and payroll extension | CLOUD | New typed personnel/payroll boundary; coordinate identity and finance public APIs. Employee/contract/attendance/payroll source mapping, immutable money posting, approval and tests. Do not invent statutory payroll rules. |
| W02 | Deployment, runtime and monitoring | CLOUD | New infrastructure/deployment paths; Desktop retains infrastructure/local-preview. Container/configuration/health/observability/deploy and rollback artifacts; deployment destination still not established. |
| W03 | Security release assurance | CLOUD | Isolated security acceptance tests and evidence; shared auth implementation remains Desktop. Review OIDC/RBAC/ABAC, tenant isolation, dependency findings, secrets and external exposure; no blanket certificate. |
| W04 | Performance and recovery testing | CLOUD | Isolated test harnesses/restore rehearsal artifacts; coordinate environment before load. Real PostgreSQL load, latency/capacity evidence, failure/recovery and restore verification. |
| W05 | Historical source dataset | CLOUD | datasets/business-history/generate.mjs; validate.mjs; source reference fixtures. Maintain deterministic manufacturer-aware fixtures and independent financial/stock reference validation. |
| W06 | Historical Java ingestion and reconciliation | DESKTOP | Java adapter and durable mapping; java-import-preflight and import evidence. Resolve missing typed schema, replay chronological events, retry without duplication, reconcile every branch/party. |
| W07 | Original screen fidelity and web wiring | DESKTOP | apps/web-admin; apps/platform-admin shared with CLOUD by page; original previews. Restore original composition and analytics meaning with real Java data; no generic substitutes. |
| W08 | Documents/reporting independent audit | CLOUD | Audit evidence and isolated regression tests; implementation fixes reserved separately with Desktop. Map every template/report/field and Python assertion to Java behavior and identify exact missing implementation. |
| W09 | Shared reactor, schema numbering, SDK and integration | DESKTOP | backend/pom.xml; backend/api-server/pom.xml; db-migrations sequencing; shared locks/contracts. Integrate focused changes; allocate unique migrations; run full reactor and consumer verification. |
| W10 | Release acceptance and main merge | DESKTOP | Commit-bound release dossier and PR integration. Assemble pass/fail/blocked/not-run gates, independent CLOUD review; merge only accepted scope. |
| W11 | Runtime and business configuration | DESKTOP | Merchant company/branch/UoM/pricing/fiscal settings and config validation. Desktop owns merchant business settings; CLOUD owns tenant onboarding/plan/platform/deployment settings. |

## Onboarding and configuration ownership

- **CLOUD: M01 tenant onboarding/provisioning, M05 plans/subscriptions/billing, M35 platform administration, M38 lifecycle/backup, M40 support diagnostics.** Existing onboarding review source is partial, not finished tenant activation.
- **DESKTOP: M02 company/branch settings, M03 identities/invitations, M04 roles/permissions, M07 units, M08 pricing, M25 fiscal/accounting settings.** CLOUD cannot bypass these services when provisioning.
- **CLOUD: deployment/environment configuration and monitoring. DESKTOP: active local preview and shared security/reactor/SDK integration.**
- **CLOUD: HR/payroll extension.** Identity access and payroll accounting remain owned by M03/M25 and are invoked through their public APIs.

## Desktop assignment tracking

| ID | Assignment | Verification required |
|---|---|---|
| D01 | Acknowledge register and reserve active paths | Publish current active files, branch and planned migrations; no duplicate owners. |
| D02 | Implement manufacturer/family/variant/supplier-offer model | Typed identity, scoped uniqueness and negative tests; preserve historical stock identities. |
| D03 | Implement historical Java importer | Durable source mapping and ordered domain commands; zero raw ledger bypasses. |
| D04 | Ingest and reconcile full historical dataset | Counts read/imported/rejected/blocked by source ID; exact stock, AR/AP, cash/bank, tax and P&L reconciliation. |
| D05 | Verify import replay and interruption recovery | Second pass and resumed pass produce no duplicate economic effects. |
| D06 | Restore original screens and analytics | Before/after actual screenshots, screen register and Java-backed KPI evidence. |
| D07 | Complete Java parity and module acceptance | All required acceptance rows resolved with executed current-build evidence; retain explicit external blockers. |
| D08 | Integrate CLOUD-owned modules | Review/cherry-pick tested PRs, resolve shared boundaries, run combined acceptance without overwriting local work. |
| D09 | Full application/browser/database test run | Current reactor, PostgreSQL, auth/role, web workflows and report/document tests with failure counts. |
| D10 | Release dossier and merge | Exact commit, gate statuses, reviewer result, rollback/restore and environment evidence. |

Each assignment starts PENDING / NOT_VERIFIED until Desktop publishes acknowledgment and evidence. Dataset source validation is already reported on Windows; actual historical import remains zero at the reviewed commit. Required follow-up is recorded, not silently treated as complete.

## Merge, test and release responsibilities

1. Each implementation owner writes and executes focused tests on an isolated branch/environment. Root-only serialization rules apply within each shared workspace.
2. Desktop allocates migration numbers and shared-file edits, integrates contributions, resolves conflicts, runs the combined Java/PostgreSQL/web suite and opens the main PR.
3. CLOUD independently reviews specification compliance, visual fidelity and current evidence. Author tests and independent review are separate responsibilities.
4. Desktop owns the final merge operation once required gates pass. CLOUD owns deployment preparation and agreed-target execution; destination/credentials have not yet been established. User's deployment authorization is recorded, but is not an address or account configuration.
5. The release dossier names the exact commit and environment, PASS/FAIL/BLOCKED/NOT_RUN for every gate, unresolved risk, rollback and restore evidence. No production certification exists until those checks pass. Mobile/WPF are paused exclusions, not completed modules.

## Enforcement and handoff

The JSON companion is the machine-readable source; this Markdown is its human-readable view. Run `node scripts/check-module-ownership.mjs` after changes. Every module must retain one owner, a different reviewer, valid dependencies and acceptance requirements. Record any reassignment in GitHub with old/new owner, paths, branch, migration numbers and handoff evidence. Shared directories in JSON are reference areas, not blanket write locks; reserve exact files before coding. No two agents edit common security, reactor, lockfiles or generated contracts concurrently.

Complete means workflow + authorization + real data + original UI + reports/documents + executed tests. File counts, green builds alone and language percentages do not certify it.
