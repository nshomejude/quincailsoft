# Local browser verification

The running Next.js application was checked against the actual Spring Boot API
on 19 November 2026. Chrome used isolated browser contexts and the existing
Keycloak one-click demo sign-in buttons. No API routes were mocked.

Desktop captures at 1440 × 1000 are retained for login, owner dashboard, and
product detail. Cashier and auditor captures and role checks are also retained.
The sidebar is sky blue and the QuincaiSoft wordmark is white.

Owner sign-in reached the dashboard and the PREVIEW-CABLE detail. Cashier and
auditor catalog reads returned 200. Their identity reads and valid product
update requests returned 403. The rejected requests did not write product data.
A malformed initial update probe returned 400 before the corrected, complete
request was used for the permission checks.

The original v4 previews were inspected. Layout differences remain in login
logo/form placement and the dashboard branch/account header. Screen statuses
remain open. Dedicated fulfillment screens, narrow viewport comparisons and
full field/action parity still require implementation and review.

Windows Computer Use found the local Chrome window and read its accessibility
tree, but screenshot capture timed out and clicks reported unavailable geometry.
The retained captures came from installed Chrome through Playwright 1.65.1.

To reproduce from the repository root with the local preview running and Chrome
installed, run these PowerShell commands.

    npm.cmd install --prefix .local-tools/browser-verification --no-audit --no-fund playwright@1.65.1
    node docs/migration-evidence/local-browser-2026-11-19/owner-check.mjs
    node docs/migration-evidence/local-browser-2026-11-19/role-check.mjs

The browser checks require the clearly labelled existing preview seed records.
They do not create historical transactions. Historical import remains deferred.
