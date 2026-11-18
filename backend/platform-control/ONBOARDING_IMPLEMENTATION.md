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

Actual tenant/bootstrap orchestration still needs public module APIs for default company/branch/stock location, role memberships, price list, chart mapping, fiscal/document settings, feature entitlements, storage/secrets namespaces and import-opening acceptance. Payment-or-trial confirmation, durable fenced worker consumption, tenant lifecycle activation, cancellation/amendment workflow, externally configured IdP discovery and browser/UI acceptance remain unimplemented or unverified. This implementation intentionally cannot activate a half-provisioned tenant. The requested plan code is descriptive configuration, not proof of a purchased/enabled entitlement.

## Verification scope

`PostgresOnboardingConfigurationTest` uses a disposable real PostgreSQL database and restricted runtime role. It exercises capability/enrollment denial, stale versions, failed-proof rollback, proof binding, exact replay after state changes, maker-checker denial, concurrent identical operations, injected event-persistence rollback, revoked replay, RLS and unchanged tenant/membership counts. Its proof-verifier double does not certify the external IdP.

`OidcOnboardingIdentityVerifierTest` uses locally generated RSA signatures to test valid proof and invalid signature/issuer/audience/contact/expiry. Root agent serializes and records actual Maven/PostgreSQL execution; this document itself is not a test-pass certificate.
