# Java migration API contract

`openapi.json` is OpenAPI 3.1 for the implemented migration slice, with `/api/v1` as its server base. It is not the full canonical endpoint register from architecture §7.1.

The `/me` endpoint and `/session` compatibility alias share a handler.

Source adapters: backend api-server SessionController; catalog CatalogController; sales SalesController. Business scope is JWT-derived and checked against current database membership. No caller-selected tenant/company header grants access.

Amounts and stock quantities are decimal strings to preserve exact long values across JavaScript/.NET clients. Sales versions currently use JSON integer and catalog versions use strings; uniform version wire representation remains a compatibility task before public API approval. Collection reads are currently bounded first pages without cursor continuation; full §7.4 pagination remains pending.

Run `node packages/api-contracts/validate.mjs` from repository root for local reference, header and exact-wire-value checks. This is a structural check, not a substitute for a full OpenAPI validator, generated client build, response-schema integration tests or contract compatibility diff.

No generated TypeScript SDK is claimed yet. The initial Next.js API adapter is hand-written and must be replaced or checked against generated clients before the API release gate closes. MFA/PKCE live-provider authentication, rate limits, error-envelope uniformity, immutable replay responses, operation-status lookup and every omitted endpoint family remain subject to the migration acceptance register.

Sales authorization accepts the configured permission authority or its OAuth `SCOPE_` equivalent as implemented by the sales adapter; verify this against the selected IdP before claiming login-to-command end-to-end completion. The web OIDC scope list must not be mistaken for a provisioned permission grant.
