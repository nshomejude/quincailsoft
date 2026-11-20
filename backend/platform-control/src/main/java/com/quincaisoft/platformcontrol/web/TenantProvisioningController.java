package com.quincaisoft.platformcontrol.web;
import com.quincaisoft.platformcontrol.api.TenantProvisioningService;
import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.UUID;
@RestController @RequestMapping("/admin/onboarding/{id}/provision-trial")
public class TenantProvisioningController {
 private final TenantProvisioningService service;
 public TenantProvisioningController(TenantProvisioningService service){this.service=service;}
 @GetMapping @PreAuthorize("hasAuthority('SCOPE_platform:tenants:read')")
 public TenantProvisioningService.Result get(@PathVariable UUID id){return service.get(id);}
 @PostMapping @PreAuthorize("hasAuthority('SCOPE_platform:tenants:provision')")
 public TenantProvisioningService.Result provision(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,
   @RequestHeader("X-Request-ID")UUID correlation,@RequestBody TenantProvisioningService.Trial input){
  if(!operation.equals(input.operationId()))throw new IllegalArgumentException("Operation mismatch");return service.provisionTrial(id,correlation,input);
 }
}
