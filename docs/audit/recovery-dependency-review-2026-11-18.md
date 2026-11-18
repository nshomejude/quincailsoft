# Recovery dependency review — CLOUD, 18 November 2026

Reviewed commit: e993fd6054a3b73fa034be6bf4f5a0e406582fee. Authority: ARCHITECTURE §41.1–41.3, Q036–Q039, D41–D42; delivery control remains docs/DELIVERY_CONTROL.md. This elaborates the existing recovery blocker, not a competing completion register.

## Execution and ownership

Executor unavailable this run: Java/Maven/PostgreSQL, project rules, filesystem and browser checks NOT_RUN. This is GitHub source review only. Previous PR test results retain their original commit/scope; PR #9 remains a failing CSV counterexample, not a fixed parser. Desktop head and issues #2/#3/#4 were refreshed; no new handoff acknowledgment or pushed Desktop code was found.

Only this new review file is changed. CLOUD owns independent recovery assurance; implementation/API/schema handoff and migration numbering remain pending #4. No existing service, migration, auth, importer, UI, mobile or WPF paths changed. No production destination is established.

## Concrete findings

1. **Manifest is not a recoverable backup.** OperationalReads.sections() produces 20 table counts and SHA-256 digests in one statement snapshot. BackupManifest stores digest/time/reason; ManifestSection stores name/count/digest. Neither stores the business rows, an object archive location, encrypted bytes or a restore mechanism. OperationsService.Backup exposes sections, not an archive. These can support integrity comparison; they cannot reconstruct lost records.

2. **Dependency closure is incomplete even as a state comparison.** The following existing tables are absent from OperationalReads.SECTIONS:

| Omitted tables | Schema source | Recovery meaning |
| --- | --- | --- |
| receivable_balance, deposit_balance, cash_session | V6__tender_cash_and_receivable.sql | Customer outstanding amounts, deposits and cash-session state |
| payable_balance, supplier_advance | V13__payables_bank_settlements.sql | Supplier liability and advance allocation |
| bank_evidence, bank_match, settlement | V13__payables_bank_settlements.sql | Approved/consumed bank evidence and settlement identity |
| sync_device, sync_recovery_evidence | V11__device_enrollment_and_recovery.sql | Device epochs, signatures and retained source facts |

This is a verified list of ten omissions, not an exhaustive schema closure. Inference: changes confined to omitted tables may leave this manifest unchanged. No mutation test was executed. Full closure must also inventory operation identities, inbox/outbox, audit, object bytes and signing-key recovery under §41.2; merely adding these ten names is insufficient.

3. **Manifest scale is deliberately bounded.** Each section uses LIMIT 5001 and rejects counts above 5000. This is an explicit preflight bound, not full-history recovery support. Keep the bound until isolated, bounded streaming recovery tooling exists; do not silently truncate or remove it. Historical import acceptance cannot substitute this digest for exact per-branch/party stock/AR/AP/financial reconciliation.

4. **Names do not establish recovery wiring.** The inspected operations module has API records, entities and helper components; no service implementation or recovery controller was found in that module tree. The separately named workers/web/OperationsController only exposes outbox audit projection. This is a scoped source observation, not a whole-application runtime route probe. Work allocation already lists operations API assembly as unfinished.

5. **Legacy success cannot be transferred to Java.** server/operations.py and tests/test_operations.py implement/test a SQLite business-record snapshot. Tests cover secret exclusion, tampered/stale backup rejection, safety backup, version advancement, session revocation, replay, authorization and blocking restore when provider/finance evidence exists. docs/operations-status.md explicitly limits that legacy snapshot. Preserve those safety outcomes while implementing the architecture-mandated Java/PostgreSQL isolated recovery; do not expose a direct shared-database overwrite or reinstate Python production routes.

## Required executable acceptance before any restore claim

| Scenario | Required assertion | Current run |
| --- | --- | --- |
| Complete tenant closure | Every exported row/reference/object belongs to target tenant/company; missing dependencies fail closed; second tenant unaffected | NOT_RUN |
| Tampering and stale review | Alter archive bytes/digests or a reviewed state record, including omitted-table fixtures; reject without writes | NOT_RUN |
| Exact recovery | Restore into isolated PostgreSQL18 candidate; compare operation IDs, stock/cash/AR/AP, document numbers and immutable history | NOT_RUN |
| Retained receipt | Recover acknowledged sale missing inside backup window once, with original identity and no duplicate cash/stock | NOT_RUN |
| Provider evidence replay | Egress blocked; record verified externally executed refund/capture facts without resending effects | NOT_RUN |
| Epoch/key recovery | Old writers/jobs fenced; historical signatures/originals remain verifiable; no automatic grant reissue | NOT_RUN |
| Scale/rollback | More than 5000 rows, bounded resource use, final delta and rollback constraints; measured RPO/RTO | NOT_RUN |

Next concrete boundary: Desktop acknowledges exact operations service/controller paths and migration number; CLOUD prepares recovery artifact/dependency contracts and isolated tests. Separate read-only manifest comparison from archive production, verification and controlled cutover. Require finance/security review and actual isolated restore evidence before promoting any recovery status.

Original S61/S62/S79 designs remain Desktop-owned. A backup list or successful checksum must not be labeled “restored.” Dataset source validation remains distinct from ingestion; all 245514 import candidates remain blocked in the current published dataset evidence. No module/parity register is promoted and release remains NO-GO.
