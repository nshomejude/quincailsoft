package com.quincaisoft.security;

import com.quincaisoft.identity.api.IdentityService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentRoleAuthoritiesFilterTest {
    private static final UUID ACTOR = UUID.randomUUID();

    private JwtAuthenticationToken signed(String... permissions) {
        var jwt = Jwt.withTokenValue("signed-test-token").header("alg", "RS256")
            .subject(ACTOR.toString()).build();
        return new JwtAuthenticationToken(jwt,
            java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList());
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void cashierCannotReuseOwnerPermissionsFromUnexpiredToken() throws Exception {
        IdentityService service = mock(IdentityService.class);
        when(service.current()).thenReturn(new IdentityService.Member(ACTOR, "cashier", true, 2));
        SecurityContextHolder.getContext().setAuthentication(signed(
            "SCOPE_sales:read", "SCOPE_sales:post",
            "SCOPE_identity:manage", "SCOPE_finance:post", "platform:admin"));
        var filter = new CurrentRoleAuthoritiesFilter(service);
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {
            var actual = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet());
            assertEquals(Set.of("SCOPE_sales:read", "SCOPE_sales:post"), actual);
        });
        verify(service).current();
    }

    @Test void auditorNeverInheritsSignedWriteOrExportPermissions() throws Exception {
        var signed = List.of("SCOPE_sales:read", "SCOPE_sales:write",
            "SCOPE_reports:read", "SCOPE_reports:export").stream()
            .map(SimpleGrantedAuthority::new).toList();
        var effective = EffectiveRolePolicy.intersect("auditor", signed);
        assertEquals(Set.of("SCOPE_sales:read", "SCOPE_reports:read"),
            effective.stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test void ownerCannotInheritControlPlanePermission() {
        var signed = List.of(new SimpleGrantedAuthority("SCOPE_sales:post"),
            new SimpleGrantedAuthority("SCOPE_platform:admin"),
            new SimpleGrantedAuthority("ROLE_ADMIN"));
        var effective = EffectiveRolePolicy.intersect("owner", signed);
        assertEquals(Set.of("SCOPE_sales:post"), effective.stream().map(a -> a.getAuthority())
            .collect(java.util.stream.Collectors.toSet()));
    }

    @Test void revokedOrUnknownMembershipDoesNotReachController() throws Exception {
        IdentityService service = mock(IdentityService.class);
        when(service.current()).thenThrow(new AccessDeniedException("membership revoked"));
        SecurityContextHolder.getContext().setAuthentication(signed("SCOPE_sales:read"));
        var response = new MockHttpServletResponse();
        new CurrentRoleAuthoritiesFilter(service).doFilter(new MockHttpServletRequest(), response,
            (req,res) -> fail("Revoked actor cannot enter the application"));
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("MEMBERSHIP_REVOKED"));
        assertThrows(AccessDeniedException.class, () -> EffectiveRolePolicy.intersect(
            "unrecognized", List.of(new SimpleGrantedAuthority("SCOPE_sales:read"))));
    }

    @Test void anonymousPublicRequestBypassesMembershipLookup() throws Exception {
        IdentityService service = mock(IdentityService.class);
        final boolean[] invoked = {false};
        new CurrentRoleAuthoritiesFilter(service).doFilter(
            new MockHttpServletRequest(), new MockHttpServletResponse(), (req,res) -> invoked[0] = true);
        assertTrue(invoked[0]);
        verifyNoInteractions(service);
    }
}
