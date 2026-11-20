# Exact application-export reconciliation

This is CLOUD-owned, offline Node.js evidence tooling beside the existing dataset generator. Production remains Java 25 / Spring Boot 4 / PostgreSQL 18. The checker never writes a database, posts a transaction, disables constraints or implements a replacement Java importer.

## Integration contract for Desktop

After implementing the chronological Java importer, produce two independent, scope-authorized exports: one after initial ingestion and one after replaying identical source operations. Export all rows for the isolated synthetic dataset, including fully settled zero-balance items. Normalize target IDs through the durable source mapping to the original source IDs. Do not substitute manufacturer, brand, supplier, product-family or variant IDs for one another. Preserve the 18,000 separate branch/warehouse/variant stock pools.

Each directory must contain these NDJSON files using the exact fields in the generated reference (no added or dropped columns):

| File | Unique source key | Expected rows |
|---|---|---:|
| stockBalances.ndjson | branchId, warehouseId, variantId | 18,000 |
| customerOpenItems.ndjson | branchId, saleId | 29,675 |
| supplierOpenItems.ndjson | branchId, invoiceId | 21,987 |
| trialBalance.ndjson | branchId, account | 144 |
| monthlyAnalytics.ndjson | branchId, month | 210 |

All Xaf/Milli fields must remain exact canonical decimal integer strings. Negative trial balances/profits and zero balances are retained. Compare dates, parties, paid/credited values and every other reference field, not just totals. Row order can differ; missing, extra and duplicate keys fail. A mapping that collapses variants or puts balances under another customer/branch cannot pass by preserving the grand total.

Add `export-provenance.json` to each application-export directory:

```json
{
  "schemaVersion": 1,
  "phase": "initial",
  "datasetId": "quincaisoft-synthetic-history-v1",
  "mappingVersion": "VERSION_OF_EXECUTED_JAVA_SOURCE_MAPPING",
  "runId": "UNIQUE_ACTUAL_IMPORT_RUN_ID",
  "tenantId": "ACTUAL_ISOLATED_TENANT_ID",
  "companyId": "ACTUAL_ISOLATED_COMPANY_ID",
  "sourceCommit": "FULL_40_CHARACTER_EXECUTED_SOURCE_COMMIT",
  "command": "EXACT_EXECUTED_EXPORT_COMMAND_WITHOUT_SECRETS"
}
```

Replay uses phase `replay`, a different actual runId, and the same dataset/mapping/tenant/company/commit. These declarations are recorded, not authenticated by this offline tool. Never place credentials in command metadata. Supply database snapshot details, command logs, actor/role and hashes independently with the integration evidence.

```sh
node datasets/business-history/reconcile-exports.mjs \
  datasets/business-history/generated \
  /path/to/actual-initial-export /path/to/actual-replay-export
```

The checker verifies the reference manifest against the checked-in manifest and checks the five reference file hashes/counts. Any missing file, invalid provenance, data/field mismatch or replay drift exits nonzero. Successful output contains per-file hashes and the deliberately limited status `MATCHED_SUPPLIED_EXPORTS_NOT_INGESTION_CERTIFICATION`, with `javaExecutionVerified:false` and `releaseCertified:false`.

This is a proposed export format for Desktop integration, not a claim that Java currently produces it. No exporter endpoint, importer, migration or UI was modified. Desktop has not acknowledged this new contract yet.

## What remains outside this check

- Actual Java execution, PostgreSQL18 integrity/RLS, rollback and concurrency.
- Complete accounting for all 245,514 candidate IDs; imported/rejected/blocked counts and reasons.
- Durable and injective source-to-target identity mapping; unchanged mapping after retry/replay.
- No duplicated transaction/journal/movement history on replay: equal final balances alone cannot prove this.
- Per-event journal balance, immutable source documents, provider ambiguity and financial approval enforcement.
- Full AR/AP statement histories, cash/bank reconciliation, tax/payroll legality, security, UI, backups/restores and deployability.

Actual application exports must be obtained independently; copying reference files can only exercise the checker and never proves ingestion. This limitation is explicit in success output. Use a consistent application snapshot and freeze unrelated writes during each export. Bind results to actual command and database evidence, reviewed independently.

## Executed CLOUD evidence — 18 November 2026

Base: Desktop `e993fd6054a3b73fa034be6bf4f5a0e406582fee`. Rechecked issues #2/#3/#4 and PR #5: no newer Desktop push or acknowledgment of the latest cloud handoffs at review time. Read root AGENTS.md, current allocation/dataset evidence and architecture §§37–39. Exact new paths were posted in #4 before editing; no existing shared file changed.

Node v24.22.1 executed:

- `node datasets/business-history/generate.mjs datasets/business-history/generated`: reproduced 767,945 rows / 295,929,133 bytes.
- `node datasets/business-history/validate.mjs datasets/business-history/generated`: 2,226,314 assertions passed; status remains source checks, NOT Java import.
- `QUINCAI_REFERENCE_DIR=datasets/business-history/generated node --test datasets/business-history/reconcile-exports.test.mjs`: **29 tests passed; zero failures/skips**. Full-size regression uses temporary copied synthetic fixtures and clearly marked test-only provenance. All 70,016 rows matched in that test; wrong tenant/mapping/commit/run, replay drift and missing file were rejected. Unit cases reject offsets preserving totals, unsafe numeric/decimal/exponent money, missing/duplicate rows and branch/party/variant substitutions.
- `node scripts/check-project-rules.mjs`: PASS static rules.
- `git diff --check`: PASS.

These are tooling tests. No Java25/Maven/PostgreSQL/browser tests or historical imports were executed by CLOUD. PR #5's Java test execution is still pending Desktop. No release certification or production destination is established.
