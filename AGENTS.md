# Mandatory QuincaiSoft engineering rules

These rules apply to every human or agent changing this repository, including future sessions. Read this file before editing. User instructions remain authoritative.

## Required preflight

1. Read `ARCHITECTURE.md` sections relevant to the assignment. Sections 31–46 override abbreviated earlier examples. Do not substitute another stack or reduce the specification.
2. Read `docs/migration-evidence/summary.json`, `docs/migration/canonical-migration-coverage.json`, and `docs/migration/python-java-parity.json` when present. Read the actual legacy implementation and its tests for migration work; screenshots and counts are not behavioral specifications.
3. Identify owned files, dependencies, applicable specification sections, database effects, authorization, and acceptance cases before edits. Record these in the assignment evidence. Coordinate shared-file changes with the root agent.

## Architecture is binding

- Production backend: Java 25, Spring Boot 4, Spring Modulith, DDD modular monolith and Clean Architecture. Hibernate/JPA owns aggregate writes; jOOQ may implement demanding reads through approved projections. PostgreSQL 18, Flyway, typed tables, composite tenant/company references and forced RLS are required.
- Next.js/TypeScript web applications; Expo/React Native mobile; .NET 10 WPF/SQLite desktop retained. Python/SQLite service is migration reference only. Never add a Python production route or a hidden Python bridge to claim Java migration.
- Do not replace typed business aggregates with a generic entity/JSON CRUD engine. Cross-module calls use public APIs; never write another module's ledger tables. Financial/stock posting must be atomic, balanced and append-only.
- XAF is exact checked integer arithmetic, serialized as decimal strings. Quantities use the prescribed checked integer scale. No floating-point monetary posting.
- External OIDC, active membership and authorization epoch validation, scoped RBAC/ABAC, independent approvals where required, and concealed cross-tenant access are mandatory. No production demo-password or authentication bypass.
- Economic commands require stable operation identity, request correlation, durable same-transaction replay, conflicting-key rejection and optimistic/pessimistic concurrency controls. Include audit/outbox facts and immutable original document snapshots where specified.
- Preserve standalone desktop behavior. Offline economic authority needs signed device grants and fencing; queued evidence is not authorization. Unknown external payment outcomes cannot become successful payments. Never fabricate provider acceptance.

## Migration and evidence

- Inventory every legacy route, command branch and meaningful tested behavior. Map each to actual Java source and executed acceptance evidence, or explicitly identify the specification-mandated replacement. Do not label unimplemented behavior migrated.
- Preserve required behavior, not legacy security defects. Document intentional replacements (such as local passwords replaced by OIDC) with specification references and tests. Mark externally blocked acceptance separately from unfinished code.
- A screen, method, generic form, source file, build, or unrelated passing test is not workflow completion. Keep screen/action/field/report/template coverage and Python-to-Java behavioral parity distinct.
- Do not delete the reference implementation until parity and cutover gates pass. Production launch configuration must not depend on it.
- Every migration assignment must return changed paths, specification sections, legacy behaviors covered, executed tests, and exact remaining gaps. No fabricated percentages or blanket completion claims.

## Verification and integration

- Run meaningful tests for changed invariants, tenant/company/branch isolation, replay/conflicting keys, concurrent writes, failure rollback and permission denials. Use real PostgreSQL 18 for database acceptance; H2/SQLite is not a substitute.
- In this shared agent workspace, the root agent alone runs Maven and edits the reactor, shared lockfile, common API security configuration, and final integration evidence. Agents may author tests; request serialized execution. Do not race shared target directories or database fixtures.
- Allocate unique migration numbers with the root. Never edit a migration already accepted on the shared branch: add a forward migration. Preserve foreign keys, triggers, RLS and constraints when correcting tests.
- Run `node scripts/check-project-rules.mjs` when available. A passing rule check is necessary, not sufficient for release. The full parity gate must remain failing while required behaviors are unresolved.
- `main` and releases require the specification's complete review/acceptance gates. Keep migration changes on the migration branch while gaps remain. Push authorized checkpoints with accurate status; never claim release certification from a draft pull request.

## Prohibited shortcuts

No weakened tests to obtain green builds, swallowed integration errors, disabled financial constraints, fake payment success, fabricated screenshots, empty handlers represented as implementations, or automatic completion based on route counts. Never silently change the architecture to make a tool limitation disappear.

## QuincaiSoft branding

- Read [QuincaiSoft brand requirements](docs/QUINCAISOFT_BRAND_REQUIREMENTS.md) before UI changes. Use the original QuincaiSoft wordmark at assets/logo.png and preserve its artwork and proportions. The sky-blue web sidebar uses a white rendering of that same wordmark with the rectangular background removed.
- Reuse the architecture tokens for navy, sky blue, white canvas, surface, text, muted text and borders across sign-in, navigation, actions, forms, details, reports and printable branding. Preserve semantic warning, error and success colors. Use accessible contrast and readable operational surfaces.
- Preserve UTF-8 English and French copy. Verify the running sign-in, dashboard, one detail view and one report or document at desktop and narrow widths.
