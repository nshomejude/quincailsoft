# Native sync protocol v1

Java is authoritative. Expo React Native and the Windows .NET adapter use the same UTF-8 JSON contract. No Python runtime, PWA, local master database or second authority is involved.

All routes use the `/api/v1` prefix and HTTPS Bearer authentication with server-validated tenant, company, branch and actor membership. Every POST requires `Idempotency-Key: <operationId>` and `X-Request-ID: <UUID>`. Operation IDs are immutable UUIDs. Never create a new operation ID to retry an ambiguous response. UUIDs are lowercase canonical 36-character text in signing material. Device epochs are UUIDs, not sequence numbers. Sequence numbers are positive integers; cross-platform producers must remain at or below 9007199254740991.

| Method/path | Input | Result / authorization |
| --- | --- | --- |
| POST `/sync/devices/challenges` | operationId, name, publicKeyPem | Device; `sync:enroll` |
| POST `/sync/devices/{id}/prove` | operationId, version, signatureBase64 | Device; original actor + `sync:enroll` |
| POST `/sync/devices/{id}/approve` | operationId, version, reason | Device; current database owner + `sync:manage` |
| GET `/sync/devices` | — | Device[]; current database owner + `sync:manage` |
| POST `/sync/recovery-submissions` | recovery-v1.schema.json | Receipt; current database owner + `sync:recover` |
| GET `/sync/recovery-submissions` | — | newest 100 Receipt[]; current database owner + `sync:recover` |
| GET `/sync/operations/{operationId}` | — | Operation; original actor and current scope only |

Device fields: `id, epoch, actorId, name, status, challenge, expiresAt, fingerprint, version, offlineSelling`. The fingerprint is lowercase SHA-256 of the Base64 DER SubjectPublicKeyInfo text. Device states currently implemented are CHALLENGED, PROVED, ACTIVE. `offlineSelling` is always false. Approval proves registration; it does not issue stock quota or an offline authorization grant.

Receipt fields: `evidenceId, sourceOperationId, status, payloadHash, receivedAt, businessPosted`. Status is REVIEW_REQUIRED or CONFLICTING_HISTORY. `businessPosted` is always false. A receipt is durable evidence intake only. Operation fields: `operationId, status, sourceType, sourceId, acceptedAt`; these describe idempotency intake and never imply an offline sale has posted.

## Signatures

RSA SHA-256 PKCS#1 v1.5 (`SHA256withRSA` / .NET SHA256 + RSASignaturePadding.Pkcs1), RSA >= 2048 bits, PEM SubjectPublicKeyInfo `BEGIN PUBLIC KEY`. Base64 signatures use the standard alphabet. Signing material is exact UTF-8 joined with LF, no final newline. All values are case-sensitive. JSON payload hashes are lowercase SHA-256 of the exact original payloadJson UTF-8 bytes. Do not parse and reserialize before verifying.

Enrollment:

```text
quincai-enroll-v1
{tenantId}
{companyId}
{branchId}
{deviceId}
{deviceEpoch}
{challenge}
```

Recovery:

```text
quincai-recovery-v1
{tenantId}
{companyId}
{branchId}
{deviceId}
{deviceEpoch}
{sequence}
{sourceOperationId}
{classification}
{payloadHash}
```

The recovery submission's operationId is a separate stable transport idempotency key. Preserve the original sourceOperationId, signature, epoch, sequence and payload forever until an explicit audited retention policy is implemented. Conflicting histories are retained, not overwritten. Already completed authorized and exception facts must never be converted to draft creation. Unknown keys or invalid signatures do not post and remain local evidence for manual recovery.

## Implemented boundary

There is no exclusive quota grant, epoch rotation, local economic posting, signed commit feed, snapshot synchronization or recovery-ledger resolution in v1. Clients must fail closed for offline sale completion. Draft uploads can call the existing Java sales draft endpoint and keep the original operation identity. Mobile queue SQL preserves immutable completed evidence even though mobile hardware signing and completed-evidence recovery UI are not yet available. The .NET adapter implements enrollment and recovery against these same endpoints.
