# Independent document/report review — 18 November 2026

Reviewed Desktop commit `e993fd6054a3b73fa034be6bf4f5a0e406582fee`. This supplements DELIVERY_CONTROL.md / issue #4; it does not replace the canonical register or certify a release.

## Ownership and scope

CLOUD owns this review and the two new `ReportPermissionMatrixTest.java` / `DocumentPermissionMatrixTest.java` files. Exact paths were published in issue #4 before editing. Desktop retains implementation, merchant UI, shared security/reactor, migration numbering (V23 reserved), and serialized Java/PostgreSQL execution. No shared implementation files changed. Platform/onboarding/billing implementation still needs an explicit path-level handoff. Mobile/WPF stay paused.

Read AGENTS.md, architecture §§38–39 and report/export scenario D22, current work allocation, dataset evidence, parity and coverage registers, legacy `v3/reports.js`, document handling in `server/server.py`, and related legacy operations/security tests. Legacy generic projections and placeholders are reference behavior, not acceptable substitutes for typed Java economic workflows.

## Confirmed findings

1. **12 Java reports cannot export through the merchant web proxy.** `DefaultReportingService` exposes 17 codes; `apps/web-admin/app/reports/[code]/export/route.ts` allows only five. Missing: sales-register, sales-lines, returns, deposits, expenses, purchase-progress, supplier-invoices, stock-movements, count-variances, journal-lines, tender-facts, audit-events. The proxy returns 404 before fetching Java. Desktop should reconcile its allowlist with the approved report catalog and test authorized/denied exports, date filters, pagination headers and CSV safety. No UI write was made here.
2. **Nine canonical document families are absent from the Java workspace template registry.** This establishes absence from that registry, not absence of all related domain code:

| ID | Canonical family |
|---|---|
| DOC12 | Delivery/handover note |
| DOC17 | Subscription invoice/receipt |
| DOC18 | Customer custody/pickup balance note |
| DOC19 | Supplier invoice-match exception approval |
| DOC20 | Landed-cost/valuation adjustment voucher |
| DOC22 | Intercompany reconciliation statement |
| DOC23 | Offline exception resolution |
| DOC25 | Recall notice |
| DOC26 | Deployment/restore acceptance report |

DOC23's mobile execution remains paused; its preserved requirement is not counted as finished. DOC17 and DOC26 depend on cloud-owned workflows but shared document implementation remains Desktop until transferred.

3. **Existing definitions are narrower than canonical acceptance.** DOC08 explicitly supplies current balances rather than a date-range account statement; report receivables explicitly excludes ageing; supplier-invoices explicitly excludes open AP ageing/payments. Do not count these as full statement/ageing acceptance or use them as complete dataset reconciliation. Map all 76 report requirements and 190 document field requirements individually.
4. **Large exports remain unproved.** Current synchronous page limits (1,000 rows, offset up to 100,000) do not implement the D22 bounded asynchronous 200,000-ledger-line export acceptance. Separate totals, stream/job lifecycle, current permissions and immutable evidence are still required.

A suspected stock pagination tie-break defect was rejected during review: order uses product_id then the first selected column, warehouse_id; V1's scoped stock balance key includes warehouse/product. No defect or patch is claimed for that ordering. Concurrent snapshot-consistent exports still need separate acceptance.

## Added regression tests

- `backend/business-modules/reporting/src/test/java/com/quincaisoft/reporting/ReportPermissionMatrixTest.java`: iterates every current report against every registered source permission and general report/document/admin authorities, both raw and SCOPE-prefixed, plus missing/empty authentication. Denied capabilities must never enter ScopedTransactions; correct source authority must reach the mocked transaction boundary.
- `backend/business-modules/documents/src/test/java/com/quincaisoft/documents/DocumentPermissionMatrixTest.java`: iterates every template with the same pairwise approach. Denied authorization, source listing and source reads must fail before EntityManager or original-document access. Correct source authority passes the permission gate only.

