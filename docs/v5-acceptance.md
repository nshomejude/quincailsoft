# QuincaiSoft v5 — implementation acceptance boundaries

Review date: 17 November 2026. Source baseline: v4 commit `6664fe76164407be04bdac6fb484b12e05248506` plus the current parallel work. This review is source inspection and local test evidence, not independent production, fiscal or native-device certification.

**The full platform is not complete.** The canonical architecture defines 40 modules and 14 cumulative acceptance phases. A rendered screen, working navigation, generic form, successful HTTP response, or changed status does not satisfy a module's end-to-end definition of done (§22.1).

The parallel agents are implementing bounded local inventory, finance, payment-adapter, operations and UI improvements. The Java/Spring/PostgreSQL production architecture, native mobile/WPF deliverables, complete commerce/offline synchronization and external provider activation remain separate unfinished work. The existing Python/SQLite service is an integration harness. No silent architecture substitution is approved by this document.

## Evidence interpretation

| Evidence | What it establishes | What it does not establish |
|---|---|---|
| 152 screen render checks | Canonical screen functions can generate HTML for fixtures | Every action/tab/dialog works for every role; responsive/accessibility correctness |
| 76 report render checks | Report views return structured output | Every named analytical metric is implemented or reconciles to authoritative ledgers |
| 26 document checks | Templates produce preview markup | Atomic issued numbering, legal/fiscal issuance, bilingual completeness or printer certification |
| Local security/API tests | Particular asserted cases pass in an isolated harness | Full tenant/company isolation, pentest, production identity, concurrency or live-provider behavior |
| Mock provider tests | Adapter request/status handling matches tested contracts | Provider-approved merchant activation, live collection, real settlement or refunds |
| A command binding | UI reaches a named handler | Domain effects, authorization, atomicity, idempotency and all compensations are complete |

`v5-gap-register.json` preserves all 40 module requirements and the **97 v4 blocked action entries**. It deliberately does not subtract an entry merely because an agent added a handler. Root integration must attach passing command tests and representative UI evidence before changing any entry to VERIFIED. The v3 coverage file is a historical baseline and cannot by itself certify v5 bindings.

## Adversarial findings handed to integration

1. **Snapshot metadata:** a snapshot created by an owner can contain labels for unrelated entities. Read-time filtering must apply to labels, product units, the source record and related records using the current reader's permissions and branch/customer scope. A source field redaction alone is insufficient.
2. **Approval integrity:** approved or in-review content must not remain editable through generic draft save or line operations. A controlled revision must invalidate prior approval, or the edit must be rejected.
3. **Domain command boundaries:** generic reserve/dispatch/publish/recover/reopen transitions must not mark arbitrary records as successfully processed. Require explicit entity-specific handlers with economic effects; unsupported operations return failure without mutation.
4. **Derived balances:** custody collected/owned quantities, deposit allocation totals, quota consumption and expected till balances must be server-owned and protected through generic forms, imports and restore validation. Product stock is already excluded from the generic product field schema; do not misreport it as directly editable there.
5. **Tenant and company isolation:** the harness hardcodes `demo` and `company-1`; fixed branch filters are not SaaS tenant isolation. Production membership, company ABAC, PostgreSQL RLS and all object/query/cache paths remain required.
6. **Money semantics:** the architecture specifies integral XAF and exact wire strings. The permissive decimal helper and frontend Number formatting need explicit currency, rounding, exponential-notation and overflow contracts before financial certification.
7. **Replay identity:** the current `(user_id, operation_id)` command cache is not the canonical company/device-independent operation identity required for bridge/offline retries. Transactional outbox/inbox, conflict reconstruction and durable change-feed tests are absent.
8. **Static serving:** exact public-root allowlisting after URL normalization is preferable to suffix/prefix checks; future files must not become reachable through encoded paths or newly added extensions.

`tests/test_boundary_audit.py` exercises four concrete API boundaries: owner-snapshot label filtering, approved record field mutation, approved line mutation, and unsupported generic role publishing. The initial run observed two passes and two approval-mutation failures. These were reported immediately and fixed by root. A subsequent isolated API run passed all four tests. The final full-suite output remains authoritative for integrated status.

## Required release gates still open

- **Architecture and build:** supported, pinned BOM; production Java modules and PostgreSQL migrations; OpenAPI contracts/generated clients; transactional outbox/inbox; CI/build artifacts/SBOM; deployment/environment/secrets runbooks.
- **Identity and scope:** production onboarding, OIDC, MFA and step-up, invitations/recovery, fine-grained role policy, tenant/company isolation, full negative authorization and export/download tests.
- **Economic workflows:** complete physical/owned/custody/reserved/transit rollforwards; taxes/rounding; purchase matching and landed costs; balanced journal/account mappings; period corrections; deposit/cash/provider/credit settlement; return/exchange eligibility and reversals. Individual new handlers do not close all these gates.
- **Payments:** real MTN merchant configuration, supported Cameroon environment, credential handling, status verification and settlement evidence; Orange merchant-specific contract/credentials and implementation; real-provider refund/chargeback and retry tests. Payment callbacks cannot be trusted as settlement proof without authenticated verification.
- **Native/offline:** actual WPF source/build evidence and companion implementation; Expo/RN app; signed disjoint quotas, epochs, protected completed facts, change feed/snapshot consistency, outage/revocation/recovery scenarios D31–D44.
- **Commerce/platform:** tenant storefront, customer session isolation, timed inventory reservations, payment/order compensation, DNS/domain verification, merchant billing/entitlements, migrations and separately controlled platform administration.
- **Documents/reports:** original-document issuance in the source posting transaction, fiscal policy, complete exact metrics, async scoped exports, EN/FR content, thermal/A4 and barcode/scanner verification.
- **Operations/compliance:** retention/legal holds, secure attachment bytes, offsite backups/PITR, tenant-selective restore, independent finance/security review, performance/fairness and disaster drills, legal/fiscal evidence and real merchant pilots.

## Completion rule for this batch

Root must collect each agent's exact files and interface, integrate against one database transaction model, run all relevant tests once the source stops changing, resolve every failure, verify key UI flows, regenerate action-to-handler coverage and package the cumulative source/evidence. Any untested or external-dependent result remains IMPLEMENTED_UNVERIFIED or BLOCKED. No P01–P14 phase is certified by agent dispatch or by this document.

For a future VERIFIED entry, attach: canonical requirement/action IDs; command and affected aggregates; role/company/branch constraints; happy-path and counterexample tests; source commit; executed results; user-visible route; document/report effects; remaining external dependency; reviewer and time. Never invent a reviewer, provider receipt, field test or evidence hash.

## Parallel delivery reports received

Inventory reports 12 passing focused tests; finance 13; operations 14. Their detailed scope and limitations are preserved in their respective status documents. These counts are component-agent reports, not a summed full-suite certification. The independent boundary audit executed 4 passing API tests after integration fixes.

One additional integration risk was reported: provider-confirmed payment success must call the unified finance settlement logic for open periods, allocations/credits/deposits and journal entries. Directly marking the payment Completed is not enough. The payment and finance agents are aligning that boundary; root must retain a review-required state if financial posting cannot safely complete.
