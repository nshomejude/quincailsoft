package com.quincaisoft.platformcontrol.api;

import java.util.UUID;

/** Must validate issuer, signature, expiry, audience and verified-contact claim; never trust client claims. */
public interface OnboardingIdentityVerifier {
    record VerifiedOwner(UUID subject, String email, String issuer) {}
    VerifiedOwner verify(String token);
}
