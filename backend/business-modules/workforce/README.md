# Workforce extension — scoped HR and reviewed payroll accrual

Implements the W01 assignment using Java 25, Spring Boot 4, JPA and PostgreSQL 18 (migration V26). Applicable architecture §§3, 5.2–5.3, 7.2, 37–38 plus the explicitly assigned personnel extension. Legacy Python has no equivalent HR workflow; this is new functionality for the historical dataset, not a claim of Python parity.

Implemented endpoints under `/api/v1/workforce`:

- Create/update/list/detail employee employment metadata, contract type and gross salary; optimistic version and stable operation identity.
- Record/list append-only daily attendance with scoped employment/date validation.
- Create immutable payroll drafts with employee/name snapshot, explicit calculation-policy evidence, exact XAF gross/deduction/net and overlap prevention.
- Independent reviewer accrues payroll using the finance module public API. The resulting status is **ACCRUED_UNPAID**. Journal: debit PAYROLL_EXPENSE; credit PAYROLL_WITHHOLDING and PAYROLL_PAYABLE. It never fabricates bank payment or pays a provider.
- Persist atomic idempotency result, audit and outbox in the same transaction. Authorization rechecked before replay.

Service authorization requires current owner/finance membership and a matching `workforce:employees:read`, `workforce:employees:write`, `workforce:attendance:write`, `workforce:payroll:read`, `workforce:payroll:write`, or `workforce:payroll:approve` authority. Issuance of these scopes belongs to identity/configuration integration. No automatic permission escalation.

`WorkforcePostgresTest` requires QUINCAI_PG_TEST_URL (PostgreSQL 18); covers concurrent employee replay, conflicting operations, scoped roles/capabilities, dates, duplicate attendance/payroll, independent approval, exact amounts above 2^53, balanced journals, missing-period rollback, stale epoch and immutable accrued evidence. Root executes Maven serially; no test result is claimed here.

Remaining: effective-dated employment contract documents/salary changes, leave/benefits/statutory payroll policies, correction/reversal and payment settlement through finance/tenders, payslip rendering, mobile/UI, historical source-to-target ingestion, and full HTTP/release acceptance. Employee employment metadata is not a substitute for an executed legal contract. Dataset flat deductions are explicitly synthetic; this module accepts reviewed amounts and does not calculate or certify Cameroon payroll law.

## Lifecycle completion increment (V29)

- Effective-dated immutable employment/salary history with reference, actor, monotonic dates, employee-version concurrency and prevention of changing terms under existing accruals. New employees have an initial employment record; old employees with no captured history are explicitly not assigned fabricated historical contracts.
- Reviewed deduction lines carry individual code, label, exact XAF and dated policy reference. Lines reconcile to the payslip by a deferred database constraint. The older aggregate input remains explicitly labelled `REVIEWED_AGGREGATE`; it is not described as a statutory calculation.
- Draft cancellation preserves its snapshot. A corrected draft can explicitly reference a canceled or reversed payslip for the same employee/period; active overlapping periods remain prohibited.
- Accrued unpaid payroll reversal has a separate request and independent approval. Finance posts equal opposite entries in an open period; original journal and snapshot remain immutable.
- Payroll bank payment consumes unused, independently approved `OUT` bank evidence through `BankService`, then clears the liability through `FinanceService` atomically. State is `PAID_RECORDED`, not provider-verified. Payment cannot predate accrual, consume evidence twice, or proceed while a reversal is pending. Paid payroll cannot be reversed as if it were unpaid.
- Printable HTML uses immutable payslip/line snapshots and escaped content, restrictive CSP and no-store response headers. CSV export is bounded, branch scoped and spreadsheet-formula neutralized. Clients must page explicitly; `payroll-page.csv` does not imply an unbounded full export.

New permission: `workforce:payroll:pay`. The service additionally requires fresh owner/finance membership. `workforce` depends on the tenders public API only. Root must include current code in the existing full authentication/HTTP acceptance suite; no token issuance is changed by this module.

