package com.quincaisoft.security;

import com.quincaisoft.identity.api.IdentityService;
import com.quincaisoft.shared.TenantContext;
import com.quincaisoft.shared.TenantScope;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EffectiveAccessControllerTest {
    @Test void returnsOnlyAlreadyFilteredEffectiveAuthoritiesAndLiveMembershipEpoch() throws Exception {
        var identity = mock(IdentityService.class);
        var actor = UUID.randomUUID();
        when(identity.current()).thenReturn(new IdentityService.Member(actor, "cashier", true, 8));
        var authentication = new UsernamePasswordAuthenticationToken("actor", "unused", List.of(
            new SimpleGrantedAuthority("SCOPE_sales:read"),
            new SimpleGrantedAuthority("SCOPE_documents:print")));
        var scope = new TenantScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), actor);
        TenantContext.call(scope, 8, () -> {
            var view = new EffectiveAccessController(identity).effective(authentication);
            assertEquals(scope, view.scope());
            assertEquals("cashier", view.role());
            assertEquals(8L, view.authorizationEpoch());
            assertEquals(List.of("documents:print", "sales:read"), view.permissions());
            return null;
        });
        verify(identity).current();
    }
}
