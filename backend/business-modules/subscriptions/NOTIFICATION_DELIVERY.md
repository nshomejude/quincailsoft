# Billing notification delivery

This dispatcher sends the existing immutable subscription invoice/credit-note events. It is not a payment-provider adapter, email/SMS connector, or proof that a merchant legally received a notice. `HTTP_ACCEPTED` records only a 2xx response from the configured HTTPS endpoint. It never approves a subscription notice, payment or commercial contract.

## Deployment configuration

The worker is disabled unless `quincai.billing.notifications.enabled=true`. Enabling it requires:

- `quincai.billing.notifications.endpoint`: one deployment-owned HTTPS URI without user info, query or fragment.
- `quincai.billing.notifications.allowed-hosts`: exact lowercase DNS host allowlist; no tenant-supplied URL is accepted.
- `quincai.billing.notifications.signing-key`: base64 encoding of at least 32 cryptographically random secret bytes, installed through secret management and shared with the receiver out of band. Never commit or log it.
- `quincai.billing.notifications.worker-actor`: a dedicated, actively enrolled `platform_operator` UUID. Revocation prevents further claims/completions.
- Optional `quincai.billing.notifications.delay-ms`, default 5000. A tick sends at most ten events, each claimed separately immediately before sending.

The HTTPS transport resolves and validates all destination addresses at startup, then pins the validated address for that process. Private, loopback, link-local, carrier-NAT and other prohibited addresses are refused. Restart/reconfigure for approved DNS rotation. TLS uses the normal trust store, hostname verification and SNI. No redirects or ambient HTTP proxies are followed. Connect timeout is three seconds, read timeout eight seconds, and an absolute fifteen-second socket-close deadline bounds the request. Request bodies are at most 4096 bytes; response headers are capped at 8192 bytes and response bodies are neither retained nor trusted.

Changing the configured destination URI changes its fingerprint. Events previously attempted against a different fingerprint become `DEAD / DESTINATION_CHANGED`; they cannot silently be redirected. HMAC key rotation does not change the URI fingerprint; rotate the receiver secret through a coordinated deployment.

## Receiver contract

The body is a version-1 JSON envelope with event ID, event type, tenant ID, invoice ID, correlation ID and occurrence time. It contains no issuer/customer names, amounts, credentials, contract references or evidence artifacts.

- `Idempotency-Key` and `X-Quincai-Event-ID` contain the stable event UUID.
- `X-Quincai-Timestamp` is Unix epoch seconds for this HTTP attempt.
- `X-Quincai-Signature` is `v1=` followed by hex HMAC-SHA256 of `timestamp + "." + exact UTF-8 request body`.

The receiver must verify timestamp freshness and compare the signature in constant time. Persist an inbox keyed by event UUID plus payload digest; duplicates with the same digest return their original acceptance, and conflicting payloads are rejected. Return 2xx only after durable receiver acceptance. These requirements are documented integration obligations, not a claim that an external receiver already implements them.

## Durability and operations

Migration V35 creates delivery state in the same transaction as each billing event and backfills existing events. Its preflight fails unless the migration session has authorized cross-tenant visibility (a privileged migration role or enrolled platform migration actor), preventing a silently empty backfill under forced RLS. Runtime remains NOBYPASSRLS. Workers claim with `FOR UPDATE SKIP LOCKED`, a random lease token and a sixty-second lease. Claimed, finished and expired attempt evidence is append-only. Crashes or unknown transport outcomes can redeliver the same event UUID; this is deliberately at-least-once delivery. A stale or expired lease cannot record successful completion.

Network failures, 408, 425, 429 and 5xx responses retry with exponential backoff (five seconds initially, capped at fifteen minutes). Other non-2xx responses become dead letters. Eight attempts are initially allowed. An authenticated enrolled operator with `platform:billing:dispatch` may redrive a dead letter against the same destination with a reason and stable operation ID; this adds eight attempts within the bounded lifetime budget. Redrive commands preserve their original responses.

Conditional platform endpoints:

- `GET /admin/billing/notifications?status=DEAD`
- `GET /admin/billing/notifications/{eventId}/attempts`
- `POST /admin/billing/notifications/{eventId}/redrive` with operation ID, reason, `Idempotency-Key` and `X-Request-ID`

Read access requires `platform:billing:read`. No HTTP endpoint exposes machine `dispatchOne` execution. Lists are bounded to 100 deliveries and 300 evidence rows per event. Logs and persisted outcomes use bounded classifications; response bodies and sensitive exception strings are not saved.

## Acceptance scope

`PinnedHttpsBillingTransportTest` starts a real local TLS server with an ephemeral test certificate and an explicitly injected test-only loopback resolver. It verifies HMAC headers, stable idempotency identity, TLS hostname checks, no redirect following, payload bounds and rejection of private addresses by the production constructor. It is not an external endpoint acceptance test.

`PostgresBillingNotificationTest` uses PostgreSQL and an explicitly simulated HTTP-result transport to exercise durable retry, concurrent claims, lease expiry, stale completion, redrive replay, destination fencing, worker revocation and immutable attempts. Parent integration evidence records actual execution outcomes. No live receiver acceptance is claimed by these tests.
