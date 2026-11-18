package com.quincaisoft.identity.api;

import com.quincaisoft.shared.TenantScope;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Trusted authentication adapter SPI, not an HTTP request/DTO or a grant-issuing API.
 * An adapter must accept only cryptographically verified, configured-issuer evidence,
 * enforce API audience and explicit issuer MFA policy, and bind it to the current
 * authenticated principal. Plain request fields, generic scopes and decoded JWTs
 * without signature/issuer/audience validation are never evidence.
 */
public interface StepUpEvidenceProvider {
    enum Assurance { VERIFIED_MFA }
    record Evidence(UUID grantId, TenantScope scope, long authorizationEpoch,
                    StepUpService.Binding binding, Assurance assurance,
                    Instant authenticatedAt, Instant issuedAt, Instant expiresAt,
                    Instant tokenExpiresAt) {}
    Optional<Evidence> current();
}
