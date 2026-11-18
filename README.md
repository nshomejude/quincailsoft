# QuincaiSoft — canonical architecture migration

The authoritative specification is [ARCHITECTURE.md](ARCHITECTURE.md), version 2.0. The active migration targets Java 25, Spring Boot 4.x, Spring Modulith, Hibernate/JPA, PostgreSQL 18 and Next.js 16 with React/TypeScript. It does not use Python as the application backend.

## Status

Migration is in progress. Ten agent assignments have delivered connected Java domain slices for catalog, inventory, sales, quotations, returns, documents, procurement, cash/deposits, expenses, period decisions, identity, audit workers, provider intents, reporting, sync intake and storefront publication. The repository includes separate merchant, platform and storefront Next.js applications, an Expo mobile client and an opt-in .NET 10 SQLite sync adapter. These implementations are partial migration slices, not completion of every module or workflow.

The [item-level coverage register](docs/migration/canonical-migration-coverage.json) enumerates the inherited contracts for 40 modules, 152 screens, 656 actions, 445 tabs, 1,140 screen field placements, 76 reports and 26 documents. Source presence does not count as individual workflow acceptance. The [ten-agent register](docs/migration/ten-agent-register.json) and each `docs/migration/m01.md` through `m10.md` identify delivered behavior and remaining gaps. ARCHITECTURE.md overrides earlier UI contract assumptions.

The previous Python/SQLite service and static screens remain in `server/`, `v3/` and `v4/` as historical reference. They are not the production backend, and their 95 tests do not count as Java migration acceptance. Their prior status is preserved in `docs/legacy-v5.1-readme.md`.

## Build and configuration

- Java 25 and Maven wrapper: `cd backend && ./mvnw verify`.
- Web workspace: `pnpm install --frozen-lockfile`, then `pnpm build:web` or `pnpm dev`.
- Database: read `infrastructure/` and `backend/db-migrations/` before startup; migrator and runtime database roles are distinct.
- Configure the API's PostgreSQL credentials, OIDC issuer and API audience. Configure the web server using `apps/web-admin/.env.example`. Tokens stay server-side; Redis stores temporary web sessions, not business balances.
- Use a real OIDC provider issuing scoped claims and provision active database memberships. There is no production demo-password bypass.

## Executed migration checks

Current results and their scope are recorded in `docs/migration-evidence/summary.json`. Integration tests use real PostgreSQL 18 with non-owner runtime roles; the application test exercises signed JWT validation and database membership checks over HTTP. Provider transport tests use simulated responses. No live payment or native-device certification is implied by a passing build.

## Remaining architecture obligations

Full screen/action/field/report/document parity, fiscal policies, advanced inventory and fulfillment, full subscription/provisioning lifecycle, billing, notifications, operational imports/exports and release acceptance remain incomplete. Mobile authorized offline economic posting is disabled until signed quotas and native-device evidence are implemented. The desktop adapter passes its own checks, but integration into the original WPF host remains pending; the recovered R02 baseline has dependency vulnerability audit blockers. Existing Windows standalone behavior remains independent of cloud availability.

MTN has a durable Java intent/reconciliation adapter, but live credentials, acceptance and provider verification remain required. Orange is explicitly unavailable pending its contracted integration; it is not marked integrated. Company-wide period close currently fails closed for multi-branch companies until a complete cross-branch close protocol exists.

No production deployment or release certification is claimed. Required fresh/upgrade migration, tenant-isolation, concurrency, financial, UI, security and integration evidence must pass before release. Main-branch acceptance remains subject to the canonical review gates.
