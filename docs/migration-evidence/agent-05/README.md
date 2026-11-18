# Assignment 05 — document disclosure and copy previews

## Preflight and scope

Read root AGENTS.md, ARCHITECTURE document ownership/printing requirements (§§18,38; source ownership table and immutable original paragraphs), access requirements and universal screen contract (§39); migration summary, canonical migration coverage and Python parity inventory. Legacy source inspected: server/server.py snapshot creation (365–370) and reader-specific label filtering (286), tests/test_boundary_audit.py test_owner_snapshot_labels_do_not_leak_to_cashier, and tests/test_security.py document cost exclusion. The old snapshot endpoint copies broader label dictionaries and must re-filter them by reader. Java captures intentionally use fixed typed source projections, not arbitrary label dictionaries.

Owned paths: documents module's DocumentController, DocumentSourceReader, DocumentWorkspaceService; new DocumentCopyPreviewTest and PostgresDocumentDisclosureTest; this directory. No database migration, module dependency, shared security configuration or contract shape change is needed.

## Change

The original document GET print endpoint accepted copy=false, which could suppress the mandatory copy marker on an already issued document. It now always returns a marked-copy preview. The compatibility parameter remains accepted. GET stays free of database write effects; existing POST print-attempts retains the physical print workflow, durable identity and recorded outcomes. This does not prove that a browser actually printed anything.

Document source and capture permission checks now require an authenticated identity in addition to source capabilities. The original-document controller's explicit source check does the same; the existing authenticated HTTP security/membership boundary remains required.

## Behavioral evidence mapping

- Legacy owner-snapshot label disclosure: PostgreSQL test issues an actual immutable invoice and captures DOC01 as the owner, with an unrelated supplier present in the database. It asserts no supplier/staff/role/integration label dictionary appears, then reads the exact capture as a cashier with only sales/document access. It also removes sales access and checks list filtering plus get/print denials. This is a typed-projection replacement for legacy dictionary redaction, not migration of /api/state.
- Immutable original issuer/template source: modifying settings after issuance leaves original/capture content and replay unchanged. HTML uses the original French issuer and escapes product markup.
- Tenant/company/branch scope: foreign scopes cannot list/read the capture or original under a PostgreSQL runtime role without RLS bypass.
- Original and capture rows reject mutation. GET copy=false still shows COPIE. Separate tracked POST returns REQUESTED with the original capture digest; physical printer acceptance is not fabricated.
- Original document render boundary: MockMvc verifies the supplied copy=false parameter cannot reach render(false). A unit denial verifies an unauthenticated token carrying a sales capability cannot authorize source access.

## Execution

Agent did not run Maven, per shared-workspace rules. Root execution requested for PostgresDocumentDisclosureTest, DocumentCopyPreviewTest and DocumentPermissionMatrixTest with real PostgreSQL 18. Results must be attached by root before acceptance. `git diff --check -- backend/business-modules/documents` passed at authoring time.

## Remaining gaps

This increment does not certify every document family, all source costs/relation fields, pagination/thermal layout, PDF generation, statutory invoice compliance, QR verification, print driver outcomes, original-vs-current UI fidelity, live OIDC/membership/epoch acceptance or full module parity. The adversarial costs check here confirms the typed receipt shape has no cost fields; full legacy return valuation/cost filtering is still owned by return/finance acceptance. No parity entry is marked accepted by this agent.
