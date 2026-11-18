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
