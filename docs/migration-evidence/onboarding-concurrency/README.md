# Onboarding concurrency acceptance — CLOUD

2026-11-18, Africa/Douala. Base `e993fd6054a3b73fa034be6bf4f5a0e406582fee`. Fresh GitHub review found Desktop unchanged, PRs #5/#6/#7 still draft and no new acknowledgment in issues #2/#3/#4. Required control documents are unchanged from the prior review; no ownership is silently reassigned.

## Changed paths and acceptance scope

One new test: `backend/platform-control/src/test/java/com/quincaisoft/platformcontrol/PostgresOnboardingConcurrencyTest.java`. Three evidence files under this directory. Boundary announced in issue #4 comment 6971981358. No existing implementation, migration, shared build/auth, importer, merchant UI or Desktop file changed.

ARCHITECTURE requires independent approval, safe replay and concurrent command acceptance (§§5.3, 7 and 21; N01/N12 onboarding). Existing `PostgresPlatformIntegrationTest` covers sequential requests/approval/revocation. This addition executes simultaneous service calls through separate thread-bound security contexts and JPA transactions against real PostgreSQL18.7, using a synthetic restricted runtime role (NOSUPERUSER/NOBYPASSRLS) and all current migrations applied by a separate fixture administrator.

One JUnit scenario contains these assertions:

- Six simultaneous identical requests return exactly the same response and leave one request row.
- Two simultaneous requests reuse another operation ID with different payloads: exactly one succeeds, one throws IllegalStateException, and replay of the winner returns its committed response.
- Two distinct independent reviewers race with expected version zero: exactly one succeeds, one throws IllegalStateException; persisted approval version is one and the winner can replay.
- Revoking the winning reviewer blocks approval replay. Original request replay remains unchanged after approval; revoking the maker blocks request replay.
- Exactly two request rows and one approved row remain; zero tenant and membership rows exist. Approval is not provisioning.

Worker start is coordinated by a barrier. Futures and worker shutdown have bounded waits. This is a regression scenario, not a load benchmark or exhaustive proof of all schedules. Unexpected runtime exceptions do not count as success: result types/counts and committed state are asserted. Security contexts are cleared per worker. Disposable database and role are removed; the local server was stopped after execution.

## Executed checks

Java25.0.5, Maven3.9.11, repository Boot4 configuration, PostgreSQL18.7. From `backend/`:

```sh
QUINCAI_PG_TEST_URL=jdbc:postgresql://127.0.0.1:55489/postgres \
QUINCAI_PG_TEST_USER=agent \
mvn -B -ntp -pl platform-control -am \
  -Dtest=PostgresOnboardingConcurrencyTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Runtime also supplied an external Maven proxy settings file, cached dependency repository, system TLS truststore and JDK25 JAVA_HOME. No repository build settings changed. Local PostgreSQL used a scratch cluster and loopback TCP; no Desktop or production database was contacted.

**PASS: 1 test, zero failures/errors/skips**, selected two-module build. `node scripts/check-project-rules.mjs` and staged `git diff --check` passed. `maven.log` retains full output with trailing whitespace removed; `result.json` binds test/service source hashes. No full reactor, HTTP/OIDC, UI, provisioning, provider, deployment or restore acceptance was run. Prior full-reactor evidence in PR #5 applies to its own source, not this new branch.

## Remaining blockers and next action

The service still ends at APPROVED_FOR_PROVISIONING. Tenant/company/branch/owner creation, typed durable provisioning progress, failure recovery, outbox/audit completeness and lifecycle controls remain unfinished. Exact shared implementation/API/schema files and migration numbering must be acknowledged through #4 before integration. This PR is test-only and draft; review/integrate it before coordinated provisioning implementation. No Python parity entries or module acceptance statuses are promoted.

Issues #2/#3 were reviewed but have no new ingestion/UI findings in this change, so their existing updates were not duplicated. Historical Java ingestion remains zero with no accepted source mapping/replay or exact stock/AR/AP/financial reconciliation. Original UI fidelity, Java analytics, manufacturer/brand/supplier/variant work and final integration remain Desktop-owned. Full current-commit functional/security/UI/database/deploy/restore acceptance and a production destination remain unresolved. Mobile/WPF remain paused. No merge or readiness claim.
