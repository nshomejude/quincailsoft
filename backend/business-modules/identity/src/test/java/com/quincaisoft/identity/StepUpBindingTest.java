package com.quincaisoft.identity;

import com.quincaisoft.identity.api.*;
import com.quincaisoft.identity.application.DefaultStepUpService;
import com.quincaisoft.shared.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StepUpBindingTest {
    final Instant now = Instant.parse("2026-11-19T00:00:00Z");
    final TenantScope scope = new TenantScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    final StepUpService.Binding binding = new StepUpService.Binding(StepUpService.Action.EXPORT_CUSTOMER_DATA, UUID.randomUUID(), UUID.randomUUID(), "0");
    final IdentityService identity = mock(IdentityService.class);
    final StepUpEvidenceProvider provider = mock(StepUpEvidenceProvider.class);
    @SuppressWarnings("unchecked") final ObjectProvider<StepUpEvidenceProvider> providers = mock(ObjectProvider.class);
    final StepUpEvidenceProvider.Evidence good = evidence(scope, 3, binding, now.minusSeconds(30), now.minusSeconds(10), now.plusSeconds(60), now.plusSeconds(120));
    final DefaultStepUpService service = new DefaultStepUpService(identity, providers, Clock.fixed(now, ZoneOffset.UTC));
    StepUpBindingTest() {
        when(identity.current()).thenReturn(new IdentityService.Member(scope.actorId(), "owner", true, 3));
        when(providers.getIfUnique()).thenReturn(provider);
        when(provider.current()).thenReturn(Optional.of(good));
    }
    StepUpEvidenceProvider.Evidence evidence(TenantScope s, long epoch, StepUpService.Binding b, Instant auth, Instant issued, Instant expires, Instant tokenExpires) {
        return new StepUpEvidenceProvider.Evidence(UUID.randomUUID(), s, epoch, b, StepUpEvidenceProvider.Assurance.VERIFIED_MFA, auth, issued, expires, tokenExpires);
    }
    void require() throws Exception { TenantContext.call(scope, 3, () -> { service.require(binding); return null; }); }
    void denied(StepUpEvidenceProvider.Evidence evidence) {
        when(provider.current()).thenReturn(Optional.of(evidence));
        assertThrows(AccessDeniedException.class, this::require);
    }
    @Test void acceptsOnlyExactOperationAndCurrentMembership() throws Exception {
        require();
        // Same exact operation may retry; the domain's durable operation receipt owns deduplication.
        require();
        verify(identity, times(2)).current();
        denied(evidence(scope, 3, new StepUpService.Binding(binding.action(), binding.resourceId(), UUID.randomUUID(), "0"), good.authenticatedAt(), good.issuedAt(), good.expiresAt(), good.tokenExpiresAt()));
        denied(evidence(scope, 3, new StepUpService.Binding(binding.action(), UUID.randomUUID(), binding.operationId(), "0"), good.authenticatedAt(), good.issuedAt(), good.expiresAt(), good.tokenExpiresAt()));
        denied(evidence(scope, 3, new StepUpService.Binding(StepUpService.Action.APPROVE_REFUND, binding.resourceId(), binding.operationId(), "0"), good.authenticatedAt(), good.issuedAt(), good.expiresAt(), good.tokenExpiresAt()));
        denied(evidence(scope, 3, new StepUpService.Binding(binding.action(), binding.resourceId(), binding.operationId(), "1"), good.authenticatedAt(), good.issuedAt(), good.expiresAt(), good.tokenExpiresAt()));
    }
    @Test void rejectsDifferentActorTenantCompanyBranchAndEpoch() {
        for (TenantScope other : List.of(
                new TenantScope(scope.tenantId(), scope.companyId(), scope.branchId(), UUID.randomUUID()),
                new TenantScope(UUID.randomUUID(), scope.companyId(), scope.branchId(), scope.actorId()),
                new TenantScope(scope.tenantId(), UUID.randomUUID(), scope.branchId(), scope.actorId()),
                new TenantScope(scope.tenantId(), scope.companyId(), UUID.randomUUID(), scope.actorId()))) {
            denied(evidence(other, 3, binding, good.authenticatedAt(), good.issuedAt(), good.expiresAt(), good.tokenExpiresAt()));
        }
        denied(evidence(scope, 2, binding, good.authenticatedAt(), good.issuedAt(), good.expiresAt(), good.tokenExpiresAt()));
        when(provider.current()).thenReturn(Optional.of(good));
        when(identity.current()).thenReturn(new IdentityService.Member(scope.actorId(), "owner", false, 3));
        assertThrows(AccessDeniedException.class, this::require);
        when(identity.current()).thenReturn(new IdentityService.Member(scope.actorId(), "owner", true, 4));
        assertThrows(AccessDeniedException.class, this::require);
    }
    @Test void rejectsExpiredFutureOverlongAndStaleAuthentication() {
        denied(evidence(scope, 3, binding, now.minusSeconds(300), good.issuedAt(), good.expiresAt(), good.tokenExpiresAt()));
        denied(evidence(scope, 3, binding, now.plusSeconds(1), now.plusSeconds(2), good.expiresAt(), good.tokenExpiresAt()));
        denied(evidence(scope, 3, binding, good.authenticatedAt(), now.plusSeconds(1), good.expiresAt(), good.tokenExpiresAt()));
        denied(evidence(scope, 3, binding, good.authenticatedAt(), good.issuedAt(), now, good.tokenExpiresAt()));
        denied(evidence(scope, 3, binding, good.authenticatedAt(), good.issuedAt(), now.plusSeconds(301), now.plusSeconds(600)));
        denied(evidence(scope, 3, binding, good.authenticatedAt(), good.issuedAt(), good.expiresAt(), now.plusSeconds(30)));
        denied(evidence(scope, 3, binding, now.minusSeconds(5), now.minusSeconds(10), good.expiresAt(), good.tokenExpiresAt()));
    }
    @Test void deniesAbsentUnverifiedAndIncompleteEvidence() {
        when(providers.getIfUnique()).thenReturn(null);
        assertThrows(AccessDeniedException.class, this::require);
        when(providers.getIfUnique()).thenReturn(provider);
        when(provider.current()).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, this::require);
        denied(new StepUpEvidenceProvider.Evidence(good.grantId(), scope, 3, binding, null, good.authenticatedAt(), good.issuedAt(), good.expiresAt(), good.tokenExpiresAt()));
        denied(new StepUpEvidenceProvider.Evidence(null, scope, 3, binding, good.assurance(), good.authenticatedAt(), good.issuedAt(), good.expiresAt(), good.tokenExpiresAt()));
        denied(evidence(scope, 3, binding, null, good.issuedAt(), good.expiresAt(), good.tokenExpiresAt()));
    }
    @Test void rejectsNoncanonicalAndOverflowAmounts() {
        for (String value : List.of("-1", "+1", "01", "1.0", "9223372036854775808", "1e3", ""))
            assertThrows(IllegalArgumentException.class, () -> new StepUpService.Binding(binding.action(), binding.resourceId(), binding.operationId(), value));
        assertEquals("9223372036854775807", new StepUpService.Binding(binding.action(), binding.resourceId(), binding.operationId(), "9223372036854775807").amountXaf());
    }
}
