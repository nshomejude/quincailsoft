package com.quincaisoft.security;

import com.quincaisoft.identity.api.IdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs after TenantScopeFilter establishes the signed tenant/company/branch.
 * Loads live membership/role with scoped PostgreSQL RLS on every JWT request.
 * Replaces token privileges with their intersection with the current role.
 */
public final class CurrentRoleAuthoritiesFilter extends OncePerRequestFilter {
    private final IdentityService identities;

    public CurrentRoleAuthoritiesFilter(IdentityService identities) {
        this.identities = identities;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            chain.doFilter(request, response);
            return;
        }
        IdentityService.Member member;
        try {
            member = identities.current(); // checks live membership and epoch
        } catch (AccessDeniedException | java.util.NoSuchElementException revoked) {
            ApiErrors.write(request, response, 401, "SESSION_REVOKED",
                "Current session is no longer valid; authenticate again");
            return;
        }
        try {
            var intersection = EffectiveRolePolicy.intersect(
                member.role(), token.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(token.getToken(), intersection, token.getName()));
        } catch (AccessDeniedException denied) {
            ApiErrors.write(request, response, 403, "ROLE_NOT_PERMITTED",
                "Current business role is not authorized");
            return;
        }
        chain.doFilter(request, response);
    }
}
