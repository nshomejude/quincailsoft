# Subscription restriction policy — isolated CLOUD implementation

Date: 2026-11-18 (Africa/Douala). Base: `e993fd6054a3b73fa034be6bf4f5a0e406582fee` on `handover/codex-computer-2026-11-18`.

## Scope and evidence

Implements the billing restriction rule in ARCHITECTURE §§5.4 and 8.10 and the protected-history requirement in scenario D18. This is new SaaS domain behavior, not accepted Python parity. No migration register entries are promoted. The existing `DefaultEntitlementService.requireCommerce()` only checks an enabled, unexpired COMMERCE entitlement; this new policy is **not yet connected to it or any application endpoint**.

CLOUD owns only these new Java paths:

- `backend/business-modules/subscriptions/src/main/java/com/quincaisoft/subscriptions/domain/SubscriptionRestrictionPolicy.java`
- `backend/business-modules/subscriptions/src/test/java/com/quincaisoft/subscriptions/SubscriptionRestrictionPolicyTest.java`

Evidence is confined to this directory. The exact new paths were announced in issue #4 comment 6970984092 before edits. That comment's §8.9 reference was a typo; §8.10 is the applicable section. Desktop has not acknowledged service/schema integration; no existing service, API, shared reactor, authentication, migration, merchant UI or Desktop file is changed.

Executed with Java 25.0.5, Maven 3.9.11 and the repository Spring Boot 4 dependency configuration:

```sh
# From backend/, with JAVA_HOME pointing to JDK 25
mvn -B -ntp -pl business-modules/subscriptions -am \
  -Dtest=SubscriptionRestrictionPolicyTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
# From repository root
node scripts/check-project-rules.mjs
git diff --check
```

The execution additionally used an external runtime proxy settings file, cached Maven repository and system TLS truststore; no repository build configuration was changed. Full output (trailing spaces removed) is in `maven.log`; tested source hashes and totals are in `result.json`.

Result: **55 tests, 0 failures, 0 errors, 0 skipped**; the selected 3-module build, project rules and whitespace check passed. Only the named test class ran. No PostgreSQL, Spring context, HTTP or full-reactor test ran for this change. Earlier PR #5 PostgreSQL evidence belongs to its own source commit and does not certify this branch.

## Decisions exercised

| Cases | Expected billing decision |
| --- | --- |
| Five protected operations × all seven states (35 tests, each with and without evidence) | Reads, exports, reconciliation, backup and statutory access are not restricted by billing |
| Four restrictive states × missing evidence / instant before grace end / exact grace end (12 tests) | Review required / review required / restrict new posting |
| TRIAL, ACTIVE, GRACE with old notice (3 tests) | No automatic billing restriction or lifecycle transition |
| Future review timestamp (1 test) | No restriction until that instant |
| Wrong tenant, subscription or version (1 test with 3 cases) | Evidence cannot authorize restriction |
| Invalid grace duration, audit references, version and null inputs (3 tests) | Reject invalid input |

## Integration conditions and remaining work

This policy is not authorization. `NOT_RESTRICTED_BY_BILLING` does not grant access; `RESTRICTION_REQUIRES_REVIEW` is neither permission to restrict access nor permission to post. Existing identity, active membership, company/branch scope, role/approval checks, feature entitlement and non-billing security holds remain mandatory.

Before wiring this component, agree exact existing service/API files and a forward migration number with Desktop via #4. Persist typed subscription state/version, communicated grace terms, delivered notice and approved contract-review audit references. Load evidence from trusted storage (never accept client-supplied evidence as proof), enforce tenant references and forced RLS, and recheck the current version atomically with the command. Notice/review IDs here are references, not proof of delivery or approval. Review revocation/version invalidation and concurrent plan changes need database acceptance. State transitions and grace terms require separately audited implementation; the policy does not infer status from a hidden clock.

Acceptance must include authorized read/export/reconciliation/backup/statutory workflows after lapse, denied unauthorized access in those states, stale-version/concurrent changes, atomic rollback/replay, and protection of historical AR/AP/stock/financial records. PostgreSQL18 persistence, HTTP permission enforcement, plan limits/proration/invoices/taxes, actual provider verification, audited outbox and billing UI remain unfinished. No platform receipt is manufactured or merchant cash used for subscription payment. No financial writes or external effects occur in this component.

Historical ingestion remains separate and requires actual Java initial/replay exports plus exact stock/AR/AP/financial reconciliation (PR #6). Original UI fidelity and real Java analytics remain under Desktop (#3). Production destination, deploy/restore, current-commit security and full functional acceptance are unresolved. This component closes a unit-level rule gap only; deployment verdict remains NO-GO.
