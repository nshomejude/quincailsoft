package com.quincaisoft.identity.web;

import com.quincaisoft.identity.api.IdentityService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Preserve the existing HTTP contract. Authentication is required for self
 * revocation; IdentityService separately enforces scoped database membership,
 * owner-only revocation of other actors, fencing and durable replay.
 */
@RestController
@RequestMapping("/api/v1/identity/sessions")
@PreAuthorize("isAuthenticated()")
public final class SessionRevocationController {
    private final IdentityService identity;

    public SessionRevocationController(IdentityService identity) {
        this.identity = identity;
    }

    @PostMapping("/revoke")
    public IdentityService.Revocation revoke(
            @RequestHeader("Idempotency-Key") UUID operation,
            @RequestHeader("X-Request-ID") UUID requestId,
            @RequestBody IdentityService.Revoke body) {
        if (operation == null || requestId == null || body == null
                || body.operationId() == null || !operation.equals(body.operationId())) {
            throw new IllegalArgumentException("Valid request ID and matching operation IDs are required");
        }
        return identity.revoke(body);
    }
}
