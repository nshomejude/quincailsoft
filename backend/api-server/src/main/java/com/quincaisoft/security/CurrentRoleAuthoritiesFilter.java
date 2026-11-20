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
        try {
            var member = identities.current(); // validates active membership and epoch
            var intersection = EffectiveRolePolicy.intersect(
                member.role(), token.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(token.getToken(), intersection, token.getName()));
        } catch (AccessDeniedException | java.util.NoSuchElementException denied) {
            ApiErrors.write(request, response, 403, "MEMBERSHIP_REVOKED",
                "An active scoped membership and effective role are required");
            return;
        }
        chain.doFilter(request, response);
    }
}
