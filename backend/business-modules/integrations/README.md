# Mobile-money integrations

MTN Java transport and durable intent/reconciliation state are implemented. Credentials are server-side. Pending/unknown responses do not settle money. Stable references survive retries; sandbox verification cannot enter the live XAF ledger. Real provider activation and acceptance remain unverified.

Orange Money is explicitly unavailable pending the subscribed Cameroon merchant transaction contract. No fake adapter or manual mobile-money completion is exposed.

See `docs/migration/m03.md` for environment configuration, verified source references, exact boundaries and acceptance gaps. `ProviderPostgresTest` exercises durable states and accounting retry against PostgreSQL18 with a simulated provider only. It does not make real payment requests.

Tenant security suspension also fences provider work, independently of subscription billing restrictions. `reconcile` deliberately is not transaction-wrapped by `TenantMutation`: it checks `TenantLifecyclePolicy.requireNewMutation` inside each state-changing transaction, again immediately before external dispatch, and before the ledger stage. Preparation and callback hints also check the policy. The provider adapter remains outside Spring mutation interception so network I/O never holds the business transaction open.

An already in-flight provider call cannot be recalled by a database suspension. If suspension arrives during dispatch, the result transaction is denied and the local intent remains `UNKNOWN`; the external outcome may differ. After authorized resume and current epoch, reconciliation queries the same provider reference and does not initiate another collection. Suspension between the pre-dispatch check and external I/O remains an external race; it never authorizes a local ledger write. Recovery receipt review cannot certify this outcome, bypass the fence, or apply a production cutover.

`ProviderPostgresTest` includes a controlled lifecycle policy, verifies that suspended preparation/callback/reconciliation writes are rejected, asserts provider I/O occurs outside a transaction, and simulates suspension during dispatch followed by query-only recovery without duplicate initiation or premature ledger posting. Real lifecycle lock behavior is covered separately by platform/security acceptance. These new assertions require the root test run before claiming acceptance.
