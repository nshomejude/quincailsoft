# CLOUD implementation checkpoint — 18 November 2026

Base: Desktop `6ec0fef643b1e53ba64c80f157783de17a084b34`, integrated after initial cloud checkpoint `9c1e13923b478ff41864f843613942e9ad3052da`. This checkpoint implements code, not only ownership documents. Java25/Spring Boot4/PostgreSQL18 remain unchanged. No Python application code added; Desktop catalog, historical importer and UI files are preserved.

## Delivered and executed

- V24 / platform-control: typed onboarding configuration, real signed owner identity proof adapter, independent approval, replay/concurrency controls and atomic immutable command/event records. Approval deliberately ends at APPROVED_CONFIGURATION, not tenant activation.
- V25 / subscriptions: separate SaaS invoices and credit notes with exact XAF amounts, immutable snapshots, independent correction operator, database credit-limit enforcement, replay, current authorization and tenant-isolated reads. No merchant cash or provider success is fabricated. Includes PR7 restriction policy.
- V26 / workforce: employee employment metadata, attendance, payroll drafts and independently approved balanced accrual through the finance module. Payroll remains ACCRUED_UNPAID. Reactor and API dependencies included.
- Operations CSV: fixes quoted NUL and final-field column-limit bypass; all 13 regression cases pass.
- Deployment runtime guard: rejects table-owner/inherited-owner identities, runtime CREATE privileges and tenant tables without forced RLS. Actual PostgreSQL test passes.
- Deployment candidate: non-root/read-only API container configuration, immutable-image/TLS/secret preflight and 12 passing Node tests. No Docker execution or production target is claimed.
- Integrated earlier PR5 document/report permission matrices and PR8 onboarding concurrency regression test. Earlier PR6 reconciliation verifier was already in the Desktop base.

## Verification

Full `mvn verify` ran serially with Java25.0.5, Maven3.9.11 and PostgreSQL18.7, a disposable test database per integration scenario, and the resolved Mockito javaagent. All 30 reactor modules pass: **1,129 tests, 0 failures, 0 errors, 0 skipped**, 38 test suites. This includes 986 document/report permission combinations; it is not 1,129 complete business workflows.

`result.json` binds this run to SHA-256 hashes of every Java/SQL/POM source and the built executable JAR. Documentation-only changes after the run do not alter that source digest. `maven.log` retains executed results. Project rules, ownership structure, current parity inventory and whitespace checks pass.

**Full parity remains blocked: 735 required legacy behaviors have not received accepted evidence mappings.** Inventory regeneration adds source hashes/candidates only; it accepts no requirement automatically.

## Desktop integration

Review/cherry-pick this checkpoint into the current handover branch; do not merge it to main as a completed release. V23 is retained from Desktop; V24–V26 are new and must be applied in order on an isolated database before any deployment. Run the combined reactor and consumer/browser checks on the integrated head. Grant the documented new capabilities only to approved roles; no user receives automatic permissions. Preserve the original glass sky-blue QuincaiSoft screens when implementing these endpoints in the UI.

## Unfinished acceptance — not hidden by green tests

- Tenant/default-resource provisioning adapters, payment/trial confirmation, worker delivery and tenant activation.
- Full subscription plan/metering/provider lifecycle, fiscal numbering/print templates, notices and UI.
- Payroll statutory rules, contract history, corrections/payment settlement, payslip documents, historical ingestion and UI.
- Remaining report/document and Python parity requirements; historical dataset import still belongs to Desktop and is not certified here.
- Real IdP discovery/login and HTTP/browser acceptance for the new routes; no static design is a substitute.
- Full recovery: rehearsal script and real archive were prepared, but the executor denied isolated Unix socket bind. This is BLOCKED, not PASS. Selective tenant restoration, key/object recovery, routing/fencing and post-restore reconciliation remain open.
- Container/image scan/signing, TLS production ingress, monitoring/alert operations, capacity benchmark and approved deployment destination.

Verdict: **TESTED IMPLEMENTATION CHECKPOINT; NOT PRODUCTION-READY.**

Saved logs normalize trailing whitespace only; no result lines are removed.

The full reactor was rerun successfully after integrating Desktop’s latest historical import changes. Optional historical catalog/opening acceptance flags were not enabled in this run; their earlier Desktop results retain their own scope. No running preview data import is claimed.
