# Independent PostgreSQL18 execution — 18 November 2026

Tested commit: `3d86cc4fac6ed5f146c8a3ba1a6ce146a04667b0`, based on Desktop `e993fd6054a3b73fa034be6bf4f5a0e406582fee`. Backend source is unchanged from PR #5's tested source `ffaf2b4`; later commits add evidence only. This folder supplements DELIVERY_CONTROL.md and does not replace its release gates.

**Result: full 29-module Maven verify PASS — 1,049 tests across 30 suites, zero failures/errors/skips, including Modulith boundary verification.** The 986 new permission-matrix cases are included in that total; they are not 986 additional database scenarios. The earlier focused run passed three PostgreSQL suites.

## Environment and isolation

CLOUD provisioned a disposable PostgreSQL 18.7 instance from Maven Central's `io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64:18.7.0` test-runtime artifact. Published SHA1 was verified; artifact SHA256 is `10a1954a80519c634b59a9c452c3a4c5ea24c409fa0a65ba28b76f36157edecc`. This binary package is a test dependency, not a production image or a production dependency-security approval.

Java: Temurin 25.0.5+8. Maven: 3.9.11. Spring Boot: 4.0.9. The JDK/Maven checksum evidence is in the sibling cloud-permissions folder. Maven used runtime proxy configuration and the system Java trust store without disabling TLS verification.

Database listened only on `127.0.0.1:55489`. Unix-domain sockets are unavailable in this workspace, so `unix_socket_directories=''` was used. Database and Maven must run within the same execution session; separate sessions cannot share this loopback service. Initial connection attempts before establishing this setup failed, not application assertions. A shell EXIT trap stopped PostgreSQL after each completed run. The server contains only synthetic test fixtures and disposable per-test databases; no Desktop preview or production database was accessed.

The isolated cluster uses local trust authentication as expected by the repository test harness, not a production authentication configuration. Cluster admin `agent` creates fixture databases/roles. Runtime roles remain non-superuser, NOBYPASSRLS; all schema constraints, triggers and forced RLS are retained.

## Fresh-cluster prerequisite found

`PostgresDocumentSettingsTest` grants membership in `quincai_app` but does not create that role. A fresh standalone invocation failed with `role "quincai_app" does not exist`; it had previously depended on prior setup/test ordering. CLOUD bootstrapped exactly `CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS` in the disposable cluster. The rerun passed without changing test assertions or application code. Desktop should document this prerequisite or make the fixture self-contained under its test-file ownership. No shared file was edited without handoff.

## Commands and evidence

Environment for both successful runs:

```text
JAVA_HOME=<scratch>/toolchain/jdk-25.0.5+8
MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts
QUINCAI_PG_TEST_URL=jdbc:postgresql://127.0.0.1:55489/postgres
QUINCAI_PG_TEST_USER=agent
```

From `backend/`, focused database checks:

```text
mvn -s <scratch>/toolchain/settings.xml -B -ntp -fae
  -Dmaven.repo.local=<scratch>/toolchain/m2
  -pl business-modules/reporting,business-modules/documents,platform-control -am
  -Dtest=ReportingPostgresTest,PostgresDocumentSettingsTest,PostgresPlatformIntegrationTest
  -Dsurefire.failIfNoSpecifiedTests=false test
```

These three selected PostgreSQL tests passed with zero failures/errors/skips. Each contains multiple behavioral assertions. Covered cases include concurrent document-settings replay/conflicts/actor/version checks, scoped exact report totals/RLS/CSV, and independent platform approval/revocation. The platform test deliberately proves approval does **not** create a merchant tenant; passing it does not implement provisioning.

Full reactor, same environment, without test selection or skip flags:

```text
mvn -s <scratch>/toolchain/settings.xml -B -ntp -fae
  -Dmaven.repo.local=<scratch>/toolchain/m2
  -DargLine=-javaagent:<scratch>/toolchain/m2/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar
  verify
```

See `result.json`, `focused-maven.log` and `full-maven.log` for actual outcomes and hashes. Paths in logs are normalized to `<scratch>`; hash values cover the published normalized files. Mockito's explicit agent is necessary in this environment; it changes test instrumentation only. No production source, POM, migration, auth, lockfile or UI file was modified.

## Acceptance boundary

Existing-suite execution is not proof that all required workflows are implemented. Preserve the outstanding 735-entry parity register and original-screen requirements. Dataset ingestion is still zero in Desktop's latest evidence; no historical records were imported by these tests. Source validation/export comparisons do not replace Java mapping, per-record counts, duplicate-history/replay and exact per-branch/party financial reconciliation.

The provider adapter tests remain simulated. Full adversarial current-role/epoch coverage, real provider acceptance, all UI/browser workflows, complete onboarding/billing/HR/payroll, reports/documents, deployment/restore/monitoring and an authorized production destination remain open. Mobile/WPF paused. No merge or production certification.
