# Screen fidelity and migration audit

Reviewed GitHub commit: 831d2dfbeeaffa0df5d39e18433f0e6f7a1a10ae. This is source/document inspection, not access to Desktop's uncommitted work or an independently executed browser/test run.

## Finding

The current Next application is a separate implementation with partial restoration of the original UI, not verified faithful reuse of every designed screen. Original root app.js/styles.css remain available, as do previews/QuincaiSoft_Dashboard_v4.jpg and QuincaiSoft_Login_v4.jpg. Desktop's globals.css includes “Original workspace composition adapted to the authenticated routes”, grouped navigation/card restoration and sky-blue/navy tokens. AGENTS.md now includes branding and UTF-8 requirements. These are useful corrections but do not prove page fidelity.

The original root app.js home view describes Sales today (FCFA), Orders to prepare, Low-stock products, Collection pending, Recent sales and Today's priorities. Current apps/web-admin/app/page.tsx instead shows Sales/Products/Purchases/Customers record counts, Workspace activity assembled from list slices, and an access panel. No business-date/branch-filtered financial or stock analytics are implemented in that dashboard component. Sales/products/purchases list lengths are explicitly “records in view”; they must not become claimed totals without complete pagination/aggregate endpoints. This dashboard is functionally and visually different.

The original root generic screen renderer itself contains local demo notes and disconnected placeholder fields. Preserve intended visual design, but do not port those placeholders as completed workflows. Use canonical contracts and real Java endpoints to complete them.

## Published progress and limits

WORK_ALLOCATION.md reports Java API packaging, PostgreSQL 18.7 Flyway startup, synthetic seed and Keycloak owner browser sign-in showing two Java catalog records. It also says targeted PostgreSQL reruns are pending after a Docker engine failure, final branded screenshots/narrow-width checks are pending after browser failure, and product history/label printing require Java integration. These are reported results, not independent reruns by this review.

The work register still states 735 unresolved parity entries. This is its published inventory, not a newly calculated count and not a count of missing modules. docs/migration-evidence/summary.json remains the older 60-test checkpoint and explicitly says full_migration INCOMPLETE; its historical main-merge/OIDC status is stale relative to later work. Refresh commit-bound evidence instead of treating it as current release certification.

## Required Desktop action

1. Integrate the accompanying AGENTS.md design-fidelity/module-acceptance rules without overwriting newer local rules.
2. Produce the canonical screen-to-route/source/API comparison register and a module acceptance matrix. Cover detail pages, dialogs, tabs, reports, templates and fields explicitly.
3. Restore the original dashboard business metrics/layout using scoped Java aggregates, real synthetic history and original brand. Preserve quick actions above stats. Implement missing APIs rather than substitute record counts or static values.
4. Complete backend parity in domain increments. Verify actual PostgreSQL, concurrency/rollback/replay, tenant/role denials and source-linked documents. Record each remaining workflow accurately.
5. Import dataset branch data/synthetic-business-history-2024 via the issue #2 instructions. Keep manufacturer, brand, supplier and SKU variant separate. Reconcile money/stock exactly before trusting dashboard charts.
6. Capture actual original/current login, dashboard, product detail and report/document comparisons first, then extend to all canonical screens. Do not certify full UI based on those four samples alone.
7. Update WORK_ALLOCATION.md and commit-bound evidence with tests run, failures/blockers and next ownership. Mobile/WPF remain paused. Do not claim complete migration or production certification while required gates are unresolved.

Cloud changes on this branch are policy/audit only; Desktop retains current implementation ownership. GitHub publication does not automatically deliver an instruction into the active Desktop session.