Still excluded from a completion claim: automatic statutory payroll computation (requires approved dated legal policies), statutory filing, deduction remittance, bank/provider external certification, partial payments, recovery/refund of already recorded payments, benefit/leave accrual rules, legally executed contract document workflows, final web screen fidelity and historical ingestion. The new bank-payment flow replaces the earlier missing settlement implementation for full net payroll with approved bank evidence; it does not add cash or mobile-money payroll.

Bank-consumption integration preserves the old settlement reference guarantee: V29 introduces a typed immutable `bank_consumption` registry. A registry row must reference exactly one real settlement or payroll payment, in the same tenant/company/branch and against the same evidence ID. Deferred foreign keys allow the financial transaction to create both records atomically; orphan IDs and mismatched evidence cannot commit. Existing settlement consumers are backfilled and continue through `BankService.consume`; payroll calls `consumePayroll`. Runtime has no registry update/delete grant, and forced RLS applies.

## Executed focused acceptance — 18 November 2026

Root executed Java 25/PostgreSQL 18 acceptance on this lifecycle working tree using `-pl business-modules/workforce -am -Dtest=WorkforcePostgresTest,WorkforceRenderingTest,SettlementPostgresTest -Dsurefire.failIfNoSpecifiedTests=false` with the resolved Mockito agent. All three test methods passed; execution log: `/tmp/quincai-finish-payroll.log` (root will preserve commit-bound evidence before publication).

The PostgreSQL methods exercise multiple assertions in disposable databases. They prove the scoped workforce lifecycle described above, safe rendering/export, and the pre-existing supplier/customer/bank settlement workflow with the new typed consumption registry. This is **focused acceptance**, not full reactor, HTTP/browser, provider acceptance, statutory certification, or deployment readiness. Prior failed payroll payment attempt exposed the old settlement-only foreign key; the passing run includes the typed registry fix and orphan-source rollback checks. Source frozen for root's full verification.

## Leave and configured benefits increment (V33)

Leave requests carry explicit dates and requested minutes, independent approval/rejection, and audited cancellation. Approved leave must agree with attendance; an approved request cannot be canceled after attendance or active payroll evidence depends on its period. No statutory leave entitlement, accrual rate, weekend pattern or salary deduction is inferred.

Benefit policies are explicit fixed XAF contributions **per submitted payroll period** with finite effective dates and a reviewed policy reference. A separate reviewer approves the policy and employee election. Policy revisions with the same code cannot overlap. Elections must fit employment and policy dates. Approval/cancellation cannot silently change an existing payroll period. Part-period benefit overlap rejects with an instruction to split periods; no prorating formula is guessed.

Payroll makers load `/employees/{id}/benefit-preview`. A positive employee contribution requires the exact detailed deduction code `BENEFIT_<policy code>`, policy name, amount and reference. The server freezes approved policy/election values in immutable payroll benefit rows. Employer contributions accrue to EMPLOYER_BENEFIT_EXPENSE/PAYABLE, independently of the employee net; approved reversals reverse those exact snapshots. Employer remittance is not asserted by recording the employee bank payment. Printable payslips include these frozen benefit values.

Permissions: `workforce:leave:read/write/approve`, `workforce:benefits:read/write/approve`; preview requires payroll write and frozen snapshots require payroll read. Existing owner/finance active-membership checks apply. Leave/policy/election lists have explicit bounded pagination. V33 and these new acceptance assertions are pending root's serialized execution; the earlier V29 pass does not certify this increment.

Root's subsequent serialized verification passed with V33 included, including WorkforcePostgresTest and WorkforceRenderingTest. This supersedes the pending V33 execution note above for the new leave/benefit assertions only. HTTP/browser/provider/statutory/release acceptance remains separately tracked by root; no blanket certification is implied.
