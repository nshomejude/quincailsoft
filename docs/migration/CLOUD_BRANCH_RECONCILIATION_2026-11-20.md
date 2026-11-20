# QuincaiSoft — Java 25 single-root cloud migration reconciliation

Date: 2026-11-20. Branch: `integration/java25-single-root-consolidation-2026-11-20`. Review PR: https://github.com/nshomejude/quincailsoft/pull/16.

## What is actually consolidated

The canonical server build is **one Java 25 / Spring Boot 4 / Spring Modulith Maven reactor** rooted at `backend/pom.xml`, comprising **29 Maven modules**: the shared kernel, database migrations, 23 domain modules, platform control, workers, the deployable API server, and architecture tests. PostgreSQL 18 and isolated migration/runtime identities are required. The web applications, native mobile client and independent Windows desktop must remain their proper non-Java client technologies; this is **not** authorization to rewrite user interfaces or WPF in Spring Boot.

The integration parent is `codex/platform-continuation-2026-11-19` (`bd62578f9f0e3adc0609f7b4ae8f7d289e745e27`), **94 commits ahead of `main` at the moment of consolidation**. This ancestry includes the reviewed `cloud/complete-platform-work`, `cloud/csv-input-boundaries`, `cloud/dataset-reconciliation-check`, `cloud/finish-owned-modules`, `cloud/governance-recovery-v38`, `cloud/onboarding-concurrency`, `cloud/recovery-dependency-review`, `cloud/release-assurance`, `cloud/subscription-restriction-policy`, `codex/reviewed-export-retention-2026-11-18`, `codex/twenty-agent-migration-2026-11-19`, and `handover/codex-computer-2026-11-18` branches. These branches are ancestors and must not be reapplied as unrelated patches.

The additional independently diverged one-commit branches `coordination/design-fidelity-and-module-acceptance`, `coordination/platform-audit-and-ownership`, `coordination/quincaisoft-brand-lock`, `data/synthetic-business-history-2024`, and `codex/preserved-local-checkpoint-2026-11-19` require **content-level review**, not automated reversal of newer code. Their current file paths, except the separately copied `docs/LOCAL_CHECKPOINT_2026-11-19.md`, already exist in the integrated tree. This is *not* a claim of semantic equivalence.

## Migrated Java module packages physically present

- Enterprise/controls: company, branch, identity, audit, workforce, platform-control, subscriptions.
- Master data & trade: catalog, suppliers, customer-accounts, commerce, quotations, purchasing, sales, returns.
- Physical operations: inventory, operations, sync, documents.
- Financial: finance, tenders, expenses, integrations.
- Supporting: reporting, workers, db-migrations, api-server, shared-kernel, architecture-tests.

**Code presence != migration acceptance.** The historical register enumerates 40 canonical product modules, 152 screens, 656 actions, 445 tabs, 1,140 screen-field placements, 76 reports, and 26 document families. These do not map one-to-one onto Maven subprojects.

## Verification ledger

- The 2026-11-19 historical Java/PostgreSQL verification artifact records 1,333 passing tests at **its recorded source SHA**. It does not certify a different PR HEAD.
- This PR introduces an independent real-PostgreSQL/Java25 GitHub Actions reactor, plus a JUnit XML gate that refuses failures, errors, or skipped tests.
- CI exposed an outdated returns/quotation integration fixture. The integration test must authenticate with *explicit* quotation/sales authorities and must record a customer acceptance before quotation conversion. No production permission is removed to make a test pass.
- The PR strengthens input checks on the existing Java session-revocation HTTP command and adds regression tests.
- The deterministic Python-to-Java source inventory must be refreshed after source edits; generating candidates does **not** mark any requirement accepted.
- Full parity is intentionally red while required behavior remains unaccepted, and must not be weakened or disabled for a merge.

## Non-negotiable unresolved work

1. **Behavioral parity:** Review the 735 outstanding historical Python-route/command/negative-case assertions (count from the prior migration inventory; refresh on current HEAD). For each accepted requirement, commit a narrow mapping to Java source, passing named test, and execution evidence with matching source digest; do not assign blanket ACCEPTED.
2. **Original product fidelity:** Map and execute every canonical screen, tab, action, form field, report and printable across role/browser/language/responsiveness. React/Expo/WPF remain clients.
3. **Operational lifecycle:** Prove production tenant provisioning, billing and entitlement lifecycle, historical catalog and HR/payroll import, recovery dependencies and restore, notifications, concurrency/fencing, and company-wide operational closes.
4. **External boundaries:** Verify real OIDC claims with active membership and role intersection; sandbox/live provider integrations (MTN/Orange) and signed callback reconciliation; no unverified callback as payment authority.
5. **Native/device & deployment:** Test signed offline grants and mobile native builds, Windows adapter-to-host integration, Postgres fresh+upgrade migrations under separate database roles, disaster-recovery rehearsals, and production deployment/rollback.
6. **Evidence:** Re-run the full reactor and consumer contracts on the final HEAD, store signed/generated reports, and close any newly detected regressions. Earlier passing suites are not proof for untested commits.

## Merge verdict

**DRAFT; NOT RELEASE-READY.** Leave PR #16 open for Codex Cloud/desktop to reconcile and validate. It is the single integration branch for the canonical Java migration work. Do not merge `main`, cut a production release, delete legacy references, disable strict parity, or assert full migration while the gates above remain open.
