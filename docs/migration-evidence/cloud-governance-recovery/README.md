# Cloud integration: governance, billing, workforce and recovery

This increment follows PR12 and integrates Desktop handover commit `b0e7472`. It targets the migration/handover branch. It does not certify the full product for production. The final result manifest binds executed checks to source hashes; previous evidence directories remain historical.

## Implemented scope

- V38 (unpublished commercial migration renumbered to preserve Desktop V31): tenant-bound signed keyset invoice/export cursors, immutable independently approved prorated or negotiated subscription terms and matching invoice activation. Cancellation retains authorized history/export.
- V32: authoritative login-seat, branch and warehouse quotas with concurrent reservations, aggregate-bound admission, immutable receipts and release. Caller-supplied counters cannot bypass quota admission.
- V33: independently reviewed leave, effective benefit policies/elections and frozen payroll deductions/employer contributions, preserving exact balanced postings and reversal. Merchant pages expose source records, detail, approval, printing and export.
- V34: scoped owner support consent, separate operator approval, expiring/revocable named access to fixed redacted diagnostic counts, immutable access evidence, reviewed suspension/resume and legal holds. Transaction-scoped shared/exclusive fences serialize new merchant mutations against suspension; resumed tenants require a fresh signed epoch. Retained receipts and reads remain available without granting new business authority.
- V35: opt-in billing notification delivery with fixed configured HTTPS destination, HMAC, pinned public addresses, certificate verification, absolute timeout, durable leases/retries, immutable attempts and dead-letter redrive. HTTP acceptance never constitutes payment settlement or proof of legally received notice.
- V36: scoped immutable recovery receipt candidates, independent reviews, mismatch quarantine and current suspended-lifecycle fencing. This is receipt review; `applied=false` and `productionCutoverAllowed=false` remain explicit.
- V37: bounded tenant-scoped operational metrics, private monitoring templates and authenticated synthetic performance harness.
- Deployment: API/merchant/platform/TLS ingress packaging, separate Flyway runner, file secrets, validation and rollout/rollback instructions. Runtime and migration database authorities stay separate.
- Recovery: exported-snapshot PostgreSQL backup, exact source row fingerprints and whole-database restore inside a private loopback-only network namespace with capabilities dropped. Corruption, mismatch and evidence overwrite fail closed.
- Contracts: source-generated OpenAPI and TypeScript SDK; contextual record/enum types preserve distinct API meanings. Platform JWT subjects fail validation before service invocation.

A project gate now rejects duplicate Flyway versions, including numeric aliases, before integration. Desktop screen-review annotations are preserved; regeneration does not erase reviewed evidence.

## Verification and limits

See `result.json` for final executed results. `recovery/` contains the actual V1–V38 migration hashes, 133 source/restored table fingerprints and isolation/negative-test evidence. Frontend build logs certify compilation only. Real Keycloak/Redis/PostgreSQL HTTP acceptance is distinct from visual browser acceptance and is recorded separately.

Remaining boundaries are explicit: no live PSP settlement, production target deployment, image build/signing/scanning, production TLS/identity-provider configuration or production capacity acceptance has been certified here. Docker is unavailable. Chromium cannot start in this environment because required socket creation is denied; original-versus-current desktop/narrow screenshot acceptance remains unexecuted.

Selective tenant PITR dependency closure, document/object/key recovery, production restore cutover, statutory retention/deletion execution and external legal/accounting acceptance remain separate unfinished scope. Payroll statutory filings/rates, partial salary settlement and already-paid salary recovery are not certified. No guessed statutory rules or silent destructive restore are introduced. The full parity gate remains blocked at 735 unresolved accepted-evidence mappings; no screens/modules are automatically accepted from these tests.
