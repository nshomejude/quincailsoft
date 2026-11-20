package com.quincaisoft.platformcontrol.web;

import com.quincaisoft.platformcontrol.api.OnboardingConfigurationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/admin/onboarding/{id}/configuration")
public class OnboardingConfigurationController {
    private final OnboardingConfigurationService service;
    public OnboardingConfigurationController(OnboardingConfigurationService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('SCOPE_platform:tenants:read')")
    public OnboardingConfigurationService.Configuration get(@PathVariable UUID id){return service.get(id);}
    @PostMapping @PreAuthorize("hasAuthority('SCOPE_platform:tenants:write')")
    public OnboardingConfigurationService.Configuration configure(@PathVariable UUID id,@RequestHeader("Idempotency-Key") UUID operation,
        @RequestHeader("X-Request-ID") UUID correlation,@RequestBody OnboardingConfigurationService.Configure input){
        matches(operation,input.operationId());return service.configure(id,input);
    }
    @PostMapping("/verify-owner") @PreAuthorize("hasAuthority('SCOPE_platform:tenants:verify')")
    public OnboardingConfigurationService.Configuration verify(@PathVariable UUID id,@RequestHeader("Idempotency-Key") UUID operation,
        @RequestHeader("X-Request-ID") UUID correlation,@RequestBody OnboardingConfigurationService.VerifyIdentity input){
        matches(operation,input.operationId());return service.verifyIdentity(id,input);
    }
    @PostMapping("/approve") @PreAuthorize("hasAuthority('SCOPE_platform:tenants:approve')")
    public OnboardingConfigurationService.Configuration approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key") UUID operation,
        @RequestHeader("X-Request-ID") UUID correlation,@RequestBody OnboardingConfigurationService.Approval input){
        matches(operation,input.operationId());return service.approve(id,input);
    }
    private static void matches(UUID header,UUID body){if(!header.equals(body))throw new IllegalArgumentException("Operation mismatch");}
}
