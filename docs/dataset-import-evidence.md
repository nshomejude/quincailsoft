# Issue #2 historical dataset integration evidence — 18 November 2026

Desktop Codex owns Java ingestion, local integration and acceptance. The source package is `941f3ba87fb2361a95baff7a8673ff272b34d003`, cherry-picked as `5013e25` onto `handover/codex-computer-2026-11-18`. The local preview uses the application code at `831d2df`. No historical import mapping version has been accepted. Mobile and native desktop remain paused. The independent documents and reporting parity audit remains with the ChatGPT agent.

## Executed source checks

From the repository root, run in this order:

```powershell
node datasets/business-history/generate.mjs datasets/business-history/generated
node datasets/business-history/validate.mjs datasets/business-history/generated
node datasets/business-history/java-import-preflight.mjs datasets/business-history/generated
node datasets/business-history/verify-corruption.mjs datasets/business-history/generated
```

All four commands succeeded on Windows with Node.js 26. Published and generated manifests and validation summaries matched. The validator ran 2,226,314 assertions. Both corruption scenarios were rejected after their fixture checksums were recomputed.

The generated package contains 767,945 rows across 33 files (295,929,133 bytes). Preflight enumerated 245,514 candidate source records with exact IDs, including event type and sequence for 137,984 economic events. All candidates are blocked before import. The other 522,431 rows are reference or acceptance fixtures. **Imported 0. Rejected by Java 0.** No historical database writes were attempted. The machine-readable record-level index is `datasets/business-history/generated/java-import-preflight.json`, which is locally reproducible and Git-ignored.

Published source totals in exact XAF are inventory 3,898,492,465, receivables 909,922,062, payables 765,942,251, net sales 3,373,340,314 and operating profit −68,334,076. Source debits and credits are each 45,749,568,268. Application reconciliation is blocked before import.

## Release acceptance

| Gate from RELEASE_ACCEPTANCE.md | Status | Desktop evidence or blocker |
| --- | --- | --- |
| Canonical parity and UI action, field, report and template coverage | FAIL | The strict register still contains 735 unresolved behaviors. |
| Full Java build, PostgreSQL 18 and Modulith | NOT_RUN | The current reactor rerun was started with `mvn -B -ntp -fae verify` against the distinct PostgreSQL 18.7 test container. Final result pending. The preview API health endpoint responded UP. |
| OIDC, membership, epoch, scoped RBAC/ABAC | NOT_RUN | Local cashier session rendered the seeded dashboard and product detail. Full adversarial, job, download and report matrix remains pending. |
| Historical import, replay and exact reconciliation | BLOCKED | Java catalog lacks typed manufacturer, family/model, variant and supplier-offer identity. The chronological adapter, durable source mapping, HR/payroll and exact replay equivalence are unfinished. |
| Responsive browser workflows and original branding | NOT_RUN | Actual login, dashboard and Electrical Cable detail captured on the local seeded preview. Sidebar computed color was rgb(21, 149, 230), and the white wordmark was visible. Historical screens and the full responsive browser matrix are pending. |
| Live MTN and Orange | BLOCKED | Requires authorized provider environments and actual integration acceptance. Synthetic data has no provider authority. |
| Onboarding, billing, deployment, backups/restore, monitoring and TLS | NOT_RUN | Local preview only. No production deployment or restore was executed. |
| Dependency/security scans and SaaS capacity/recovery | NOT_RUN | No fresh release-scoped evidence for this build. |
| Independent approval and exact economic behavior | NOT_RUN | Await reactor results and chronological import acceptance. |

The preview at http://localhost:3100 uses the small synthetic application seed. Screenshots saved locally as `live-login.jpg`, `live-dashboard.jpg` and `live-product-detail.jpg` under the Git-ignored generated directory. The screenshots show the seeded preview only.

## Next implementation boundary

Start with typed company-scoped manufacturer and model/variant identity in catalog under architecture §§36 and 38. Preserve separate brand and supplier offer relationships, maintain stable idempotency and leave posted history immutable. Test identical model names across manufacturers, duplicate/conflicting identities, tenant/company isolation and scope denials. Then implement the isolated Java import adapter and resume/replay ledger under §§32, 37, 38, 41 and 44. Exact reconciliation must compare per-branch and party-level application values against the reference, with no raw inserts into economic tables.
