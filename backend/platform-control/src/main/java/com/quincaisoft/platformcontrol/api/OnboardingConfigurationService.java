package com.quincaisoft.platformcontrol.api;

import java.time.Instant;
import java.util.UUID;

/** Control-plane preparation only. Approval is not tenant activation. */
public interface OnboardingConfigurationService {
    record Profile(String legalName, String registrationNumber, String address, UUID ownerSubject,
                   String ownerEmail, String region, String timezone, String locale, String documentPrefix,
                   String openingMode, String privacyVersion, String termsVersion, String planCode) {}
    record Configure(UUID operationId, long requestVersion, Profile profile) {}
    record VerifyIdentity(UUID operationId, long version, String ownerToken) {
        @Override public String toString() { return "VerifyIdentity[operationId=" + operationId + ",version=" + version + ",ownerToken=REDACTED]"; }
    }
    record Approval(UUID operationId, long version) {}
    record Configuration(UUID requestId, Profile profile, UUID configuredBy, UUID approvedBy,
                         String status, String version, Instant identityVerifiedAt, String identityIssuer,
                         Instant createdAt) {}
    Configuration get(UUID requestId);
    Configuration configure(UUID requestId, Configure input);
    Configuration verifyIdentity(UUID requestId, VerifyIdentity input);
    Configuration approve(UUID requestId, Approval input);
}
