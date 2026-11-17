# QuincaiSoft — unified app shell and RBAC integration build v4

This cumulative build adds one authenticated shell to the existing 152-screen UI. It is a runnable local integration build with synthetic data, not a completed production ERP. The master production architecture remains Java 25 / Spring Boot; the bundled Python/SQLite service is an integration harness, not a replacement architecture.

## Run
Requires Python 3.11+ with SQLite; no third-party Python packages.

```sh
python3 server/server.py --demo --port 5173
```
Open http://127.0.0.1:5173. Do not open index.html directly or use a static-only server: authentication and saving require the bundled API. Demo accounts are created only with `--demo` and only in an empty database.

Expand “Explore demonstration accounts”, choose an account and sign in. Nine accounts: owner, manager, cashier, inventory, purchasing, finance, auditor, platform_admin, customer. Email format: `<role>@demo.quincai.local`; demo password: `Demo-Quincai-2026!`. These are intentionally public test credentials and must never protect production data.

Runtime data is created in runtime/quincai.sqlite3, excluded from this ZIP. QUINCAI_DATA can select a separate data directory. Bind defaults to loopback. For a properly configured HTTPS reverse proxy, set QUINCAI_HTTPS=1 to mark session cookies Secure. This does not provide TLS itself.

## Working in v4
- One responsive shell, role-specific overview, navigation/search, record links, forms, reports and document library.
- Password verification with salted scrypt, opaque server sessions, HttpOnly/SameSite cookies, eight-hour expiry, sign-out, password change and session revocation.
- Independent server authorization, branch/customer filtering, cost redaction for cashiers/customers, read-only auditor, separate platform administrator permissions.
- Owner user access screen at S55/S56: change existing staff role, branch, active status with reason; sessions revoked immediately. Self-access changes and conversion to platform/customer identities are blocked.
- Typed draft CRUD with server validation, version checks and audit events; line add/remove; draft duplication; POS basket saved through the API.
- Command idempotency; approval separation; sale completion decrements stock or creates customer custody; quota/custody availability checks; payment over-allocation guard; journal balance validation. These are narrow integration rules, not a complete accounting engine.
- Server-stored document snapshots and integrity hashes; report filtering, drill-down, CSV and browser printing use authorized data.

## Verification
`python3 -m unittest discover -s tests -v` passes 14 security/API tests. `node v3/validate.cjs` checks 152 screen renders, 1,140 field placements, 76 report views and 26 document templates. The render validator exercises the shared renderer with fixtures; it is not an end-to-end test of every role/screen combination.

Browser evidence: owner login, linked sale detail, edited draft saved to server, persistence visible in a later cashier session, sign-out, cashier approval control absent, forbidden S56 route denied. Screenshots are real browser captures.

## Explicit remaining work
The 97 `integration_required` entries in v3/coverage.json remain incomplete. Navigation to a form does not mean its named business workflow is finished. Full goods receipt posting, transfers, returns/refunds, valuation, accounting posting, bank reconciliation, tax/fiscal issuance and offline synchronization still need production domain implementations. Unsupported completion commands fail without changing the record.

Payment/mobile money, email/MFA/recovery/invitations, DNS, printer discovery, attachment byte storage, CSV import processing, backup restore, native WPF/Expo clients, full French translation and device-certified printing are not implemented. Some reports are source-record views or explicitly labelled approximations rather than the full named analytical metric. Demo tax examples do not establish fiscal compliance. Document hashes are not legal certification or protection against database administrators.

Role templates are fixed in server/policy.py. This build changes access for existing seeded users; production provisioning and federated identity remain outstanding. The local single-tenant harness has not passed a production security audit or a complete multi-tenant isolation assessment.

## Registers
- docs/Architecture.md: retained master architecture.
- v3/contracts.json: canonical screen, schema, report, document contracts.
- v3/coverage.json: every action classification (including unfinished work).
- docs/v4-test-results.txt: actual test output.
- docs/v4-status.json: current completion boundary.

Historical v1/v3 source files remain for cumulative traceability; index.html with v4/session.js and v4/shell.js is the active application. Historical status reports describe their original version and are superseded by this README and v4-status.json.
