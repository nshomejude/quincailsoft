# Onboarding configuration implementation

Scope: ARCHITECTURE §§5.1, 5.2, 5.3, 8.1 and 38. This implements the typed control-plane preparation workflow following the existing independent onboarding request review. It replaces no economic Python posting route. The legacy generic tenant record is not a safe provisioning mechanism and is not reused.

## Implemented

- One immutable legal-company/owner/deployment configuration per approved onboarding request: legal name, registration, address, intended owner subject/contact, deployment region, IANA timezone, English/French locale, document prefix, opening-data mode, privacy/terms versions and requested plan code.
- Every service entry point checks its platform capability and current active platform-operator enrollment. Controller checks are additional.
- Owner proof is a signed merchant-OIDC JWT. Production validation checks configured issuer, signature, expiry, merchant audience and verified-email claim. The verified subject and exact email must match configuration. Raw owner tokens are not stored in commands, events or responses and are redacted by the request record's `toString()`.
- A separate operator approves the identity-verified configuration. Approval requires the current version and cannot be performed by the original request maker or configuration maker.
- PostgreSQL row locking and operation advisory locks; conflicting operation identities are rejected. Replays require current authority and return the original saved response, including after later state changes.
- JPA writes typed aggregates. V24 installs forced RLS, transition/immutability guards, durable original-response commands and immutable platform audit/outbox facts, all committed atomically.

## API

All endpoints use `/admin/onboarding/{id}/configuration` and the separate control-plane authentication chain.

| Method/path | Capability | Behavior |
|---|---|---|
| GET base path | platform:tenants:read | Read current configuration |
| POST base path | platform:tenants:write | Configure an approved request at its current version |
| POST `/verify-owner` | platform:tenants:verify | Validate signed owner identity and bind it to profile |
| POST `/approve` | platform:tenants:approve | Independent versioned configuration approval |

Commands require `Idempotency-Key` equal to the body's operation ID and `X-Request-ID`. Owner proof must be handled only over TLS; request-body logging must remain disabled/redacted.

States are `AWAITING_IDENTITY → IDENTITY_VERIFIED → APPROVED_CONFIGURATION`. The event `OnboardingConfigurationApproved.v1` is a durable preparation fact, **not** `TenantProvisioned`, payment acceptance or tenant activation.

## Remaining blockers

The EMPTY standard-trial bootstrap is implemented below. Paid-provider confirmation, opening-data import acceptance, asynchronous fenced worker execution, cancellation/amendment workflow, external object-storage and provider-secret provisioning, externally configured IdP discovery/token-scope issuance, and browser acceptance remain separate work or external verification. The requested plan code in configuration alone is not proof of purchased/enabled entitlement. Fiscal/statutory chart mappings remain provisional until accountant review.

## Verification scope

`PostgresOnboardingConfigurationTest` uses a disposable real PostgreSQL database and restricted runtime role. It exercises capability/enrollment denial, stale versions, failed-proof rollback, proof binding, exact replay after state changes, maker-checker denial, concurrent identical operations, injected event-persistence rollback, revoked replay, RLS and unchanged tenant/membership counts. Its proof-verifier double does not certify the external IdP.

`OidcOnboardingIdentityVerifierTest` uses locally generated RSA signatures to test valid proof and invalid signature/issuer/audience/contact/expiry. Root agent serializes and records actual Maven/PostgreSQL execution; this document itself is not a test-pass certificate.

## V27 real empty-trial provisioning

`TenantProvisioningService` implements actual tenant provisioning for **EMPTY opening mode + standard-trial**. Other opening modes and paid subscriptions remain separate workflows and are rejected by this command.

`POST /admin/onboarding/{id}/provision-trial` requires `platform:tenants:provision`, an active enrolled operator, `Idempotency-Key`, `X-Request-ID`, and `{operationId, configurationVersion, branchName, warehouseName, businessDate}`. Configuration must be independently approved with verified owner identity. `GET` on the same path requires `platform:tenants:read`.

A durable intent stores immutable generated resource IDs, request hash, correlation, owner, business date, trial end, and reserved tenant storage prefix. A second transaction invokes the owning modules' public bootstrap APIs to persist the tenant, company/default retail configuration, branch, warehouse, document settings and three numbering series, open fiscal period, 22 provisional operational chart mappings, 14-day trial entitlement and owner membership. Its nine durable steps and activation event commit with those resources. The active owner row is never externally visible before successful validation and commit. Financial amounts and stock are not invented: the new tenant starts empty with zero journal entries/movements.

The transaction validates the persisted resources before `ACTIVE`; a deferred PostgreSQL constraint adds a second completeness check. Failure rolls back all business resources and records the durable intent as `FAILED`. Retrying the same actor/operation/payload reuses the same generated IDs; concurrent retries serialize and return one original response. A changed payload or operation is rejected. Runtime INSERT grants are constrained by restrictive RLS policies requiring the current approved platform bootstrap, and initial owner insertion is separately guarded. No security-definer provisioning function or runtime RLS bypass is introduced.

The result returns actual tenant/company/branch/warehouse IDs and owner authorization epoch 1. The configured external IdP must issue a fresh scoped token with these claims before that owner can enter the merchant UI. The implementation does not invent a bearer token, mutate external IdP users, fabricate paid settlement or certify external identity/browser configuration. Trial documents currently use the application's database-backed storage; the reserved namespace is not a claim that an external object-storage bucket was created. Fiscal/statutory chart review remains explicitly provisional, as required by §8.1.

`PostgresTenantProvisioningTest` covers successful actual resource creation, no synthetic ledger facts, exact replay, four concurrent retries, restricted merchant INSERT attempts, failed late-event rollback with zero partial tenant/membership rows, resumed stable IDs, unsupported opening-mode rejection and revoked-operator denial. Root records execution results separately.

## Executed acceptance for V27

Root executed the following in the integration working tree based on `831d2faf55c65832182086be780f356cc3d3b0ad` (these additions were uncommitted at execution):

```sh
bash /tmp/run-cloud-java.sh -pl platform-control,api-server -am \
  -Dtest=PostgresTenantProvisioningTest,BillingCommandGatePostgresTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -DargLine=-javaagent:/home/agent/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar test
```

Observed result in the serialized Java 25/PostgreSQL 18 run: `PostgresTenantProvisioningTest` **1 executed scenario, 0 failures, 0 errors, 0 skips**; combined selected reactor **BUILD SUCCESS**. The scenario covers the assertions and concurrent operations listed above, including preservation of the existing merchant fiscal-period creation workflow. Root's consolidated evidence captures the full log and final source hashes; this focused result does not certify later changes or external deployment.

Remaining operational dependencies: external IdP must issue the owner's fresh tenant/company/branch/epoch scope claims; signed proof is validated but no IdP-admin token/claim-mapping mutation is fabricated. Fiscal/statutory mappings are provisional; no statutory rate or compliance certification is invented. The usable bootstrap supported here is an empty 14-day standard trial, not historical ingestion, a paid subscription or external object-storage provisioning. Those paths remain explicitly rejected or separate, rather than being reported as active resources.
