package com.quincaisoft.subscriptions.web;

import com.quincaisoft.subscriptions.api.SubscriptionBillingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.UUID;

@RestController
public class SubscriptionBillingController {
    private final SubscriptionBillingService service;
    public SubscriptionBillingController(SubscriptionBillingService service){this.service=service;}
    @PostMapping("/admin/billing/invoices")
    @PreAuthorize("hasAuthority('SCOPE_platform:billing:write')")
    public SubscriptionBillingService.Invoice issue(@RequestHeader("Idempotency-Key") UUID operation,
        @RequestHeader("X-Request-ID") UUID request,@RequestBody SubscriptionBillingService.Issue input) {
        if(input==null||!operation.equals(input.operationId()))throw new IllegalArgumentException("Operation mismatch");
        return service.issue(request,input);
    }
    @GetMapping("/admin/billing/invoices")
    @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')")
    public List<SubscriptionBillingService.Invoice> platform(@RequestParam UUID tenantId){return service.platformInvoices(tenantId);}
    @GetMapping("/api/v1/billing/invoices")
    @PreAuthorize("hasAnyAuthority('billing:read','SCOPE_billing:read')")
    public List<SubscriptionBillingService.Invoice> merchant(){return service.merchantInvoices();}
}
