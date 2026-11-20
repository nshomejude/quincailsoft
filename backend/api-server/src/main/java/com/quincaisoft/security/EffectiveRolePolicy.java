package com.quincaisoft.security;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Intersects signed OIDC capabilities with the current database membership role.
 *
 * This does not issue new permissions. A valid, unexpired token is still
 * necessary, and each request must revalidate membership and authorization
 * epoch against PostgreSQL. Control-plane privileges are never merchant roles.
 */
public final class EffectiveRolePolicy {
    private EffectiveRolePolicy() {}

    private static final Map<String, Set<String>> ROLE_CAPABILITIES = Map.of(
        "manager", Set.of(
            "company:read", "branch:read", "branch:write",
            "catalog:read", "catalog:write", "inventory:read", "inventory:write",
            "inventory:approve", "purchasing:read", "purchasing:write",
            "purchasing:receive", "purchasing:approve", "suppliers:read",
            "suppliers:write", "sales:read", "sales:write", "sales:post",
            "quotations:read", "quotations:write", "quotations:issue",
            "returns:read", "returns:write", "returns:approve", "returns:post",
            "tenders:read", "tenders:write", "tenders:close",
            "finance:read", "expenses:read", "expenses:write",
            "documents:read", "documents:write", "documents:print",
            "reporting:read", "reports:read", "audit:read",
            "customers:read", "customers:write", "fulfillments:read",
            "fulfillments:write", "fulfillments:dispatch", "fulfillments:complete"),
        "finance", Set.of(
            "finance:read", "finance:write", "finance:post", "finance:approve",
            "tenders:read", "tenders:write", "tenders:approve", "tenders:close",
            "expenses:read", "expenses:write", "expenses:approve",
            "bank:read", "bank:write", "bank:approve", "journals:read",
            "journals:write", "journals:approve", "sales:read",
            "returns:read", "customers:read", "documents:read",
            "reporting:read", "reports:read", "audit:read"),
        "inventory", Set.of(
            "catalog:read", "inventory:read", "inventory:write",
            "inventory:approve", "inventory:adjust", "inventory:count",
            "inventory:transfer", "inventory:receive", "purchasing:read",
            "purchasing:receive", "suppliers:read", "sales:read",
            "fulfillments:read", "fulfillments:write",
            "fulfillments:dispatch", "fulfillments:complete",
            "documents:read", "documents:print", "reports:read"),
        "purchasing", Set.of(
            "catalog:read", "inventory:read", "purchasing:read",
            "purchasing:write", "purchasing:receive", "purchasing:approve",
            "suppliers:read", "suppliers:write", "documents:read",
            "documents:write", "documents:print", "reports:read"),
        "cashier", Set.of(
            "catalog:read", "inventory:read", "sales:read",
            "sales:write", "sales:post", "tenders:read", "tenders:write",
            "tenders:close", "returns:read", "returns:write",
            "customers:read", "quotations:read", "quotations:write",
            "documents:read", "documents:print", "fulfillments:read"),
        "sales", Set.of(
            "catalog:read", "inventory:read", "sales:read", "sales:write",
            "quotations:read", "quotations:write", "quotations:issue",
            "customers:read", "customers:write", "documents:read",
            "documents:print", "fulfillments:read"),
        "delivery", Set.of(
            "catalog:read", "inventory:read", "sales:read",
            "fulfillments:read", "fulfillments:write",
            "fulfillments:dispatch", "fulfillments:complete",
            "documents:read", "documents:print"),
        "auditor", Set.of(
            "company:read", "branch:read", "catalog:read",
            "inventory:read", "purchasing:read", "suppliers:read",
            "sales:read", "quotations:read", "returns:read",
            "tenders:read", "finance:read", "expenses:read",
            "customers:read", "documents:read", "reporting:read",
            "reports:read", "audit:read", "subscriptions:read"));

    public static Set<GrantedAuthority> intersect(String databaseRole, 
            java.util.Collection<? extends GrantedAuthority> signed) {
        if (databaseRole == null || signed == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Current membership role and signed authorities required");
        }
        if (!"owner".equals(databaseRole) && !ROLE_CAPABILITIES.containsKey(databaseRole)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Unknown or unsupported business role");
        }
        return signed.stream()
            .map(GrantedAuthority::getAuthority)
            .filter(v -> v != null && !v.isBlank())
            .filter(v -> {
                String capability = v.startsWith("SCOPE_") ? v.substring(6) : v;
                // Platform identity is a completely separate trust domain.
                if (!capability.matches("[a-z][a-z0-9-]*:[a-z][a-z0-9-]*")
                    || capability.startsWith("platform:")
                    || capability.startsWith("support:")) return false;
                return "owner".equals(databaseRole)
                    || ROLE_CAPABILITIES.get(databaseRole).contains(capability);
            })
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toUnmodifiableSet());
    }
}
