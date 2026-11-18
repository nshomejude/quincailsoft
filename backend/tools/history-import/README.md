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
