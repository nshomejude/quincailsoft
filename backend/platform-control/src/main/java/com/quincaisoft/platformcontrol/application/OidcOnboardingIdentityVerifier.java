package com.quincaisoft.platformcontrol.application;

import com.quincaisoft.platformcontrol.api.OnboardingIdentityVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public final class OidcOnboardingIdentityVerifier implements OnboardingIdentityVerifier {
    private final String issuer;
    private final String audience;
    private volatile JwtDecoder decoder;
    @org.springframework.beans.factory.annotation.Autowired
    public OidcOnboardingIdentityVerifier(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuer,
            @Value("${quincai.security.audience:}") String audience) {
        this.issuer = issuer; this.audience = audience;
    }
    OidcOnboardingIdentityVerifier(String issuer, String audience, JwtDecoder decoder) {
        this.issuer=issuer; this.audience=audience; this.decoder=decoder;
    }
    @Override public VerifiedOwner verify(String token) {
        if (issuer.isBlank() || audience.isBlank()) throw new IllegalStateException("Merchant OIDC issuer and audience must be configured");
        try {
            Jwt jwt = decoder().decode(token);
            if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(java.time.Instant.now()) || (jwt.getAudience() == null || !jwt.getAudience().contains(audience)) || !Boolean.TRUE.equals(jwt.getClaim("email_verified")))
                throw new AccessDeniedException("Verified owner contact and merchant audience required");
            String email = jwt.getClaimAsString("email");
            if (jwt.getSubject() == null || email == null || email.isBlank()) throw new AccessDeniedException("Verified owner contact required");
            return new VerifiedOwner(UUID.fromString(jwt.getSubject()), email, issuer);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AccessDeniedException("Owner identity proof rejected");
        }
    }
    private JwtDecoder decoder() {
        JwtDecoder current = decoder;
        if (current != null) return current;
        synchronized (this) {
            if (decoder == null) decoder = JwtDecoders.fromIssuerLocation(issuer);
            return decoder;
        }
    }
}
