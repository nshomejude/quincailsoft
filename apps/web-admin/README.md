# QuincaiSoft web admin

Next.js 16 / React / strict TypeScript client for the Java Spring API. This application does not execute or proxy the legacy Python backend. See `../../docs/migration/m05.md` for delivered workflow coverage and remaining acceptance gaps.

## Application and security

The unified shell exposes role-aware navigation from the current Java `/me` response. Authorization and tenant/company/branch isolation remain server responsibilities. OIDC Authorization Code + PKCE uses state/nonce validation and opaque HttpOnly Redis-backed sessions; OAuth tokens never enter browser storage. Sign-in, MFA, password recovery and identity provisioning use the configured identity provider. Refresh-token rotation and deployment-level identity acceptance remain outstanding.

Business forms use React Hook Form and Zod where schema-driven, TanStack Query mutations/cache and exact decimal-string money/quantity payloads. Multiline sales/purchases select scoped customers, suppliers, products and warehouses. A finite authenticated BFF allowlist forwards only implemented Java paths, checks mutation Origin, and adds operation idempotency and request correlation headers. Stable operation IDs survive retries of the same payload within a mounted form. Backend validation/errors and optimistic versions are preserved; no optimistic financial success is shown.

English/French selection translates the application shell, navigation and loading view using shared dictionaries. Domain forms and reports are not yet fully translated. Existing palette tokens and responsive CSS are retained; full Tailwind/shadcn design-system migration remains open.

## Setup

Use the root pnpm workspace. Configure `.env.example` values through deployment secrets: Java API URL, OIDC issuer/client/scopes, APP_URL and Redis URL. Register `<APP_URL>/auth/callback` at the provider. Provision tenant/company/branch/actor claims, audience, authorities and membership epoch consistent with the Java resource server. Use HTTPS; `OIDC_ALLOW_HTTP=true` applies only to development.

- `pnpm --filter @quincaisoft/web-admin dev` listens on 3100.
- `pnpm --filter @quincaisoft/web-admin typecheck`
- `pnpm --filter @quincaisoft/web-admin test`
- `pnpm --filter @quincaisoft/web-admin build`

Build/typecheck and five focused exact-quantity/BFF-boundary tests pass. These do not certify browser workflows, real OIDC provisioning, merchant payments or production acceptance. MTN collection requires real configured provider credentials and deployment verification. Orange's unavailable provider state is displayed; no Orange success is fabricated.
