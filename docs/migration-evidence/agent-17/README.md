# Assignment 17 — current subscription disclosure

Base: handover after PR14. Owned files: `DefaultSubscriptionLifecycleService`, `PostgresSubscriptionLifecycleTest`, and this evidence. No migration or API schema change; V57 unused.

Preflight read AGENTS, architecture §§24.1–24.3, 31–46 scope/security requirements including §44.3, migration summary, canonical coverage, Python parity register, actual legacy `server/policy.py` role/read/scope definitions and legacy subscription references. SaaS billing is a new cloud capability (§23 migration direction), not a claimed one-to-one replacement for a Python subscription engine. No parity rows are marked accepted.

Finding: `current()` read subscription plan and limits without revalidating current membership, epoch, authenticated identity or billing capability. `printReceipt(..., false, ...)` validated token shape/capability and membership but omitted authenticated status. Both now use one merchant-read boundary requiring authenticated JWT identity equal to scoped actor, billing:read, and active current-epoch owner/finance membership. Platform commands and authoritative accounting/payment state are unchanged. Read validation never fabricates reconciliation or mutates entitlements.

Acceptance authored in `PostgresSubscriptionLifecycleTest.trialReviewGraceAndManualPaymentRemainAuditedAndSeparate`: valid current snapshot, stale epoch, missing branch, actor mismatch, missing read capability, unauthenticated JWT and inactive membership denial. Existing trial, grace, independent payment review, exact coverage, activation, billing CSV pagination and immutable evidence checks remain.

Root must execute `-pl business-modules/subscriptions -am -Dtest=PostgresSubscriptionLifecycleTest,PostgresSubscriptionBillingTest,PostgresSubscriptionQuotaTest -Dsurefire.failIfNoSpecifiedTests=false test` using PostgreSQL18. Agent did not run Maven. Files frozen for root execution.

Remaining: issuer-backed privileged step-up, live payment-provider acceptance, full customer data portability/retention, tenant legal/commercial release review and all unresolved parity rows are not certified by this increment. Subscription lifecycle reads remain available to currently authorized billing users despite commercial expiry; no historical-data lockout is introduced.
