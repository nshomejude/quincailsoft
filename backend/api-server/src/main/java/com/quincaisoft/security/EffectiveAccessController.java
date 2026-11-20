package com.quincaisoft.security;

import com.quincaisoft.identity.api.IdentityService;
import com.quincaisoft.shared.TenantContext;
import com.quincaisoft.shared.TenantScope;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only access contract for role-directed web and mobile navigation. */
@RestController
public class EffectiveAccessController {
    private final IdentityService identity;

    public record Access(TenantScope scope, String role, long authorizationEpoch,
                         List<String> permissions) {}

    public EffectiveAccessController(IdentityService identity) {
        this.identity = identity;
    }

    @GetMapping("/api/v1/access/effective")
    public Access effective(Authentication authentication) {
        var member = identity.current();
        var permissions = authentication.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .filter(a -> a.startsWith("SCOPE_") || a.indexOf(':') > 0)
            .map(a -> a.startsWith("SCOPE_") ? a.substring(6) : a)
            .distinct().sorted().toList();
        return new Access(TenantContext.current(), member.role(),
            member.authorizationEpoch(), permissions);
    }
}
