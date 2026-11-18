# Issue #2 historical dataset integration evidence — 18 November 2026

Desktop Codex owns Java ingestion, local integration and acceptance. The source package is `941f3ba87fb2361a95baff7a8673ff272b34d003`, cherry-picked as `5013e25` onto `handover/codex-computer-2026-11-18`. The first local comparison used application code at `831d2df`. The scoped sales aggregate and acceptance registers are at `f35d258dfc9a6a7d1618f6c59e1954d9bc866c35`. No historical import mapping version has been accepted. Mobile and native desktop remain paused. The independent documents and reporting parity audit remains with the ChatGPT agent.

## Executed source checks

From the repository root, run in this order:

```powershell
node datasets/business-history/generate.mjs datasets/business-history/generated
node datasets/business-history/validate.mjs datasets/business-history/generated
node datasets/business-history/java-import-preflight.mjs datasets/business-history/generated
node datasets/business-history/verify-corruption.mjs datasets/business-history/generated
```

All four commands succeeded on Windows with Node.js 26. Published and generated manifests and validation summaries matched. The validator ran 2,226,314 assertions. Both corruption scenarios were rejected after their fixture checksums were recomputed.

The generated package contains 767,945 rows across 33 files (295,929,133 bytes). Preflight enumerated 245,514 candidate source records with exact IDs, including event type and sequence for 137,984 economic events. At the preflight checkpoint, all candidates were blocked before import. See the authenticated catalog acceptance below for later isolated execution. The other 522,431 rows are reference or acceptance fixtures. **At preflight, imported 0 and rejected by Java 0.** The preflight itself made no database writes. The machine-readable record-level index is `datasets/business-history/generated/java-import-preflight.json`, which is locally reproducible and Git-ignored.

Published source totals in exact XAF are inventory 3,898,492,465, receivables 909,922,062, payables 765,942,251, net sales 3,373,340,314 and operating profit −68,334,076. Source debits and credits are each 45,749,568,268. Application reconciliation is blocked before import.

## Release acceptance

| Gate from RELEASE_ACCEPTANCE.md | Status | Desktop evidence or blocker |
| --- | --- | --- |
| Canonical parity and UI action, field, report and template coverage | FAIL | The strict register still contains 735 unresolved behaviors. |
| Full Java build, PostgreSQL 18 and Modulith | PASS | The fresh 29-module Java 25 reactor passed on PostgreSQL 18.7 with 63 tests, zero failures, zero errors and zero skips. The Modulith boundary test passed. The backend code is identical to f35d258; test runtime and limits are below. |
| OIDC, membership, epoch, scoped RBAC/ABAC | NOT_RUN | Local cashier session rendered the seeded dashboard and product detail. Full adversarial, job, download and report matrix remains pending. |
| Historical import, replay and exact reconciliation | BLOCKED | Java catalog lacks typed manufacturer, family/model, variant and supplier-offer identity. The chronological adapter, durable source mapping, HR/payroll and exact replay equivalence are unfinished. |
| Responsive browser workflows and original branding | NOT_RUN | Actual login, dashboard and Electrical Cable detail captured on the local seeded preview. Sidebar computed color was rgb(21, 149, 230), and the white wordmark was visible. The 390px sign-in, dashboard and product detail checks verified no sign-in horizontal overflow, collapsible navigation and preserved seeded form values. Historical workflows, matched original comparison and complete browser coverage are pending. |
| Live MTN and Orange | BLOCKED | Requires authorized provider environments and actual integration acceptance. Synthetic data has no provider authority. |
| Onboarding, billing, deployment, backups/restore, monitoring and TLS | NOT_RUN | Local preview only. No production deployment or restore was executed. |
| Dependency/security scans and SaaS capacity/recovery | NOT_RUN | No fresh release-scoped evidence for this build. |
| Independent approval and exact economic behavior | NOT_RUN | Await reactor results and chronological import acceptance. |

The preview at http://localhost:3100 uses the small synthetic application seed. Screenshots saved locally as `live-login.jpg`, `live-dashboard.jpg` and `live-product-detail.jpg` under the Git-ignored generated directory, with review copies in [issue-3 preview evidence](migration-evidence/issue-3-preview). The screenshots show the seeded preview only. The owner-side dashboard after installing the new API package is saved as `live-dashboard-sales-summary.jpg`. Its real posted-sales total is 0 FCFA. Day, Monday-based week and calendar-month filters use the same Java API. The browser confirmed the 1–30 November 2026 month and the posted amount returned by the synthetic preview. The remaining three required metrics show their missing connections.

## Executed Java and web checks

