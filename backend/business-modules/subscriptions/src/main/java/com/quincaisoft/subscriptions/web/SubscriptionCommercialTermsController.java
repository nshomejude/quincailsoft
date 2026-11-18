package com.quincaisoft.subscriptions.web;
import com.quincaisoft.subscriptions.api.SubscriptionCommercialTermsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;
@RestController @RequestMapping("/admin/subscriptions")
public class SubscriptionCommercialTermsController {
 public record Approval(UUID operationId){}
 private final SubscriptionCommercialTermsService service;
 public SubscriptionCommercialTermsController(SubscriptionCommercialTermsService service){this.service=service;}
 @PostMapping("/commercial-terms") @PreAuthorize("hasAuthority('SCOPE_platform:billing:write')")
 public SubscriptionCommercialTermsService.TermView request(@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody SubscriptionCommercialTermsService.Request input){if(!operation.equals(input.operationId()))throw new IllegalArgumentException("Operation mismatch");return service.request(input);}
 @PostMapping("/commercial-terms/{id}/approve") @PreAuthorize("hasAuthority('SCOPE_platform:billing:approve')")
 public SubscriptionCommercialTermsService.TermView approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody Approval input){if(!operation.equals(input.operationId()))throw new IllegalArgumentException("Operation mismatch");return service.approve(operation,id);}
 @GetMapping("/{tenantId}/commercial-terms") @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')")
 public List<SubscriptionCommercialTermsService.TermView> list(@PathVariable UUID tenantId){return service.list(tenantId);}
}
