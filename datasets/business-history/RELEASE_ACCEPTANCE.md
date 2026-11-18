# Deployment acceptance contract

User intent: complete Java migration, preserve the original QuincaiSoft UI, load the historical manufacturer-aware data, and deliver a deployable SaaS. No percentage or certificate is evidence by itself.

Desktop owns local runtime and final integration. Cloud dataset scope owns deterministic input and reference reconciliation. Track work through docs/WORK_ALLOCATION.md and this branch's GitHub issue; pushing instructions does not mean the Desktop session has read them. Preserve uncommitted work and avoid shared-file races.

Before describing the platform as production ready, record commit-bound results for:

- Canonical migration parity and screen/action/report/template/field registers, with no unresolved required behavior. Backend production launch must not depend on Python reference handlers.
- Full Java 25 / Spring Boot 4 builds and real PostgreSQL 18 migrations, typed aggregates, rollback/concurrency/replay tests and Modulith boundaries.
- OIDC and current membership/epoch enforcement, RBAC/ABAC and cross-tenant/branch denials including jobs, downloads, documents and reports; no demo-auth bypass.
- Dataset import/resume/replay results and exact branch/party financial and stock reconciliation; manufacturer/brand/supplier/variant identities preserved.
- Genuine browser acceptance of every required workflow, original sky-blue/navy/white designs, responsive layouts, French UTF-8 text and all original document templates.
- Actual MTN and Orange integration acceptance in their authorized environments. Synthetic transport tests do not certify live provider onboarding or settlement.
- Tenant onboarding/provisioning, entitlement/billing rules, backups and tested restore, observability, audit retention, secrets, TLS, network exposure, deploy/rollback instructions and readiness probes.
- Dependency/security scans and addressed findings, workload/capacity evidence at agreed SaaS scale, and load/failure recovery tests.
- Independent approval and exact money behavior for tax, procurement, credit, deposits, refunds, stock ownership/custody and period close/reopen.

Every item must state PASS, FAIL, BLOCKED or NOT_RUN, include command/evidence and identify external requirements. Certification must be limited to the exact build and tested environment; never issue blanket legal/security/payment certification. Mobile/native desktop are intentionally paused and must be stated as excluded scope rather than completed. A successful local preview is not evidence of production deployment.
