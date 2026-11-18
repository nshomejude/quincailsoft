# Historical catalog import adapter

This development adapter uses the signed-in Java HTTP API. It accepts the reviewed synthetic dataset and its matching SHA-256 manifest, and requires the dataset's exact tenant, company and a listed branch. It does not provision accounts or accept non-loopback destinations.

Supply an access token for the same active actor and branch on every pass. It needs catalog, purchasing and customer write capabilities. Pass it through the process environment as `QUINCAI_HISTORY_TOKEN`. Do not place it in arguments, logs or committed files.

From the repo root in PowerShell, build and run:

```powershell
mvn -f backend/tools/history-import/pom.xml package dependency:copy-dependencies
java -cp 'backend/tools/history-import/target/classes;backend/tools/history-import/target/dependency/*' `
  com.quincaisoft.tools.CatalogHistoryImport http://127.0.0.1:8080 `
  datasets/business-history/generated datasets/business-history/evidence/manifest.json `
  datasets/business-history/generated/catalog-initial.ndjson
```

Use a new output path on replay. If a request fails or the process is interrupted, run the same input again. Operation keys derive from dataset ID, record type and source ID. Java writes and receipts remain durable together. The completed NDJSON mapping records the source and returned Java IDs, including both parts of the supplier-offer key. It is published only after a complete pass. The normal preview uses a different synthetic tenant and will reject this loader.

The isolated full-application test can run the real catalog pass and replay with `QUINCAI_HISTORY_CATALOG_ACCEPTANCE=true`, the generated source fixture available, and the standard PostgreSQL test variables. This covers manufacturers, categories, units, suppliers, customers, product families, the existing SKU identities, variant associations, and supplier offers. Historical postings, exact financial reconciliation, personnel and preview data deployment are separate and still required.

## Opening capital prefix

Append `--opening` after the mapping path. Set `QUINCAI_HISTORY_TOKEN` for a historical branch member allowed to draft and submit journals, and `QUINCAI_HISTORY_APPROVER_TOKEN` for a different active member of that branch allowed to approve. The authority check stays on the Java API. January 2024 must be open in the isolated company.

The adapter verifies the event and journal file hashes, accepts only the first historical event, submits its balanced lines through the manual journal API, and records the returned journal ID. The three operation keys are stable across reruns. Use another output path on replay. The adapter stops at the opening prefix so it cannot jump over unimplemented events. The next source event is the first purchase receipt.

An isolated full-application test can execute this prefix with `QUINCAI_HISTORY_OPENING_ACCEPTANCE=true`. It compares the exact BANK, CASH and EQUITY lines and confirms one posted journal after replay.

## First historical goods receipt

Run the opening capital stage first. For sequence 2, invoke the same adapter with
a fresh receipt mapping output and append `--receipt <catalog-mapping.ndjson>`.
Use the catalog mapping exported from the same synthetic database. The maker
needs `purchasing:write` and `purchasing:receive`. The separate approver needs
`purchasing:approve`. Both actors must belong to the historical receipt branch.
Set up that branch's reviewed warehouse and the open January 2024 financial
period in the isolated database before running.

The adapter validates the receipt, purchase order, and stock movement in the
reviewed fixture before it drafts the order. Retry using the same source and
actors with a fresh output path. The output maps the source order, receipt, and
movement to the Java purchase, receipt, and posting operation. The PostgreSQL
acceptance test checks the resulting stock and journal records independently.
