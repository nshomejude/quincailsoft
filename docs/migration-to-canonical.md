# Migration to the prescribed QuincaiSoft architecture

> Historical first-checkpoint register. The binding architecture table and non-substitution rules below still apply. The initial implementation status and blocker lists are superseded by [the ten-agent register](migration/ten-agent-register.json), the individual `migration/m01.md`–`m10.md` reports, [item coverage](migration/canonical-migration-coverage.json) and [current verification evidence](migration-evidence/summary.json). Do not interpret this checkpoint's “not migrated” rows as the current implementation state.

Authority: [ARCHITECTURE.md](../ARCHITECTURE.md), version 2.0, 17 November 2026. Sections 31–46 take precedence over abbreviated earlier examples (§31.1). This register records migration work, not a new architecture decision or a production-completion claim.

## Architecture that remains binding

| Area | Required architecture | Governing sections |
|---|---|---|
| Backend | Java 25 LTS; Spring Boot 4.x; compatible Spring Modulith; Maven wrapper and pinned BOM | ADR001–003, §2.3, §45 |
| Structure | DDD modular monolith, Clean Architecture; api/domain/application/infrastructure/web per bounded module; cross-module access through public APIs | §2.2, §4.1 |
| Persistence | PostgreSQL 18; Hibernate/JPA transactional aggregate writers; jOOQ demanding reads; Flyway; tenant/company composite keys and forced RLS | ADR004–005, §2.3, §5.5, §36 |
| Web | Next.js 16, React, strict TypeScript; separate business, platform-admin and merchant-storefront applications | ADR006, §11 |
| Mobile | React Native/Expo candidate SDK 56, compatibility evidence required; encrypted/protected SQLite and one durable offline queue | ADR007, §§9,12,33,45 |
| Windows | Existing .NET 10 WPF + SQLite; separate opt-in .NET sync adapter, no mandatory cloud dependency | ADR016, §10, §35 |
| APIs/security | REST JSON, OpenAPI 3.1; external OIDC, PKCE, MFA, verified membership plus scoped RBAC/ABAC | ADR008–009, §§5,7 |
| Transactions | Exact integral XAF; BIGINT milli-base quantities; append-only financial/stock facts; transactional outbox/inbox and tenant-operation idempotency | ADR010–011, §§3,32,37,38 |
| Supporting services | Redis only derived/ephemeral state; private S3-compatible objects; Linux containers and workers | ADR012–015, §17 |

## Migration acceptance status

The Python/SQLite code in `server/` and its vanilla-JavaScript UI remain legacy reference artifacts. Their 95 passing tests are **not** Java/PostgreSQL migration evidence. New code must not proxy business posting to Python or invoke Python as the authoritative backend.

| Workstream | Current scope | Acceptance still required |
|---|---|---|
| Java foundation | Java25/Boot4/Modulith Maven composition, shared value types, resource-server boundary | Executed compile and unit/module tests; supported pinned dependency matrix; validated issuer/audience, active DB membership and negative scope tests |
| PostgreSQL foundation | Typed schema, Flyway migrations, RLS and ledger/idempotency/outbox foundations | PostgreSQL18 fresh/upgrade execution, non-owner runtime role, cross-tenant/company negative tests, concurrent invariant tests |
| Catalog/inventory | Initial typed catalog and stock issue/receipt services | Full ownership/custody/reservation/delegated dimensions; acquisition-cost evidence; counts/transfers/returns/traceability and concurrency matrix |
| Sales/finance | Initial draft and bounded posting coordination | Customer subledger, period guards, immutable pricing/tax/cost/document snapshots, ledger balance, posting replay, stock atomicity and complete tender/credit controls |
| Next.js | Initial business shell and typed catalog/sales integration | Every canonical screen/tab/dialog/field/report/document migrated; accessibility/responsive EN/FR checks; real OIDC session flow and full-role browser matrix |
| API contract | OpenAPI for source-implemented endpoints only | Contract lint, generated SDK and consumer compatibility, canonical headers/errors, durable operation status endpoint |
| Platform/storefront | Not migrated | Separate apps and control-plane security, commerce stock/payment compensation, tenant lifecycle and entitlements |
| Mobile/Windows | Not migrated in this iteration | Actual native builds; signed offline grants/epochs/recovery; adapter/cutover and device field tests |
| MTN/Orange | Python adapter behavior is reference only; no Java migration or live-provider certification claimed | Java adapters, merchant contracts/configuration, sandbox acceptance, exact settlement/refund/idempotency/reconciliation evidence |
| Full release | Not complete; no P01–P14 phase certified by this document | All fourteen source/API/financial/security/UI/offline/recovery/field acceptance gates |

