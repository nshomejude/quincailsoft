# QuincaiSoft web-admin migration

This is the actual Next.js 16 / React / strict TypeScript application required by ADR006. It calls the Java API only. It does not use or proxy the legacy Python backend. This first migrated slice is **not the entire 152-screen platform**.

## Implemented routes

- `/`: unified shell, quick actions and API-derived scoped summary; no invented business figures.
- `/catalog`: typed product list from `GET /api/v1/products`.
- `/sales`: typed list; `/sales/new`: atomic one-line draft form; `/sales/[id]`: details and authorised zero-tax credit-sale posting.
- `/auth/login`, `/auth/callback`, `/auth/logout`: OIDC Authorization Code + PKCE with state/nonce validation through openid-client. Opaque HttpOnly session cookie; access token remains in Redis and server memory. Expiry requires fresh sign-in; no refresh-token flow is claimed.

API amounts and milli-quantities are exact strings. The browser never chooses authoritative catalogue prices. Java enforces permissions and tenant/company/branch scope; hiding a UI button is not authorization. Form mutations check Origin and use Next server actions, stable operation IDs during an attempt and explicit error display. Redis stores ephemeral sessions only, not stock or finance.

## Setup

Use the root pnpm workspace. Copy `.env.example` to a deployment-secret environment configuration and set the Java API URL, OIDC issuer/client, scopes and Redis URL. Register the exact callback `<APP_URL>/auth/callback` with the provider. Provision the provider's audience and tenant/company/branch claims to match the Java resource server. Use HTTPS in deployment. `OIDC_ALLOW_HTTP=true` is permitted only in development and never in production.

`pnpm --filter @quincaisoft/web-admin dev` listens on 3100. `pnpm --filter @quincaisoft/web-admin build` creates the Next production artifact. Set `NEXT_TELEMETRY_DISABLED=1` if telemetry is not desired.

## Executed evidence

- Next production build: PASS, TypeScript compilation included.
- Two exact-quantity tests: PASS (`pnpm --filter @quincaisoft/web-admin test`).
- OIDC merchant/provider sign-in and Java/PostgreSQL end-to-end mutation require deployed services and have not been certified by this client build.

Pinned direct versions: Next16.4.6, React/ReactDOM19.3.1, TypeScript5.9.3, openid-client6.9.0, Redis client5.11.0. Official framework references: https://nextjs.org/docs/app/guides/upgrading/version-16 and https://nextjs.org/support-policy ; package versions were resolved from npm on18Nov2026. Root pnpm lock records exact transitive dependencies.

## Remaining frontend migration

The catalogue currently has no edit/detail page. Draft entry currently accepts one product and requires a warehouse UUID; a scoped warehouse picker, multiline editing, customer selection, tender capture, reports, documents and the rest of the old screens remain outstanding. Posting supports only the Java module's bounded zero-tax immediate credit-sale flow. No payment collection is implied.

Platform-admin/storefront are separate future Next apps; mobile and Windows remain separate specified deliverables. EN/FR dictionaries and palette tokens are started under `packages/`, but this initial UI is English and does not yet implement full bilingual routing. TanStack Query, form-library/Zod integration, generated OpenAPI client and comprehensive accessibility/device/browser evidence remain open. No OIDC MFA/role provisioning or password reset is implemented locally; these are provider responsibilities with deployment acceptance still required.
