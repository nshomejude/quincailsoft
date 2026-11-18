# Generated TypeScript contract

`schema.d.ts` is real output from pinned `openapi-typescript@7.10.1`, using the source-derived OpenAPI 3.1 contract. Never edit it manually. `index.ts` is an explicitly handwritten runtime adapter around `openapi-fetch@0.15.0`; it does not invent generated models.

Regenerate from repository root: `pnpm --filter @quincaisoft/generated-ts-sdk generate`. Validate using `pnpm --filter @quincaisoft/generated-ts-sdk typecheck`. The source extraction script inventories controller declarations and Java records, not all validation and business invariants. Unresolved dynamic `Object` responses remain `unknown`; generated types do not prove runtime completeness. Consumers are not all migrated to this SDK yet.

Use server-side access tokens from the actual OIDC session, choose the appropriate business or isolated platform base URL, and provide command UUID headers. Exact money/quantity wire strings must not be converted to JavaScript Number. The caller owns request retries; reuse an operation identity only for the same intended command.