These tests cover service capability gates, not current membership/epoch intersection, controller annotations, cross-tenant RLS, original snapshots, replay, print output or financial reconciliation. They use mocks solely to prove no data access occurs on denial, never as a database substitute. They were initially NOT_RUN; the focused Java25 execution below now passes.

## Executed checks and limitations

- `node scripts/check-project-rules.mjs`: PASS lexical project rules.
- `node scripts/check-module-ownership.mjs`: PASS register structure (40 modules, no unowned rows); assignment acknowledgment and implementation remain separate.
- `git diff --check`: PASS.
- Executed Node source comparison: 17 Java report codes versus five proxy codes; 17 Java template IDs versus 26 canonical families, producing the missing lists above. This is source-level evidence, not an HTTP/browser test.
- Local `java -version`: OpenJDK 21.0.13; Maven and Docker absent. No Java25 compile, JUnit, Spring Boot or PostgreSQL tests were executed in CLOUD. No runtime pass is claimed.
- Desktop's latest published evidence reports 63 tests, zero failures/errors/skips on Java25/PostgreSQL18.7, tied to backend source at f35d258. CLOUD has not independently rerun or certified that result; it does not include these new tests.

## Desktop handoff and remaining blockers

Run the new tests in the serialized Java25 reactor, e.g. from `backend/`: `mvn -B -ntp -pl business-modules/reporting,business-modules/documents -am -Dtest=ReportPermissionMatrixTest,DocumentPermissionMatrixTest -Dsurefire.failIfNoSpecifiedTests=false test`. Then run the required full PostgreSQL18 verify suite and attach exact commit/results. The focused command intentionally omits unrelated tests; it is not a release gate. Review and fix the export proxy under Desktop ownership.

Historical import remains 0 of 245,514 candidate source records, with no accepted mapping/replay or exact stock/AR/AP/financial reconciliation. Manufacturer, brand, variant and supplier identity must remain separate. Provisioning, billing, HR/payroll, complete reports/documents, full UI/security/provider acceptance, restore and deployment remain open. No authorized production destination exists. Verdict remains NO-GO.

## Executed Java25 follow-up — 18 November 2026

At tested source commit `ffaf2b4baf725e2dae1e98a1561f4c67ca484ff6`, CLOUD compiled the focused four-module dependency reactor with Java25 and executed both added permission matrices: **510 document + 476 report cases = 986 tests; zero failures, errors or skips**. See [structured evidence](../migration-evidence/cloud-permissions/result.json), [Maven log](../migration-evidence/cloud-permissions/maven.log) and the two Surefire summaries in that directory. This supersedes the earlier environment/NOT_RUN statements for these two suites only.

Temurin 25.0.5+8 and Maven 3.9.11 were downloaded to a separate scratch toolchain and verified against published checksums. Maven used the existing runtime proxy and system Java trust store. Initial Mockito self-attachment failed; a command-line `-javaagent` pointing at the resolved Mockito 5.20.0 artifact enabled the successful rerun. No security checks, tests or repository build files were weakened. The evidence records the exact normalized command. Desktop should use its resolved Mockito artifact path if its runtime also requires explicit agent loading.

This is not a full reactor/PostgreSQL18 run: `-Dtest` selects only the two service-boundary test factories. Membership/epoch freshness, HTTP authorization, RLS, captures/replay, financial reconciliation, complete UI and deployment/restore still require execution. No new Desktop pushes or acknowledgment of PR #5/#6 was observed. No merge or release certification.

## Independent PostgreSQL/full-reactor follow-up — 18 November 2026

At source commit `3d86cc4fac6ed5f146c8a3ba1a6ce146a04667b0`, CLOUD executed the full 29-module Java25/SpringBoot4 reactor against disposable PostgreSQL18.7: **1,049 tests, zero failures/errors/skips**, including Modulith boundaries. This includes the 986 permission cases previously reported. [Execution details and limitations](../migration-evidence/cloud-postgres/README.md), logs and hashed results are now published. This supersedes earlier full-reactor/PostgreSQL NOT_RUN statements for this exact existing suite only. Historical ingestion, full required workflow coverage, UI, live providers, production restore and deployability remain unresolved. No source changes or merge.
