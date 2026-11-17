# QuincaiSoft — unified workflow integration build v5.1

Six parallel implementation/review agents contributed inventory, finance, mobile money, dedicated UI dialogs, operations and an independent gap audit. This is still a local integration build, **not a finished production platform**. The Java 25/Spring Boot production architecture remains outstanding; the bundled Python/SQLite service proves bounded workflows.

## Run

Python 3.11+ with SQLite, no third-party Python dependencies:

```sh
python3 server/server.py --demo --port 5173
```

Open http://127.0.0.1:5173. Choose a demonstration account on the sign-in page. Accounts use `<role>@demo.quincai.local` and public test password `Demo-Quincai-2026!`. Roles: owner, manager, cashier, inventory, purchasing, finance, auditor, platform_admin, customer. Demo seeding requires --demo and an empty database. Do not use these credentials to protect real data. Optional --data-dir chooses a separate runtime directory; `npm run dev` uses runtime/v5 for independent demo QA.

## Added in v5

- Transactional goods receipt posting with purchase quantity limits and weighted product cost; count snapshot/capture/approval/posting; same-branch warehouse transfers and partial receipt; custody pickup; bounded physical returns; zero-loss repack; quarantine/recovery/disposal.
- Deposit receipt, allocation and refund; cash-backed customer/supplier payments; supplier advance/invoice matching; cash reconciliation; balanced operational journals/reversals; period controls; quote conversion; expenses and bank matching.
- MTN adapter code, durable payment intents, retry-safe initiation, provider status verification, callback reconciliation flags, sandbox isolation and balanced finance posting. Closed periods preserve confirmed provider truth for later review. Tests use simulated HTTP responses; no real provider sandbox or live transactions were executed.
- Dedicated workflow dialogs, explicit record selection, reason/version checks, stable retry IDs, mobile-money status, backup and CSV workflows. POS draft and all lines now save atomically and idempotently.
- Guarded server-created backup restore, safety backup, typed master-data CSV preview/commit, integrity checks and session revocation. Restore refuses data sets with finance/provider auxiliary evidence until coordinated recovery is implemented.
- Approved-record and approved-line immutability; snapshot-label permission filtering; unsupported generic business transitions rejected.

## Added in v5.1

- Approved zero-loss cutting with explicit output product, immutable product lineage, exact length and cost conservation, warehouse scope and product-version checks. Kerf, scrap and lot lineage remain blocked.
- Bounded customer return credit from immutable original zero-tax sale valuation, proportional discount allocation, receivable/customer-credit split, duplicate prevention and period controls. Physical returns are held from resale until credited. Changed-cost and unsupported valuation cases fail closed; cash/provider refunds remain separate and unimplemented.
- Product-row inputs for stock counts and partial transfer receipts, cut and return-credit detail controls, and credit-status display.
- Current-role and branch checks on replayed responses, nested cost redaction, and shared resolved-path restrictions for static GET/HEAD requests.

These changes extend the local harness. The action register still contains 53 integration-required bindings; its unchanged count does not measure the added detail controls or certify completion.

## Verification

95 automated API/domain/security tests pass (see docs/v5.1-test-results.txt). The original shared-renderer checks cover 152 screens, 1,140 field placements, 76 report views and 26 document families. These numbers do not establish production completeness. Current action bindings are separately recorded in docs/v5-action-bindings.json.

```sh
python3 -m unittest discover -s tests -v
node v3/validate.cjs
node tests/validate_workflow_bindings.cjs
node tests/test_workflow_forms.cjs
```

Browser checks verified owner login, POS item selection and atomic draft save, command review dialog, and mobile-money status view. The browser Complete Sale submission was blocked by automatic approval review, so browser end-to-end posting was not verified. Isolated API/domain tests are separate evidence.

## Remaining release gates

Every module is NOT complete. Read docs/v5-gap-register.json and docs/v5-acceptance.md. All original 97 blocked action entries are preserved for traceability; some now have bounded handlers, while the new binding register is not an end-to-end completion count.

Orange Money is unavailable pending the subscribed Cameroon merchant transaction contract. MTN requires merchant configuration, provider acceptance and real sandbox testing before live activation. Credentials are configured outside source; .env.example is documentation only and is not automatically loaded. No API keys, PINs or real customer data are shipped.

Production Java/PostgreSQL/OIDC/tenant isolation, full tax/fiscal rules, customer-custody/delivery revenue recognition, return financial credits, cross-branch/offline authority, cut output lineage, full lot/serial tracking, complete native mobile/WPF clients, full bilingual UI, production backup/PITR and external email/MFA/DNS/printer integrations remain incomplete. Nonzero-tax and unsupported recognition postings are rejected. Some reports remain basic source projections; journal/map inputs are functional integration editors rather than polished final screens.

## Evidence and source

- docs/Architecture.md: retained master design contract.
- docs/v5-test-results.txt: actual automated test output.
- docs/inventory-status.md, finance-status.md, operations-status.md, mobile-money.md, ui-wiring-status.md: bounded implementation details and gaps.
- docs/v5-gap-register.json: module/action acceptance register.
- previews/: actual browser captures.
- v3/contracts.json: canonical screen/schema/report/document definitions.

Historical v1–v4 files are retained for cumulative traceability. This README supersedes their completion statements. Runtime databases, sessions and credentials are excluded from source control.
