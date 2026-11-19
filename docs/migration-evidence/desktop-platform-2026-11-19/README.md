# Platform verification on 19 November 2026

The complete Java backend reactor passed Maven verify against isolated
PostgreSQL 18.7 with Java 25.0.5.1. All historical acceptance flags were false.
The source-bound result retains each suite and the SHA-256 of tracked backend
files. The filtered reactor output contains test summaries and module outcomes.

The report contains 1,333 passing cases and zero failures, errors or skips.
Document and report permission matrices account for 1,160 of those cases.
These counts describe executed checks. Original workflows, screen comparisons,
external integrations and unresolved parity require their own acceptance.

Web TypeScript checking and 25 consumer tests passed, including the restricted
fulfillment proxy route/method checks. The local preview was restored after a
Docker Desktop restart. Its existing development database was retained.

V35 rejected the previous migration identity because the notification backfill
requires cross-tenant visibility. Preview startup now grants that documented
authority to the dedicated migration role and rejects a privileged runtime role.
PostgreSQL confirms the runtime remains NOSUPERUSER NOBYPASSRLS. Production
deployment uses its separately controlled migration runner and secret files.

The verified executable JAR serves the local API. Root HTTP 200, login HTTP 307
to Keycloak, and API health UP were checked. Authenticated browser workflows and
original-versus-current screenshots remain unverified. Historical import remains
deferred until assigned platform migration acceptance passes.