- The final full `mvn -B -ntp -fae verify` invocation completed successfully with Java 25 in the Maven container and PostgreSQL 18.7 on a shared loopback namespace with no published ports. Across 28 Surefire XML reports, 63 tests ran with zero errors, failures or skips, including `ModuleBoundaryTest`. Test sources and the backend match `f35d258dfc9a6a7d1618f6c59e1954d9bc866c35`. The run used a disposable database and did not import historical data.

- Full PostgreSQL 18.7 reactor initially failed in `IdentityWorkerPostgresTest` because its migration allowlist omitted the identity revocation table. The fixture now applies all migrations in numeric order. Its separate-owner approval, epoch revocation and idempotent worker projection passed in the targeted rerun.
- `PostgresSaleIntegrationTest` passed on the current sales code. It checks the exact scoped date total, exclusions for draft sales and wrong day, rejection of oversized ranges, empty cross-tenant/branch totals and immutable custody collection facts.
- `FullApplicationPostgresTest` and four `SecurityBoundaryTest` cases passed. The signed test token can read the summary; an unauthenticated request cannot. This is narrower than the complete deployment RBAC/ABAC matrix.
- The full rerun passed settlement, tender, return, provider, expense, reporting, sync and platform PostgreSQL tests before reaching the worker failure. The reporting case exercised permitted and denied source capabilities.
- Twenty-one web Node tests, Next.js TypeScript checks and the project rule gate passed. Strict parity still reports 735 unresolved entries. This checkpoint does not establish a release.

## Next implementation boundary

Start with typed company-scoped manufacturer and model/variant identity in catalog under architecture §§36 and 38. Preserve separate brand and supplier offer relationships, maintain stable idempotency and leave posted history immutable. Test identical model names across manufacturers, duplicate/conflicting identities, tenant/company isolation and scope denials. Then implement the isolated Java import adapter and resume/replay ledger under §§32, 37, 38, 41 and 44. Exact reconciliation must compare per-branch and party-level application values against the reference, with no raw inserts into economic tables.

## Authenticated historical catalog acceptance

The standalone Java 25 importer under `backend/tools/history-import` ran twice against a disposable PostgreSQL 18.7 database and the real Spring Boot API using a generated test issuer and a signed actor in the historical synthetic scope. The four V23 HTTP write endpoints each denied anonymous and read-only requests, rejected mismatched operation IDs, and returned the same response on authorized replay. The supplier quote retained its exact integer string beyond JavaScript's safe integer range.

The initial catalog and party pass processed 14,117 fixture records and produced 17,117 mappings. A variant has both a stock-SKU creation and a variant-association command. The test asserted stored counts after replay and byte-identical mapping files with SHA-256 `091210a9d3d7d7d612a4d8d2e28a439a68043bb823e0ce24cac21de45495d227`. Historical counts exclude the pre-existing HTTP test rows.

| Imported fixture | Records |
| --- | ---: |
| Manufacturers | 10 |
| Categories and units | 27 |
| Suppliers and customers | 1,080 |
| Product families | 1,000 |
| Sellable variants | 3,000 |
| Supplier offers | 9,000 |

Executed `QUINCAI_HISTORY_CATALOG_ACCEPTANCE=true mvn -B -ntp -pl api-server -am -Dtest=FullApplicationPostgresTest -Dsurefire.failIfNoSpecifiedTests=false verify` in the isolated test container. One expanded acceptance scenario passed, zero errors, failures or skips. The process rejected a non-loopback destination in a separate local check.

This database was disposed of by the test. The running preview has zero historical records. The preflight report describes the state before this test; 231,397 candidate records were outside this pass. Restart after an actual interruption, authenticated export generation, chronological economic posting and normalized branch/party reconciliation remain pending. The imported quotes did not change stock valuation. The separately integrated export checker passed 29 local Node tests and still awaits actual Java financial exports.

## Authenticated opening capital prefix

Sequence 1 (`50568a8b-4ab9-5696-a1ab-c3f17692357d`) was posted through draft, submit and independent approval on the Spring Boot API. The disposable PostgreSQL 18.7 test observed one posted journal after two passes, with exact source net lines BANK 1,900,000,000 XAF, CASH 100,000,000 XAF, and EQUITY −2,000,000,000 XAF. The initial and replay mappings had the same SHA-256 `e6f254f54d95ac950331e50df0af93b76eee88c7477b370711420ccc4bb9b155`.

Executed `QUINCAI_HISTORY_OPENING_ACCEPTANCE=true mvn -B -ntp -pl api-server -am -Dtest=FullApplicationPostgresTest -Dsurefire.failIfNoSpecifiedTests=false verify` on the isolated fixture. The expanded scenario passed without failures, errors or skips. This is one historical economic event, not full replay or financial reconciliation. Sequence 2 and later events, interruption coverage and deployment of historical data in the running preview remain pending.
