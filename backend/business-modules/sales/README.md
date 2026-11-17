# Sales migration boundary

Canonical references: architecture §§2.2–2.3, 3.2, 7.2, 8, 32, 36, 38.

This Java 25 module implements typed PostgreSQL/JPA sale drafts and immediate zero-tax credit-sale recognition. Draft prices come from the catalog API. Positive quantity milli-units and integral XAF totals are exact and overflow checked; unsupported fractional-FCFA results fail explicitly. A posted sale issues stock through the inventory public API, records a balanced finance journal, seals its lines, saves a durable tenant operation identity and writes a SalePosted outbox fact within the same transaction. The customer API checks active membership in the legal company.

REST reads/draft/post require scoped OIDC permissions. Costs are omitted from client DTOs. Idempotency-Key and X-Request-ID headers are mandatory; request operation IDs must match. Replay validates current scope and returns its saved original DTO even after the sale changes. Expired response payloads return a conflict identifying the already committed sale; historical reconstruction after expiry remains unfinished, with no second posting permitted.

## Verification

`SaleRulesTest` tests exact fractional-unit pricing, unsupported fractional FCFA, signed/exponent quantity rejection and overflow. `PostgresSaleIntegrationTest` uses real PostgreSQL 18 under a non-owner NOBYPASSRLS role, fresh migrations and synthetic scoped fixtures. It exercises JPA schema validation, stock/journal/outbox commit, immutable draft/post replay and a closed-period failure after attempted stock mutation with complete rollback. It requires `QUINCAI_PG_TEST_URL` and `QUINCAI_PG_TEST_USER`; otherwise JUnit explicitly skips it. The repository evidence distinguishes executed tests from skips.

## Remaining migration work

This is not the complete POS/sales module. Atomic numbered document issuance and immutable print snapshots are absent, so this path is not pilot-ready financial issuance. Credit-limit policy, tenders, taxes, discounts, held carts, draft editing, customer allocations, cash shifts, return credits, fiscal policies and the complete workflow inventory remain to migrate. Database integration is not live merchant/provider acceptance. No Python service is invoked.
