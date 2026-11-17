# Dedicated workflow UI wiring

Active additions: `v4/workflows.js` and `v4/workflows.css`, loaded after `v4/shell.js`. The file installs wrappers around the existing renderer, action descriptor, workflow executor and transition review. Existing canonical routes remain unchanged.

## Implemented UI

- Every mutating workflow dialog asks for an exact record; it no longer silently mutates the first register row. Displays reference, status, version and available line-product IDs.
- Reason, record version and permissions accompany each command. The same user/path/payload combination retains its operation ID in memory across retries; server idempotency remains authoritative. Errors leave the dialog open. Only successful server responses produce completion messages.
- Record-specific product quantity rows for count capture and transfer receipt, refreshed when the selected record changes. Counts require every snapshot quantity; partial transfer receipts omit blank/zero rows and display remaining quantities.
- Approved zero-loss cut dialog selects output product, warehouse and evidence, and submits current parent/output product versions. Customer return credit dialog requires approval authority and business date, with explicit separation from cash/provider refunds.
- Goods receipt posting; inventory movement/count posting; count snapshot and physical-count capture; transfer dispatch and partial receipt; custody pickup and fulfillment handover; approved returns and repack; recall quarantine/recovery/disposal.
- Deposit posting/allocation/refund; customer/supplier payment posting with cash-session and invoice references; supplier advances and allocation; invoice matching; cash open/close with counted balance and variance reason; journal posting/reversal; period closure/reopen; quotation issue/conversion.
- Domain command toolbar appears on authorized record details/registers. The server still decides role, state, branch and accounting eligibility.
- Backup create/download, list/verify and restore review. Restore requires exact `RESTORE` confirmation, reason and reviewed state digest. Successful restore signs the user out when the server revokes sessions.
- CSV master-data selection, file reading, optional column mapping, server preview/validation and commit. Opening balances are not represented as ordinary imports.
- Integrity results, current-password change screen, lock/sign-out/switch account, self/owner session revocation.
- Document zoom and duplicate-copy display.
- Mobile-money request and status dialogs: provider configuration/environment, payment-record selection, phone number, idempotent initiation and provider query. The UI does not accept API secrets or payment PINs. Sandbox and unknown results are labelled; missing provider configuration remains an error rather than a success.

## API dependencies

All business operations use `POST /api/command`; fields are declared in `commandForms` and `commandBindings`. Operations API endpoints follow `docs/operations-status.md`. Mobile-money endpoints follow `docs/mobile-money.md`.

## Explicitly not completed

Orange Money live activation is not implied by its provider option. External enrollment, MFA, owner recovery, DNS/certificates, device authority/cutover, certified offline synchronization, physical printer discovery and notification delivery remain dependent on their real services.

Cut completion supports only the dedicated zero-loss piece-to-piece workflow; scrap, kerf losses and lot-level traceability remain unsupported. Offline device revoke/retire and quota freeze remain explicit unavailability notices, because a metadata status change alone cannot safely revoke authority or release reserved exposure.

Journal entry dialogs still accept validated JSON arrays for accounting lines. Count and partial transfer receipts now use labelled product rows. Journal editing remains an integration operator form rather than the final retail editing experience. A browser-only retry after closing/reloading the app does not preserve the in-memory operation key; workflows requiring durable offline recovery still need their authoritative operation registry UI.

Existing navigation-only bindings not replaced here retain their original limitations. This addition does not certify all 656 named actions complete.

## Checks performed

- `node --check v4/workflows.js`: pass.
- VM interaction checks: exact action binding, deposit allocation form fields, command endpoint/payload, operation ID, and unconfigured-provider dialog: pass.
- Additional VM checks pass for dynamic count rows, selection reset, zero counts, transfer remaining preview and exact command payload/version/operation ID.
- Parent integration agent owns combined API, browser and repository validation. No full browser matrix or production-provider payment test is claimed by this module.
