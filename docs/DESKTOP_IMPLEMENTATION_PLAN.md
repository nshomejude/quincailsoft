# Desktop implementation plan

| Batch | Assignments | Implementation and exit evidence |
| --- | --- | --- |
| Coordination and catalog | D01, D02 | Confirm issues #2–#4 and reserve V23. Add typed manufacturer, family, sellable variant and supplier offer identity. Verify forced RLS, composite references, stable replay, and same-model differentiation on PostgreSQL 18. |
| Historical adapter | D03, D05 | Provision a dedicated synthetic tenant through scoped services. Preserve source IDs and operation receipts in the same transaction as typed writes. Verify interruption, duplicate replay, conflicting keys and authorization denials. |
| Full history | D04 | Replay chronological economic commands and reconcile exact per-branch and per-party stock, journals, AR/AP, cash/bank, tax and P&L. Publish exact source IDs for rejected or blocked operations. |
| Web and parity | D06, D07 | Restore original designs and connect required workflows to Java. Verify the order, low-stock and collection dashboard measures, details, reports and templates. Update the canonical parity and fidelity registers with observed results. |
| Integration | D08, D09 | Review and integrate coordinated contributions. Run Java 25, PostgreSQL 18, Modulith, web and scoped browser acceptance serially where they share resources. |
| Release | D10 | Publish commit-bound gate evidence, independent review and deployment/rollback/restore results. Keep changes off main while mandatory gates remain open. |

Before each batch, check GitHub issues #2 and #3 and follow any new coordination in #4. Push verified increments to the handover branch. The first increment owns V23 and the catalog module. It will keep each SKU mapped to its existing stock identity and keep supplier offers separate from posted valuation. Mobile and native desktop remain paused.

## First checkpoint

V23 adds the company-scoped catalog identity tables. The disposable PostgreSQL 18.7 migration runner applied V1–V23 and passed all 19 existing assertions, including forced RLS. Java command wiring, dedicated catalog identity tests and history import remain pending. No historical rows were loaded.

The latest coordination notes add PR #6's exact initial/replay export contract to the importer exit criteria. Review PR #5's document/report evidence and the merchant CSV route exclusions in the web and integration batches. Carry forward the billing read/export guarantees and verify actual recovery archives independently of manifest checks. The existing CSV helper must reject quoted NUL and 41-field rows before importer reuse.
