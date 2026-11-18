package com.quincaisoft.subscriptions.web;
import com.quincaisoft.subscriptions.api.SubscriptionQuotaService;
import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1/billing/capacity")
public class SubscriptionQuotaController {
 private final SubscriptionQuotaService service;public SubscriptionQuotaController(SubscriptionQuotaService service){this.service=service;}
 public record Release(UUID operationId){}
 @GetMapping @PreAuthorize("hasAnyAuthority('billing:read','SCOPE_billing:read')") public List<SubscriptionQuotaService.Usage> usage(){return service.usage();}
 @PostMapping("/reservations") @PreAuthorize("hasAnyAuthority('billing:write','SCOPE_billing:write')") public SubscriptionQuotaService.Reservation reserve(@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody SubscriptionQuotaService.Reserve input){same(operation,input.operationId());return service.reserve(input);}
 @GetMapping("/reservations/{id}") @PreAuthorize("hasAnyAuthority('billing:read','SCOPE_billing:read')") public SubscriptionQuotaService.Reservation get(@PathVariable UUID id){return service.get(id);}
 @PostMapping("/reservations/{id}/release") @PreAuthorize("hasAnyAuthority('billing:write','SCOPE_billing:write')") public SubscriptionQuotaService.Reservation release(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody Release input){same(operation,input.operationId());return service.release(operation,id);}
 @GetMapping("/reservations/{id}/committed") @PreAuthorize("hasAnyAuthority('billing:read','SCOPE_billing:read')") public SubscriptionQuotaService.Reservation committed(@PathVariable UUID id){return service.committed(id);}
 private static void same(UUID header,UUID body){if(!header.equals(body))throw new IllegalArgumentException("Operation mismatch");}
}
