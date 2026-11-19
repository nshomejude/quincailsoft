# Work allocation — 18 November 2026

## Branch and checkpoint

- Repository is `nshomejude/quincailsoft`. Work is based on `handover/codex-computer-2026-11-18` at `e22522f`.
- The local gate checkpoint immediately preceding this register is `b3d77e43f73d768df531db4c50292bc1f6ce3e65`. The commit containing this register is the coordination checkpoint.
- Keep this branch separate from `main`. Mobile and native desktop work are paused. Preserve the Python reference and its tests.

## Codex ownership and active paths

- Java reactor, shared API security and configuration, and serialized Maven and PostgreSQL work. Active files are `backend/pom.xml`, `backend/api-server/pom.xml`, inventory `DefaultStockService.java` and customer-accounts `MasterDataPostgresTest.java`.
- Local Spring Boot and Next.js preview with isolated PostgreSQL, Redis and OIDC. Preview files live under `infrastructure/local-preview/`. Generate credentials locally. The separate integration-test database is disposable.
- Java API verification through `apps/web-admin` catalog forms and tests. Codex owns final integration, dev restarts, shared lockfile, OpenAPI/SDK regeneration and final acceptance evidence.
- Parity inventory and gate tooling under `scripts/` and `docs/migration/`. No rows were accepted from inspection alone.

## Verification and remaining migration

- Project rule gate and three gate regression tests passed. Nineteen web Node tests passed, including unit and product contract tests. Next.js dependency installation timed out, so typecheck, build and authenticated browser execution remain pending.
- The Java 25 Maven reactor run against isolated PostgreSQL 18.6 is underway. Operations reactor wiring, the full migration selection in the master-data test, and the lot-guard fix await the result. The earlier 60-test result applies to the pre-handover checkpoint.
- Local preview realm JSON and demo seed are reviewable. Actual Keycloak import, Flyway startup, seed application and owner/cashier browser sign-in remain pending.
- The strict parity inventory has 735 unresolved entries. Required work includes active role/capability intersection and customer ownership scoping, V13–V21 acceptance, operations API assembly and recovery controls, payment callback and reconciliation safeguards, inventory custody and valuation conservation, and report/document capability and replay verification.
- After backend contracts stabilize, verify corresponding web routes and detail/actions end to end. Record executed evidence by behavior and keep external provider, fiscal and hardware acceptance distinct.

## Independent assignments available

Start from the published checkpoint on a separate branch. Read `AGENTS.md`, the applicable architecture sections and legacy source/tests. Select one scope and report exact paths before editing.

| Assignment | Isolated ownership and expected return |
| --- | --- |
| Documents and reports review | Audit `documents` and `reporting` against §§38–39 and corresponding Python assertions. Propose scoped regression cases and identify missing templates and projection fields. Keep visibility and replay-denial checks explicit. |
| Inventory parity review | Map the remaining legacy lot, custody, partial collection, return and conversion assertions to Java tests under §§32, 37 and 40. Identify residual valuation cases and scope failures. Coordinate before editing inventory implementation. |
| Finance and provider evidence review | Audit legacy settlement, customer deposit, AP, reconciliation, refunds and provider ambiguity against §§37–38. Identify exact missing behavior and author isolated tests by agreement. Never treat a callback alone as settlement. |
| Operations parity review | Compare `server/operations.py` and its tests with the Java imports, recovery and export components. Document missing handlers and failure cases under §§38, 39 and 41. Coordinate before implementing API changes. |

Codex coordinates migrations, shared security and final integration. Submit an independent branch or draft PR with changed paths, spec references, legacy cases, executed tests and remaining gaps. Coordinate shared database or reactor execution before running it. Leave mobile and native desktop untouched.

## Local integration follow-up

- The Java 25 API package built. Flyway started the disposable PostgreSQL 18.7 preview and the synthetic seed committed. The browser signed in as the preview owner through Keycloak and loaded a dashboard with two catalog records from Java.
- A forward custody collection immutability migration and the reporting RBAC fixture correction are reviewable. The targeted PostgreSQL rerun is pending following a Docker engine error.
- The workspace restoration references the original login and dashboard previews and the S11 product-detail contract. The final branded login/dashboard/detail screenshots and narrow-width verification are pending after the browser stopped responding. Product history and label printing still require Java integration.

## Issue #2 historical dataset

Desktop Codex has taken integration ownership and cherry-picked the synthetic fixture package. See [dataset import evidence](dataset-import-evidence.md) for the verified input, exact pre-import blocker index, live preview scope and release gate status. The cloud agent retains the independent documents/reporting parity audit. Mobile and native desktop remain paused.

## Issue #3 design fidelity and module acceptance

