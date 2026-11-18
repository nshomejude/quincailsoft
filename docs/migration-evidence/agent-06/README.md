# Assignment 06: identity authorization epoch and approval replay

Preflight: read AGENTS.md, ARCHITECTURE §§34.2, 34.3 and 44.3, migration evidence summary, canonical migration coverage and Python parity requirements. Inspected actual `tests/test_security.py` and `server/server.py` access-change/logout/password routes, identity service/entities/controllers and V8/V18 guards. Historical summary test counts are not acceptance for this patch.

Owned changes:
- `backend/business-modules/identity/src/main/java/com/quincaisoft/identity/application/DefaultIdentityService.java`
- `backend/business-modules/identity/src/test/java/com/quincaisoft/identity/IdentityAuthorizationPostgresTest.java`
- This assignment evidence.

The service previously accepted an active owner/member even when TenantContext carried a stale authorization epoch. Owner administration and current membership now require the exact stored epoch. This protects direct module calls as well as HTTP entry points. An already-applied membership approval previously returned its result to any active owner possessing its operation ID. Replay now also requires the original approving actor. Existing independent maker/checker, branch RLS, current target epoch and SQL role-change guards remain intact. No schema or API change; V46 unused.

## Legacy behavior mapping

`test:tests/test_security.py:test_access_change_revokes_sessions` requires access changes to revoke old authority and disallows self-editing. The Java test proves independent approval changes the target epoch and disables the target; self-approval is denied; revoked request epochs cannot call current, members, changes, request, approve or revoke. Explicit revocation preserves one fact/audit/outbox event, with receipt replay available only after reauthentication at the new epoch. This is source/test mapping, pending execution evidence, not automatic acceptance.

`test:tests/test_security.py:test_logout_revokes` is a related security invariant, but this patch does not certify complete browser/OIDC logout. Local password/session storage is intentionally not recreated: ARCHITECTURE requires external OIDC and §34.2 scoped authorization epochs. Password reset, IdP logout and HTTP rejection need separate live acceptance.

New actor-bound replay denial is additionally required by §34.3 durable operation identity and independent approval. The PostgreSQL test proves the original checker can replay while the maker cannot retrieve the applied approval through the mutation method, even after obtaining a valid refreshed epoch. Wrong-branch membership lookup is concealed by RLS; direct role mutation remains rejected by SQL.

## Verification requested

Root must execute `IdentityAuthorizationPostgresTest` with PostgreSQL 18, then regress `IdentityWorkerPostgresTest` and `PostgresSyncTest`, which call identity APIs. The fixture applies all migration SQL in order, validates JPA mappings, and uses a runtime database role with NOSUPERUSER/NOBYPASSRLS. Status at handoff: NOT_RUN by this agent; no Maven run or acceptance claim made.

## Remaining gaps

Action/resource/actor/amount/short-expiry-bound step-up is absent from identity and api-server. Assignment 07 must implement the public verification API before customer exports can be enabled; exports agent was told to fail closed. Full OIDC browser sign-in/logout, password-reset replacement acceptance, invitations/membership switching and all 735 parity requirements are not certified by this patch. A live HTTP test and full migration/release gates remain root work.
