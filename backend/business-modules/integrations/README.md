# Mobile-money integrations

MTN Java transport and durable intent/reconciliation state are implemented. Credentials are server-side. Pending/unknown responses do not settle money. Stable references survive retries; sandbox verification cannot enter the live XAF ledger. Real provider activation and acceptance remain unverified.

Orange Money is explicitly unavailable pending the subscribed Cameroon merchant transaction contract. No fake adapter or manual mobile-money completion is exposed.

See `docs/migration/m03.md` for environment configuration, verified source references, exact boundaries and acceptance gaps. `ProviderPostgresTest` exercises durable states and accounting retry against PostgreSQL18 with a simulated provider only. It does not make real payment requests.
