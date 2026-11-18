# Platform operator workspace

Separate OIDC operator application for onboarding, company/owner configuration,
independent approval, empty standard-trial provisioning, subscription plans and
lifecycle, invoice/credit issuance, manual bank reconciliation and delivered-notice
review. All displayed records come from the platform API; no local demo data.

## Run

Use the repository-pinned Node/package manager and install workspace dependencies.
Copy `.env.example` to your local environment and configure the platform issuer,
client, API and Redis. Register `/auth/callback` at `PLATFORM_APP_URL` in the
operator IdP. Use HTTPS outside local development. The example scopes describe
supported operations; grant each operator only their assigned capabilities.
Operator enrollment and independent review are enforced by the backend.

Run `pnpm --filter @quincaisoft/platform-admin dev`. Test validation with
`node --experimental-strip-types --test apps/platform-admin/lib/*.test.mjs`
from the repository root, and run the package typecheck/build scripts.

## Operator flow

1. Request and independently approve a merchant from Onboarding.
2. Open its detail, configure the company and owner, verify signed owner proof,
   and independently approve the configuration.
3. Provision an empty standard-trial workspace and inspect actual IDs and setup
   results. The merchant IdP must issue fresh scoped owner credentials.
4. Follow Manage this subscription to review the current plan, capacity and dates.
5. Open Invoices & credits for immutable billing documents and linked corrections.
6. Record bank evidence for an invoice and have another operator inspect the
   original evidence before manual reconciliation. Reviewed future-period payments
   can activate only when the server confirms coverage and period eligibility.
7. Record already-delivered contractual notices and independently inspect evidence
   before approval. This app does not send notices or fabricate provider receipts.

Command forms retain an operation ID across retries, surface safe authorization,
conflict and validation failures, and require reload after success. Server sessions
retain access tokens in Redis, not browser storage; owner proof is submitted as a
password input and never logged/persisted by these UI handlers. Money is validated
and formatted using exact integer strings/BigInt.

## Acceptance prerequisites

Build and validation tests do not substitute for authenticated end-to-end tests.
Live acceptance needs the API/database, Redis, operator OIDC clients and distinct
maker/reviewer identities, merchant signed identity proof, and approved bank or
notice evidence. Check loading, denied permissions, stale versions, duplicate
operations, expiry, maker/reviewer separation, and restricted lifecycle states.
The operator UI supports English and French. Its server-selected language uses
an explicit preference cookie, then the browser language; switching preserves the
current page. Printed billing documents use the same language through authenticated
server proxy routes with private/no-store caching and restrictive content policies.
Full authenticated browser acceptance remains outstanding.
