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
