# QuincaiSoft — canonical architecture migration

The authoritative specification is [ARCHITECTURE.md](ARCHITECTURE.md), version 2.0. The active migration targets Java 25, Spring Boot 4.x, Spring Modulith, Hibernate/JPA, PostgreSQL 18 and Next.js 16 with React/TypeScript. It does not use Python as the application backend.

## Status

Migration is in progress. The initial source includes the Java composition/security foundation, typed catalog/inventory/customer/sales/finance modules, PostgreSQL migrations with row-level security, and a Next.js business shell with catalog and sale routes. These are a bounded first migration slice, not completion of every module or workflow. See [the migration acceptance register](docs/migration-to-canonical.md).

The previous Python/SQLite service and static screens remain in `server/`, `v3/` and `v4/` as historical reference. They are not the production backend, and their 95 tests do not count as Java migration acceptance. Their prior status is preserved in `docs/legacy-v5.1-readme.md`.

## Build and configuration

- Java 25 and Maven wrapper: `cd backend && ./mvnw verify`.
- Web workspace: `pnpm install --frozen-lockfile`, then `pnpm build:web` or `pnpm dev`.
- Database: read `infrastructure/` and `backend/db-migrations/` before startup; migrator and runtime database roles are distinct.
- Configure the API's PostgreSQL credentials, OIDC issuer and API audience. Configure the web server using `apps/web-admin/.env.example`. Tokens stay server-side; Redis stores temporary web sessions, not business balances.
- Use a real OIDC provider issuing scoped claims and provision active database memberships. There is no production demo-password bypass.

## Executed migration checks

Java 25 Maven verify passes with 26 tests, zero failures/errors/skips, including the real PostgreSQL18 sale transaction test and Modulith boundary verification. PostgreSQL migrations pass 19 database assertions. Next.js production build, TypeScript check and two precision tests pass. Evidence is in `docs/migration-evidence/`. These checks cover only the implemented slice; they do not establish complete migration.

## Remaining architecture obligations

Complete module migration, full transactional document issuance, fiscal policy, purchase/return/cash/provider workflows, worker/outbox processing, reporting, mobile Expo offline queue, separate platform/storefront apps and the opt-in .NET 10 WPF bridge remain tracked obligations. Existing Windows standalone behavior must remain independent of cloud availability. MTN and Orange provider activation is not established by this migration.

No production deployment or release certification is claimed. Required fresh/upgrade migration, tenant-isolation, concurrency, financial, UI, security and integration evidence must pass before release. Main-branch acceptance remains subject to the canonical review gates.
