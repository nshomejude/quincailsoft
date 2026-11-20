# Agent 19 — report web wiring

Read root/web AGENTS, brand requirements, architecture §44.1/§44.3, current migration evidence/registers, Java ReportingService/ReportingController and report catalog, the original v3/reports.js and workflow binding tests. The existing page composition/branding is preserved; no redesign or synthetic analytics introduced.

## Concrete gaps and changes

The Java report catalog exposes `requiresDateRange` and `permission`. The web page instead hardcoded only three dated codes, so newer dated reports were requested without required dates. The download route hardcoded only five codes even though the page displayed the full backend catalog, producing 404 for otherwise implemented export links.

The page now derives date controls/default filters from the actual source contract and hides report choices lacking their specific authority. The export route validates a bounded report-code syntax, retrieves the catalog with the current session's bearer token and no cache, and resolves only a catalog-listed export. Backend execution remains the final authority for permissions, current membership/epoch and scope. Only supported date/paging query keys are forwarded; arbitrary tenant/target keys are excluded. Report rows/amounts remain strings and the selected-page warning remains.

Owned files: `apps/web-admin/lib/report-contract.ts`, `apps/web-admin/tests/report-contract.test.ts`, `apps/web-admin/app/reports/page.tsx`, `apps/web-admin/app/reports/[code]/export/route.ts`.

## Acceptance

24 web Node tests passed, including new date-range, arbitrary-catalog-code export, traversal/target rejection and supported-filter tests. `git diff --check` passed for owned files. Full web TypeScript `tsc --noEmit --incremental false` passed using the existing pinned dependency installation (temporary symlink removed). Source-shape wiring is verified, not all-report business parity. The tests are not live HTTP/OIDC acceptance.

Legacy v3 report rendering and workflow binding coverage remain references only: this increment does not mark complete report/template migration. Live authenticated download, desktop/narrow screenshots and visual equivalence are NOT_RUN; no live session was available to this assignment. No SQL migration, backend edits, shared contract edits or lockfile changes.
