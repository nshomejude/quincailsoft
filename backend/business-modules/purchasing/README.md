# Assignment 02 — procurement and suppliers

Canonical references: ARCHITECTURE §§2.2–2.3,3,7.2,8.4,32,36–38. Java25/Boot4, typed Hibernate/JPA aggregates and PostgreSQL18; no Python proxy.

## Migrated and executed

- Scoped supplier create/list/detail and active-supplier eligibility; exact repeated creation returns its original result.
- Purchase draft with authoritative active catalog products, original supplier/unit-cost/quantity snapshots, exact FCFA totals and no stock side effect.
- Submit and independent maker/checker approval with revision and reason checks.
- Approved partial goods receipt; remaining order quantities serialize under order lock, no overreceipt, warehouse stock increments and Dr MERCHANT_INVENTORY / Cr GOODS_RECEIVED_NOT_INVOICED journal commit atomically. Order closes only at full received quantity.
- Exact receipt supplier invoice with original supplier match, unique supplier reference and single-invoice-per-receipt guard. Dr GRNI / Cr ACCOUNTS_PAYABLE; no fabricated payment.
- Bounded uninvoiced supplier physical returns: original receipt line eligibility, aggregate returned quantity guard, original value equals current weighted issue value, stock issue and Dr GRNI / Cr MERCHANT_INVENTORY. Invoiced returns and mismatched valuation fail explicitly.
- Canonical tenant/operation idempotency with immutable original DTO response, current company/branch comparison, and immutable outbox evidence including actor/request material. Header/body operation IDs must agree.
- Typed V5 migration adds composite foreign keys, forced RLS, receipt/invoice/return fact immutability, and receipt/return line transaction sealing.

Public module interfaces stay in `.api`; commands in `.application`, JPA entities and operation persistence in `.infrastructure`, HTTP permissions in `.web`. Stock/finance/catalog/supplier calls cross module boundaries only through public APIs.

## HTTP

`/api/v1/suppliers`: GET list, GET /{id}, POST create.

`/api/v1/purchases`: GET list, GET /{id}, POST draft, POST /{id}/submit, /approve, /receipts; GET /{id}/receipts; POST /receipts/{id}/invoice; GET /invoices; GET/POST /receipts/{id}/returns.

Mutations require UUID Idempotency-Key and X-Request-ID. Scope comes from verified identity. Permissions are purchasing:read, purchasing:write, purchasing:approve, purchasing:receive, purchasing:invoice and purchasing:return (or corresponding OAuth SCOPE_ authorities). Money and quantities use decimal strings; quantity units are thousandths of the SKU base unit.

## Test evidence

Five `PurchaseRulesTest` cases and one `PostgresPurchasingIntegrationTest` pass with zero skips against real PostgreSQL18.7 and a non-owner, NOBYPASSRLS runtime role. Integration exercises supplier→draft→submit→independent approval→partial receipt→invoice; immutable replay, overreceipt, self-approval, duplicate invoice, cross-tenant concealment, closed-period full rollback, supplier return replay/eligibility, invoiced-return rejection, net-invoice guard and balanced stock/finance results. Root combined verification remains authoritative for the final integrated build.

Reproduce with Java25, a disposable PostgreSQL18 admin endpoint and environment QUINCAI_PG_TEST_URL/QUINCAI_PG_TEST_USER, then `./mvnw -pl :purchasing -am test`. The test creates/drops a random fixture database and runtime role; no merchant data is used. Without the environment the database test is explicitly skipped.

## Remaining work — not complete module certification

Supplier edits/activation lifecycle, requisitions/RFQ, PO amendment/rejection/cancellation, tax-bearing invoices, landed-cost apportionment, quarantine/lot receipt inspection, supplier advances/payment allocation, AP aging, partial invoice tolerances, invoiced supplier credit notes/refunds, returned-receipt net invoicing, cross-company trade, immutable numbered purchasing printables, full request/device evidence, cursor pagination and complete reports/screens remain required. User-controlled cost inputs require the configured purchasing permission; price approval thresholds remain pending. No unsupported state is represented as posted success.
