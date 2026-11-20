package com.quincaisoft.identity.application;

import com.quincaisoft.identity.api.IdentityService;
import com.quincaisoft.identity.api.StepUpEvidenceProvider;
import com.quincaisoft.identity.api.StepUpService;
import com.quincaisoft.shared.TenantContext;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public final class DefaultStepUpService implements StepUpService {
    private static final Duration MAX_AGE = Duration.ofMinutes(5);
    private final IdentityService identity;
    private final ObjectProvider<StepUpEvidenceProvider> providers;
    private final Clock clock;

    @Autowired
    public DefaultStepUpService(IdentityService identity, ObjectProvider<StepUpEvidenceProvider> providers) {
        this(identity, providers, Clock.systemUTC());
    }
    public DefaultStepUpService(IdentityService identity, ObjectProvider<StepUpEvidenceProvider> providers, Clock clock) {
        this.identity = Objects.requireNonNull(identity);
        this.providers = Objects.requireNonNull(providers);
        this.clock = Objects.requireNonNull(clock);
    }
    @Override
    public void require(Binding binding) {
        Objects.requireNonNull(binding, "binding");
        var scope = TenantContext.current();
        var epoch = TenantContext.authorizationEpoch();
        var member = identity.current();
        if (member == null || !member.active() || !scope.actorId().equals(member.actorId())
                || member.authorizationEpoch() != epoch) deny();
        // No adapter, or ambiguous adapter configuration, is deliberately unavailable.
        var provider = providers.getIfUnique();
        if (provider == null) deny();
        var evidence = provider.current().orElseThrow(DefaultStepUpService::denied);
        var now = clock.instant();
        if (evidence.grantId() == null || !scope.equals(evidence.scope())
                || evidence.authorizationEpoch() != epoch || !binding.equals(evidence.binding())
                || evidence.assurance() != StepUpEvidenceProvider.Assurance.VERIFIED_MFA
                || evidence.authenticatedAt() == null || evidence.issuedAt() == null
                || evidence.expiresAt() == null || evidence.tokenExpiresAt() == null) deny();
        if (evidence.authenticatedAt().isAfter(now) || evidence.issuedAt().isAfter(now)
                || evidence.authenticatedAt().isAfter(evidence.issuedAt())
                || !evidence.authenticatedAt().plus(MAX_AGE).isAfter(now)
                || !evidence.expiresAt().isAfter(now)
                || !evidence.tokenExpiresAt().isAfter(now)
                || !evidence.expiresAt().isAfter(evidence.issuedAt())
                || evidence.expiresAt().isAfter(evidence.issuedAt().plus(MAX_AGE))
                || evidence.expiresAt().isAfter(evidence.tokenExpiresAt())) deny();
    }
    private static AccessDeniedException denied() {
        return new AccessDeniedException("Fresh externally verified step-up for this exact operation required");
    }
    private static void deny() { throw denied(); }
}
