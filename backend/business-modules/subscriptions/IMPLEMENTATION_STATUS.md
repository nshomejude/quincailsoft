# Subscription billing implementation checkpoint

Specification: ARCHITECTURE §§5.4, 8.10, 24.1–24.2 and N02/N11. This is a new SaaS capability, not a replacement for merchant payment-provider or POS settlement logic. Parent integration lead runs and records executable evidence; no test pass is claimed by this document.

## Implemented boundary

- `POST /admin/billing/invoices` issues a typed, immutable subscription invoice or a credit note referencing an original invoice. Integral XAF subtotal, discount and tax reconcile using checked arithmetic. Issuer, customer, jurisdiction, description and billing-period snapshots are retained.
- Platform operations require a signed scope and a currently enrolled active operator. Credit corrections additionally require `platform:billing:credit` and a different operator from the original invoice issuer. Replays recheck current authority and enrollment.
- Invoice and durable billing event are persisted together through JPA; an event failure rolls back invoice creation. Global operation locking rejects changed payloads/actors and concurrent repeated requests retain one original receipt. Independent original-invoice locking prevents concurrent credits exceeding original net or tax.
- Platform invoice listing requires `platform:billing:read`. Merchant listing requires `billing:read` (raw or SCOPE authority), matching authenticated actor, active owner/finance membership in the current tenant/company/branch, and the current authorization epoch. Historical invoice reads have no billing-expiry gate.
- Forward migration V25 uses tenant composite invoice references, forced RLS, actor-attributed insert-only runtime grants and immutability triggers. A PostgreSQL trigger independently enforces original-issuer separation, identity snapshot matching and serialized net/tax credit conservation. No merchant journal, AR/AP, sale, tender or cash record is changed.
- Restored the previously published `SubscriptionRestrictionPolicy` and its tests. It remains a pure policy component, not wired enforcement or proof of notice delivery.

## Acceptance authored

`PostgresSubscriptionBillingTest` exercises real PostgreSQL invoice creation, exact totals, unchanged replay, changed-payload and other-actor conflict, separate credit operator, net/tax credit limits, overflow, RLS, immutable facts, atomic rollback, six concurrent same-operation requests, competing credits, current role and epoch, revoked platform replay and zero merchant economic writes. `SubscriptionRestrictionPolicyTest` covers the existing billing-only policy.

## Not complete

- Invoice numbers are unique `SUB-UUID` / `SUB-CN-UUID` platform references. Jurisdiction-specific sequential fiscal numbering, numbering audits, legally reviewed invoice templates and tax calculation rules are not implemented or certified. The caller supplies tax explicitly; these fields do not establish tax compliance.
- No payment receipt, subscription-fee refund settlement, provider webhook acceptance or live subscription payment integration is implemented. A credit note is an accounting correction document, not a refund-success assertion.
- Plan catalog, effective-dated plan changes, entitlement provisioning, trial lifecycle, quota/usage meters, proration calculation and restrictions backed by persisted delivery/review evidence remain incomplete. The existing commerce entitlement gate is unchanged.
- Durable billing events are not a verified dispatch worker or delivered billing notices. Notification retries/delivery proofs remain outstanding.
- Listing is bounded to the latest 100 invoices; cursor pagination, full historical export, downloads/print templates, frontend integration and browser acceptance remain outstanding.
- Full HTTP issuer/audience-chain verification and final integration remain with the shared API integration tests; these focused service/database tests cannot certify production readiness.
