# Reviewed operational export expiry

This increment implements ARCHITECTURE §44.3 operational-payload retention with the legal-hold controls in §§41–44. It does not certify selective tenant recovery or complete data erasure.

## Preflight and ownership

Base: 9f4da3c15ecc5f3df2b47aa3b763e27919e47580. Read the architecture recovery/retention sections, migration summary, canonical coverage and Python parity registers. Legacy `server/operations.py` backup/restore is reference only; its business-data restore does not establish tenant PITR or erase credentials/audit. No Python behavior is marked migrated by this increment.

Owned source: operations ExportRetentionService, DefaultExportRetentionService, ExportRetention, ExportRetentionController and ExportRetentionPostgresTest; forward migration V40; generated API contract/SDK and this evidence. Dependencies: shared scoped transactions/lifecycle policy, current membership epoch, audit API, durable operation receipts/outbox, platform legal-hold records. No accepted migration is edited.

The original `operation_export_job` completion trigger made READY payloads immutable indefinitely, even after expiry. V40 permits one tightly checked READY → EXPIRED transition that empties only payload content. Export identity, digest, row count, owner, scope, expiry and source metadata remain immutable. Retention decisions cannot be deleted or rewritten.

Request requires an expired READY payload owned by the caller. An independent current owner/finance member approves a snapshot containing export ID, digest, expiry and request reason. Execution requires the original requester, expected version, current permission and authorization epoch. The same-transaction receipt, audit and outbox preserve replay evidence. The shared tenant-lifecycle advisory fence serializes purge against legal-hold placement; active holds and suspended tenants deny purging. SQL independently guards the destructive transition.

## Acceptance and limits

`ExportRetentionPostgresTest` uses PostgreSQL 18 and a runtime role without superuser/RLS bypass, applies V1–V40 and validates Hibernate mappings. It exercises independent approval, expired versus future data, scoped denial, suspension, exact replay, immutable metadata, resurrection/deletion rejection and a concurrent legal hold.

The endpoint is an administrative API workflow. No retention UI or scheduled deletion is claimed. The repository contains the export job model but no complete export-producing OperationsService implementation; the test seeds persisted jobs and does not certify export generation. Underlying fiscal/business records, receipts, object-store keys and backup copies are not purged. General retention scheduling, all data classes, selective tenant PITR/dependency closure, cutover, live OIDC acceptance and visual/release acceptance remain open. Main/release gates remain unsatisfied.
