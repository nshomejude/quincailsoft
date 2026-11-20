# Assignment 15: current reporting authorization

Status: IMPLEMENTED_UNVERIFIED pending root PostgreSQL run. No SQL migration; V55 unused. Owned source: `backend/business-modules/reporting` only.

Preflight: read repository AGENTS, ARCHITECTURE §§44.1–44.3 (exact wire integers, scope restrictions, revocation), migration summary (historical evidence only), canonical coverage and Python parity registers; inspected actual `server/policy.py` allow/scoped and `server/server.py` authorized state projection plus legacy boundary-audit disclosure assertions. The typed Java report catalog replaces broad legacy state-derived reporting; this change does not establish one-to-one acceptance for all canonical reports.

Finding: reporting service checked JWT authority strings but did not require authenticated=true or verify current scoped membership/authorization epoch. A stale token could therefore read financial projections through direct service invocation or after membership revocation if relying only on caller-side validation.

Change: preserve each report's source capability, then require active tenant/company/branch/actor membership matching the request epoch within the same scoped transaction before reading views. Unauthenticated tokens carrying authority strings fail closed. Wrong-branch membership now returns access denial, not a misleading empty authorized report. Scope predicates, RLS, exact BigDecimal-to-integer strings, date and paging constraints remain intact.

Root test request: `-pl business-modules/reporting -am -Dtest=ReportingPostgresTest,ReportPermissionMatrixTest,ReportRulesTest -Dsurefire.failIfNoSpecifiedTests=false test` with real PostgreSQL18. New acceptance cases live in `ReportingPostgresTest.exactScopedPagedStockAndSafeCsv`: inactive membership denial, stale epoch denial, current epoch positive read with exact 9999999999999992 outstanding XAF, unauthenticated token denial, branch denial. Existing tests retain >JavaScript-safe totals, full-result totals across pages, cross-tenant exclusion and CSV formula escaping. Fixtures seed legal membership states rather than disabling identity triggers or manufacturing an approved identity transition.

Remaining: no full report/filter/template parity, PDF/print fidelity, live OIDC deployment, scheduled report jobs, or all data-retention classes are certified. Dedicated permissions and active membership are necessary controls; marketing/release acceptance remains separate. No changes to shared contracts, parity acceptance or repository history by this agent.