Desktop Codex integrated the policy audit from commit 4e9520c and owns the screen and module registers. Run `node scripts/generate-acceptance-registers.mjs` after updating the canonical coverage and parity sources. The generated registers distinguish source candidates from accepted evidence. S01 now has a scoped Java posted-sales summary with a business-date selector in Next.js. Additional date presets, preparation, low-stock and collection queues remain unaccepted. See the [fidelity audit](migration/SCREEN_FIDELITY_AUDIT.md) and [dataset evidence](dataset-import-evidence.md).

## Issue #4 consolidated delivery control

Desktop acknowledges [issue #4](https://github.com/nshomejude/quincailsoft/issues/4) and integrated the published ownership proposal and platform audit. The proposal is reviewed for overlap. This acknowledgment records boundaries, without accepting any module or assigning work on behalf of the cloud agent.

- Desktop active paths are `backend/business-modules/catalog/`, sales summary integration, historical Java import and source mapping, `infrastructure/local-preview/`, `apps/web-admin/`, and combined test/evidence. Shared Maven reactor, API security, common SDK/contracts and migration numbering remain serialized under Desktop.
- V1–V22 remain immutable. V23 is reserved for the next Desktop catalog identity migration. This is a reservation; no V23 SQL has been applied. Coordinate any cloud migration numbers before editing shared schema.
- Cloud source-dataset generation/reference validation and independent documents/reporting parity audit stay separate from Desktop Java ingestion and UI. Cloud platform/onboarding, billing, HR/payroll and deployment/recovery proposals remain in the consolidated ownership register. Shared code and module-level handoff require explicit path coordination.
- The deposited preview shows the actual Java posted-sales result and day/week/month filtering. Preparation queues, reorder aggregates and pending collections remain visibly pending. Historical ingestion remains blocked before import and release verdict remains NO-GO. The historical dataset and exact branch/party reconciliation remain mandatory under issue #2. Mobile and native desktop remain paused.

See [delivery control](DELIVERY_CONTROL.md), [screen fidelity](migration/screen-fidelity-register.json), and [module acceptance](migration/module-acceptance-register.json) for the current coordination boundaries.

### Current migration coordination

Issue #4 now records Cloud's V24 platform-control, V25 subscriptions, and V26 workforce reservations. Desktop retains V23 and the historical importer. The opening-capital adapter reuses the current journal workflow and does not allocate a migration. Cloud contributions still require focused review and combined acceptance before release.

## Combined integration on the handover branch

Draft PR #11 source `9c1e13923b478ff41864f843613942e9ad3052da` was
cherry-picked locally as `779ce12` above the verified receipt checkpoint
`570515c`. V23 remains Desktop-owned and V24 through V26 are from PR #11.
GitHub subsequently merged PRs #5 through #11 into handover at `831d2fa`.
Desktop synchronized to that commit after confirming identical backend, web,
and infrastructure paths against the local verification candidate.
The 21 web consumer tests, TypeScript check and Windows Webpack production
build passed. The one-click demo owner sign-in reached the live Java-backed
dashboard. Order queue, low-stock and collection cards still require wiring.
Combined PostgreSQL replay and broader consumer acceptance remain open.
The local deployment environment validator passed 12 tests. Historical supplier
payment needs authenticated bank evidence creation and approval before the
settlement can consume it. The historical bank reference must be preserved in
that evidence. Continue checking the actual payable and journal after replay.

The first combined local reactor run was interrupted when Docker Desktop's
Linux container API began returning HTTP 500 and disconnected the Maven client.
It completed the module-level reporting, workforce, onboarding and deployment
safety checks before the full-application historical replay was interrupted.
There is no combined result for that run. Docker Desktop was restarted, and
the original preview database volume recovered. Windows loopback checks
returned HTTP 200 for the preview and Keycloak sign-in and UP for the Java API.

## Cloud delivery after merging PRs 5–11

All seven PRs were merged into `handover/codex-computer-2026-11-18`, culminating at `831d2faf55c65832182086be780f356cc3d3b0ad`. Main remains separate. The subsequent cloud implementation owns V27–V30, onboarding bootstrap APIs, subscription lifecycle, workforce lifecycle, operational documents/reports, platform-admin, new web workforce routes, and shared billing command enforcement. Existing Desktop historical importer/catalog flows are preserved.

The new implementation and source-bound verification are recorded in `docs/migration-evidence/cloud-owned-completion/`. These results supersede the earlier cloud checkpoint only for the explicitly tested changes; the earlier evidence remains historical. Full module/screen/parity/release acceptance is not inferred from compilation or test counts.

## Dashboard summary checkpoint

Handover commits `6fe42f5`, `ef01ebe`, and `676f0da` connect the original
collection, order preparation, and low stock cards to scoped Java APIs and
retain the HTTP integration assertions. Java 25 with PostgreSQL 18.7 passed
`TenderPostgresTest` (partial payment, full allocation, and branch scope) and
`FullApplicationPostgresTest` (anonymous rejection and authorized dashboard
counts with historical acceptance flags disabled). The web type check and
21 web consumer tests passed. Browser acceptance remains open.

Complete assigned platform workflows and acceptance before resuming historical
import. The local historical sequence replay changes are preserved and their
expanded acceptance test remains unverified.

## Cloud V33–V39 continuation

Cloud integrated Desktop handover `9c1084b`, preserving Java dashboard aggregates and historical importer work. V33–V39 add commercial terms/export cursors, authoritative quotas, leave/benefits, consented support and governance, durable notification delivery, fenced recovery receipt reviews and bounded metrics. Shared service annotations enforce suspension within the command transaction. Deployment and private-network whole-database recovery are included. See [executed evidence and precise remaining scope](migration-evidence/cloud-governance-recovery/README.md); successful tests do not close unaccepted parity or visual rows.

## Fulfillment continuation, 19 November 2026

The continuation branch starts from handover 0bb0995. Historical import remains
deferred until assigned platform behavior passes acceptance. The preserved
unfinished source checkpoint is 25d2db8 on codex/preserved-local-checkpoint-2026-11-19.

The preparation queue now includes PARTIAL tasks (5f6aedc). Whole-order dispatch
and direct completion of a pending task record dispatch lines in the same
transaction. Completing an already dispatched task preserves its existing
dispatch history. Existing request fingerprints remain stable.

Executed Java 25/PostgreSQL 18.7 PostgresSaleIntegrationTest on 19 November 2026.
The scenario passed with zero failures, errors or skips. It verifies partial
shipments, exact cumulative quantities, stale and excessive dispatch rejection,
whole-order retry without duplicate history, and unchanged stock/journal counts.
The project rules check also passed. Direct pending completion has source
implementation but still needs its own regression assertion. Browser acceptance,
original fulfillment screen integration and full platform parity remain open.

Direct pending completion regression passed on PostgreSQL 18.7 on 19 November.
It verifies required proof, exact dispatch quantity, retry without duplicate history,
empty remaining picklist, and unchanged stock movement and journal counts.

Dispatch persistence now uses the typed FulfillmentDispatchLine JPA entity in
the sales aggregate. PostgreSQL 18.7 acceptance passed for partial, whole-order
and direct-completion paths. A shipment with a valid first line and invalid
second product rolls back all dispatch lines and preserves the task version.
The project rules check passed. No new database migration was required.

## Concurrent fulfillment acceptance

PostgreSQL 18.7 acceptance at a7ddb69 races two 1500 milli-unit dispatches
against a 2000 milli-unit sale and the same fulfillment version. One commits,
one receives RevisionConflict, and the remaining picklist contains exactly
500 milli-units. Dispatching the final 500 completes the shipment. Stock
movement and journal counts remain unchanged. Source hashes and the executed
report are retained under desktop-fulfillment-2026-11-19/concurrency.json.

## Fulfillment queue pagination assignment

Desktop owns FulfillmentService, its sales application/controller, the S74/S75
web consumer, and V50 for this increment. Architecture sections 7.4 (signed,
scoped keyset pagination), 31–46 (typed/JPA authority and exact quantities),
and original S74 filter/detail actions govern the change.

The legacy list's 200-row bound must not become a complete queue. Add a
separate paginated read with status/search filters, a 100-row limit and an
expiring signed cursor bound to tenant/company/branch/actor/epoch and filter.
Use a security-invoker read projection for customer labels and sale warehouse.
The signing key stays in a private database table and is never returned.
Acceptance includes ordered pages, exact next cursor, filter/scope/tamper/expiry
rejection, and the unchanged legacy list contract. Original screen acceptance
remains open until the Java-backed queue is rendered and exercised.

## 19 November checkpoint before further migration

Current branch: codex/platform-continuation-2026-11-19; preceding commit 52a8461. Active paths: apps/web-admin fulfillment routes/components/tests, packages/api-contracts, packages/generated-ts-sdk and scoped browser evidence. See migration-evidence/fulfillment-web-2026-11-19/README.md for executed checks and untested workflows. Historical import is last; mobile/native desktop remain paused. Independent documents/reporting parity audit remains reserved for the ChatGPT agent. Full platform migration and release acceptance remain incomplete.

## 19 November quotation and catalog continuation

Branch codex/platform-continuation-2026-11-19; source checkpoint cfeb8ce6c866ad7e9b79edb296fa82a449f3d9d1. Desktop changed quotations API/service/entity, forward V52, original quotation web route, proxy, contracts/SDK and scoped evidence. PostgreSQL HTTP acceptance, 30 web tests, production build, ten contract tests, SDK typecheck and actual owner browser issue/accept/sale-staging workflow passed. Full M21 and platform acceptance remain incomplete.

Parallel read-only catalog review found V23 typed manufacturer/family/variant/offer data lacks read APIs. Next Desktop scope: company-shared product identity readback and separately protected procurement-offer reads; preserve product/stock IDs and deny cashier cost disclosure. No migration required for readback. The independent documents/reporting parity audit remains reserved for the ChatGPT agent. Historical import stays last; mobile/native desktop stay paused.
