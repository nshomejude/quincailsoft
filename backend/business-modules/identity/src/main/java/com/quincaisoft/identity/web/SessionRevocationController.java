package com.quincaisoft.identity.web;
import com.quincaisoft.identity.api.IdentityService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/identity/sessions") @PreAuthorize("isAuthenticated()")
public class SessionRevocationController {
 private final IdentityService identity;public SessionRevocationController(IdentityService identity){this.identity=identity;}
 @PostMapping("/revoke")public IdentityService.Revocation revoke(@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody IdentityService.Revoke body){if(!operation.equals(body.operationId()))throw new IllegalArgumentException("Operation mismatch");return identity.revoke(body);}
}
