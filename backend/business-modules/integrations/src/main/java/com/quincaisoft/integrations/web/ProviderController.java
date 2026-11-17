package com.quincaisoft.integrations.web;
import com.quincaisoft.integrations.api.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1/mobile-money") public class ProviderController {
 private final ProviderRuntime runtime;private final MobileMoneyService service;public ProviderController(ProviderRuntime runtime,MobileMoneyService service){this.runtime=runtime;this.service=service;}
 @GetMapping("/providers") @PreAuthorize("hasAnyAuthority('tenders:read','SCOPE_tenders:read')") public List<ProviderStatus> status(){return List.of(new ProviderStatus("MTN",runtime.configured(),runtime.environment(),runtime.configured()?"Configured; provider acceptance remains required":"Merchant credentials or live activation absent"),new ProviderStatus("Orange",false,"unavailable","Subscribed Cameroon merchant transaction contract required"));}
 public record ProviderStatus(String provider,boolean enabled,String environment,String status){}

 @GetMapping("/intents") @PreAuthorize("hasAnyAuthority('tenders:read','SCOPE_tenders:read')") public List<MobileMoneyService.Intent> intents(){return service.list();}
 @PostMapping("/intents") @PreAuthorize("hasAnyAuthority('tenders:write','SCOPE_tenders:write')") public MobileMoneyService.Intent prepare(@RequestHeader("Idempotency-Key")String key,@RequestBody MobileMoneyService.Prepare p){if(!UUID.fromString(key).equals(p.operationId()))throw new IllegalArgumentException("Idempotency-Key mismatch");return service.prepare(p);}
 @PostMapping("/intents/{id}/reconcile") @PreAuthorize("hasAnyAuthority('tenders:write','SCOPE_tenders:write')") public MobileMoneyService.Intent query(@PathVariable UUID id){return service.reconcile(id);}
}
