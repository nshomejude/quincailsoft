# Operations implementation — v5 integration increment

Implemented in `server/operations.py`; the HTTP handler must call `handle_api(c,user,path,body)` after authentication, HTTP-method and CSRF enforcement and commit the surrounding transaction only on success. Unknown paths return `None`.

## Implemented endpoints

| Endpoint | Input | Verified behavior |
| --- | --- | --- |
| POST `/api/backup/create` | Optional reason | Owner-only, stores canonical SHA-256 business-record backup in SQLite; returns ID, digest, entity payload and current state digest. Excludes credentials and sessions. |
| POST/GET `/api/backup/list` | None | Owner-only, latest 100 backups and current business-state digest. |
| POST `/api/backup/verify` | `backup_id` | Verifies stored digest, exact schema set, unique record IDs, metadata and declared relation targets. Accepts the previous build's checksum serialization. |
| POST `/api/backup/restore` | `backup_id`, `expected_state_digest`, `reason`, `operation_id` | Owner-only, restores only server-created backups. Rejects changed current state. Creates a safety backup in the same transaction, restores business records, advances versions, revokes all sessions and audits the operation. Repeated same operation returns recorded result. |
| POST `/api/import/preview` | `entity`, `csv`, optional `delimiter`, optional column-to-field `mapping` | Owner/manager plus entity-write authorization. Typed CSV parsing with row errors, duplicate detection and account-bound 30-minute preview. Max 500 rows / 512 KB. |
| POST `/api/import/commit` | `preview_id`, `digest`, `reason`, `operation_id` | Rechecks permission and current validation; commits all rows or none, creates draft records, sets product stock to zero, marks preview consumed and audits. Idempotent same-request replay. |
| POST/GET `/api/system/integrity` | None | Owner/auditor only. SQLite quick check plus entity metadata and declared relations. Returns explicit unchecked areas. |
| POST `/api/sessions/revoke` | `user_id`, `reason` | Self-service session revocation or owner-managed staff revocation; increments auth version. Owner cannot revoke platform-admin identity. |

CSV headings are canonical field names unless mapped. Supported master entities are `product`, `customer`, `supplier`, `category`, `unit`. Password, attachment, unknown and stock quantity fields cannot be imported. Preview validation errors prevent issuing a commit token. Financial opening-balance imports are deliberately rejected because they require ledger posting rather than master-record insertion.

Restore preserves users, audit, immutable document snapshots, operation receipts and auxiliary ledgers. Because this is a business-record snapshot rather than a coordinated whole-platform recovery artifact, **restore is blocked when any payment-intent/mobile-money evidence or `finance_cash_entries` / `finance_postings` rows exist**. This avoids reverting balances without restoring economic evidence. Coordinated recovery across these domains is still required before production usage. Recovery does not re-execute external effects.

## Verification

`python3 -m unittest discover -s tests -p test_operations.py -v`: 14 tests pass. Covers backup checksums/secret exclusion, safety backup, version advancement, revoked sessions, stale restore rejection, tampered backup, provider/finance evidence guards, authorization, typed CSV import, duplicate rows, atomic rollback after concurrent SKU insertion, blocked financial import, preview ownership, session revocation and integrity reporting. Tests are isolated SQLite integration-service checks; root must separately verify browser wiring and HTTP integration.

## Remaining limits

No encrypted offsite storage, KMS, PITR, immutable object-store backups, filesystem snapshots, attachment-byte backup, external-drive discovery, production disaster-recovery orchestration, large asynchronous import jobs, XLSX, opening-balance ledger import or independently certified recovery performance. Exported JSON contains business information and must be handled as such. SHA-256 detects accidental modification, not a malicious database administrator. Local rollback is not claimed as production accounting recovery.
