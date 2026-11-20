# QuincaiSoft — computer Codex handover, 18 November 2026

## Start here
Read AGENTS.md, ARCHITECTURE.md (sections 31–46 override earlier abbreviated examples), this document, and docs/migration-evidence/summary.json. Continue the prescribed Java migration; do not rebuild in Python or replace typed aggregates with generic JSON CRUD. User authorizes implementation, agents, tests and GitHub checkpoints. All cloud agents are stopped. This branch preserves interrupted work, not a verified release.

Repository: https://github.com/nshomejude/quincailsoft
Handover branch: handover/codex-computer-2026-11-18
Main checkpoint: bfb0265c4aeee9c78bb33f8aca3dcebd001dbef0 (PR #1 merged at user request).
Tested migration parent: 6e3cbfa9ff434cf4cddf839350820c0f3f7a7403.

## What is verified versus unfinished
Main checkpoint passed Java25 Maven verify across 28 modules: 60 tests, no failures/errors/skips, real PostgreSQL18.7. Twelve migrations passed 19 direct database assertions. Three Next builds passed. Expo TypeScript, seven SQLite queue tests and Android JavaScript export passed; no APK/iOS/native-device certification. .NET adapter passed 24 Linux SQLite/crypto/HTTP assertions; original R02 Windows host integration remains pending and its inherited packages failed vulnerability audit.

THIS HANDOVER ADDS UNVERIFIED WAVE-2 WORK. Do not apply main's 60-test result to it. Only root's new MasterDataPostgresTest was executed separately and passed (customer/category/unit/product metadata, replay/races/scoping). Full wave-2 reactor compile, migrations, browser flows and tests remain to run. Some files may be half-finished because user explicitly paused the workers. Do not delete them or blindly merge this branch.

The source coverage register is conservative: 32/40 modules and 92/152 screens have related partial source evidence; this is NOT a completion percentage. 60 screens lack mapped evidence. The new Python parity inventory currently reports 735 unresolved rows and zero accepted; agent review was interrupted. That does not mean zero implementation: review real tests and record narrow evidence rather than changing flags to make the gate pass. Older 45-module register has 40 unique module IDs and five repeated revised entries; preserve their requirements.

## Required stack
Java25 / Spring Boot4 / Spring Modulith / Hibernate-JPA aggregate writes / jOOQ approved reporting reads / PostgreSQL18 / Flyway. Exact integer XAF and milli-base quantities. Forced tenant/company/branch RLS, active membership/authorization epochs, OIDC/PKCE, audit/outbox, durable idempotency, independent approvals. Next16/React/TypeScript web; Expo56 mobile; .NET10 WPF/SQLite standalone desktop retained.

## Interrupted ownership and source areas
- Finance worker: V13/V14; purchasing PayablesService, supplier payments/advances/allocations, bank confirmation and reconciliation, bank-funded receipts/deposits/expenses, manual journals and reversals. New SettlementPostgresTest authored; not executed. Signed MTN callback inbox was approved as hint-only but implementation may be incomplete. Check docs/migration/wave2-finance.md.
- Inventory worker: V15/V16; reservations, lot recall/quarantine, approved custody-recognition policy, separate customer custody and fulfillment. Sales/returns changes may be incomplete. Collection must not issue merchant stock or COGS twice. Half-even proportional valuation and final-unit residual clearing were being added. Require actual PG conservation tests.
- Operations worker: V17/V18; new operations module, typed preview/commit imports, exports, notifications, recovery integrity manifests/rehearsal (NOT arbitrary JSON ledger restore), supplier metadata CRUD, session revocation. Check docs/migration/wave2-operations.md. May need reactor/API dependencies added.
- Documents/reporting worker: V19/V20; 17 source capture families, immutable snapshots/print attempts/outcomes, deterministic EN/FR A4/58/80 layouts, expanded scoped report projections. Tests and contracts may be unfinished. Source capabilities must prevent cashier financial/cost disclosure, including replay.
- Root: V21; CustomerDirectory CRUD/page/detail and all legacy metadata; CatalogMasterService categories/units with cycle checks and immutable base/rational scale; expanded CatalogService metadata/inactive imports. MasterDataPostgresTest passed against V1–V12 + V21 only. Update it to run all complete migrations in final integration. Shared package scripts added.
- Web worker: customer/category/unit/other new routes, search/paging, localized feedback and domain wiring. Recheck contracts against final Java DTOs and rerun TS tests/build.
- Parity worker: scripts/inventory-python-parity.mjs, check-project-rules.mjs, check-python-java-parity.mjs, project-gates.test.mjs, baseline/acceptance JSON, CI workflows, docs/migration/python-java-parity.json. Rules + two gate tests passed before interruption; full parity intentionally fails. Review CI before pushing to main: strict parity blocks incomplete work.

## Known root integration tasks not yet implemented
1. Wire new operations module and changed dependencies in backend/pom.xml and api-server/pom.xml; check cycles with Modulith.
2. Root had PLANNED (not implemented) effective authorities = signed token capabilities intersect current database role. TenantScopeFilter currently establishes signed scope; ScopedTransactions checks current membership/epoch. IdentityService.current() was added by operations worker. Implement and test role constraints without bypassing distinct platform issuer/control plane. Customer portal own-customer scoping is not finished.
3. Signed callback boundary proposal: /api/public/provider-callbacks/mtn/{capability}, HMAC scope/expiry, bounded payload and durable inbox dedup only; never trust callback as payment success. Exact security permit/request-ID exemption was NOT applied. Verify implementation before exposing it.
4. Add root role mappings for new permissions, including operations:import/export/recover/integrity and bank/journal/supplier permissions; test denied cases.
5. Regenerate OpenAPI/SDK after DTOs stabilize. Existing generated files may describe only previous checkpoint.
6. All accepted V1–V12 migrations are immutable; changes are forward V13–V21. Serialize Maven; shared target directories and shared PostgreSQL test fixtures previously collided under parallel builds.

## Local setup and useful commands
Install Java25, Maven3.9+, PostgreSQL18, Node22+, pnpm11.25.0; .NET10 for adapter. Use repository env examples; do not invent credentials or disable authentication. Prior /tmp toolchains were ephemeral and are NOT part of this branch.

PowerShell examples after tools are installed:

```powershell
pnpm install --frozen-lockfile
pnpm check:rules
pnpm test:project-gates
pnpm inventory:parity
pnpm check:parity # expected to fail until real required parity is evidenced
$env:QUINCAI_PG_TEST_URL = 'jdbc:postgresql://127.0.0.1:5432/postgres'
$env:QUINCAI_PG_TEST_USER = 'your-isolated-test-admin'
Set-Location backend
mvn verify
```

CAUTION: existing PG integration harnesses use a local disposable trust-auth test server and empty test password; they create/drop unique databases and roles. Set up an isolated local test instance or improve the harness credential support first. Never point this at production. Without QUINCAI_PG_TEST_URL, integration tests skip: skipped tests are not a pass. Inspect Maven XML totals.

Then run web typecheck/tests/build, mobile typecheck/tests, generated SDK checks and .NET adapter checks using package/readme instructions. The Maven wrapper is Unix-only currently; use installed mvn on Windows or WSL. Do not infer release readiness from compilation.

## Mobile preview request still outstanding
User explicitly wants to see the actual app. Browser export attempt failed because react-native-web is missing. No emulator/adb was available in cloud. Mobile source has Workspace, Catalog, Sales, Queue and two details. SQLCipher/SecureStore need native support; do not fake authenticated data or silently downgrade encryption for a preview. On the computer use an Android emulator/device and an Expo development build, configure HTTPS Java API/OIDC, and verify genuine screens. Alternatively add a clearly limited browser preview with platform-appropriate storage boundaries and keep native authority disabled until verified. The previous Android Hermes export is not an installable APK.

## GitHub language percentage concern
Actual merged main tree had 203 .java files (471,813 bytes), 13 unchanged .py files (198,701 bytes), and 102 TS/TSX files. Public languages API omitted Java/TypeScript and returned Python78.9%; cause not verified. No .gitattributes overrides were found. Do not manipulate statistics to disguise incomplete migration. Backend/pom.xml and QuincaiSoftApplication.java verify Java25/SpringBoot4. Legacy Python is reference only; remove it from the active release only after reviewed parity/cutover.

## Immediate continuation prompt
Continue from this handover branch. Audit interrupted files, enforce AGENTS.md/ARCHITECTURE.md, finish and integrate wave-2 Java implementations, execute real PostgreSQL tests serially, wire web/mobile against actual APIs, and update evidence. Do not claim complete migration until the strict behavioral gate and canonical acceptance requirements are met. Preserve existing work and commit tested increments. First report the specific remaining blockers and open a real mobile app preview on this computer.
