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
