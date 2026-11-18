# Source-backed OpenAPI 3.1 contract

Run `node packages/api-contracts/generate-source-contract.mjs`, then `node packages/api-contracts/validate.mjs` from the repository root. The generator reads real annotated Java controllers and record definitions. Full API paths include `/api/v1`, public-store routes use `/api/public`, and `/admin` paths belong to the isolated platform deployment. Do not route platform tokens through the merchant API.

`endpoint-inventory.json` preserves each source controller/method, declared type, authorization expression and effective command headers. `X-Request-ID` inheritance from the actual `RequestIdentityFilter` is included. Missing Idempotency-Key declarations are reported for review; replay behavior must be checked separately. Java dynamic Object responses are deliberately unresolved. Validation constraints are only included where reviewed; Java record declarations alone do not establish required/nullability/business validation. This is a source-shape contract, not proof of complete workflows or a substitute for runtime schema/consumer tests.

`generate-coverage.mjs` enumerates every entry in `v3/contracts.json` into `docs/migration/canonical-migration-coverage.json`. Related source evidence is conservative and never treated as complete acceptance. Original UI field provenance remains explicit. Regenerate after source changes.

Reporting exposes 20 bounded query codes extracted from `DefaultReportingService` into the contract, including customer custody, approved recalls and supplier payables. Read-only projections preserve tenant/company/branch scope and exact monetary strings. Exports are paged and do not represent an all-record export job. The generated response schema does not certify all canonical analytical reports.