## Non-substitution checks

1. A Boot main class is not a migrated backend. Domain handlers, typed persistence, authorization and transactional effects must run in Java against PostgreSQL.
2. A new frontend route is not the migrated canonical screen. Its fields, commands, details, reports and printables must be tracked individually.
3. JWT validity is not active tenant/company membership. Claims and current membership must agree; headers cannot grant scope.
4. Generic JSON record storage must not replace typed aggregates and database constraints.
5. JDBC may establish transaction-local PostgreSQL RLS context; it is not an unapproved replacement for the specified Hibernate/JPA aggregate writer.
6. No module directly writes another module's ledger table. Inventory owns stock; finance owns journal posting; business coordinators call public APIs in the same transaction.
7. POST commands committing money/stock require `Idempotency-Key`, `X-Request-ID`, version checks where applicable, and same-transaction request/result persistence (§7.2).
8. Drafts, pending provider states and unverified external outcomes must not be represented as authoritative posted sales.
9. XAF values use integral decimal strings on the wire and overflow-checked `long` internally. Negative balances cannot be silently capped; semantic credits/liabilities must be explicit.
10. Historical Python tests and render counts do not count toward the Java migration acceptance gate.

## Evidence policy

Each implemented claim needs a path plus an executed test artifact. A build blocked by missing Java25, Maven dependencies, PostgreSQL18, container runtime or an identity provider stays `NOT_EXECUTED`; do not downgrade versions or substitute H2/SQLite to report a production-database pass. Missing external merchant credentials do not justify fake payment confirmation.

This first migration slice is not authorization to remove canonical modules or silently reduce the architecture. All unimplemented canonical work remains required.

## Review findings and current boundaries

Source review corrected the initial JDBC aggregate-writing direction to Hibernate/JPA; catalog/inventory now expose named public APIs with infrastructure implementations. Command replay now stores the complete original sale DTO rather than looking up and returning its later state. The sales HTTP adapter requires matching operation/idempotency IDs and request IDs; revision conflicts use 412. The resource-server adapter checks issuer/audience and current scoped membership/authorization epoch, and the error envelope uses the canonical field array. These are source findings; parent integration evidence establishes executed status separately.

Remaining release blockers include: atomic original-document series/snapshot in sale posting; complete customer subledger and credit limits; nonzero tax, tenders/deposits/refunds; complete ledger dimensions and stock source approvals; persisted complete audit/request/occurrence evidence; tenant commit-clock/change-feed wiring; actual outbox workers and delivery/recovery; cursor pagination; generated SDK/contract tests; source coverage for all modules and canonical screens; native clients; production identity/MFA and external provider acceptance. No complete migration or phase certification is claimed.

The migration is prepared on a **draft migration branch**, not accepted as the replacement production baseline. Passing a bounded Java build or Next.js build does not close these gates. PostgreSQL migration/RLS execution and Java/PostgreSQL service integration tests must be distinguished from static SQL inspection and unit tests.

Local service defaults are Next.js on 3100 and Java API on 8080. The web example points to an external OIDC issuer on 8180; it is configuration, not evidence of a provisioned identity provider. Browser redirects, server-side discovery and Java issuer validation must use the same externally valid issuer. Development HTTP is explicit and disabled by default in the web authentication configuration.

## Executed first migration checkpoint

The integrated Java25 Maven verify passes with 26 tests and zero skips, including actual PostgreSQL18 JPA commit/replay/rollback and Modulith boundaries. Nineteen real PostgreSQL18 schema/isolation assertions pass separately. Next.js build, TypeScript checks, two precision tests and structural OpenAPI checks pass. See `migration-evidence/summary.json` and execution logs. Upgrade history, concurrent load, complete OIDC/browser integration and all remaining release gates above are still open.
