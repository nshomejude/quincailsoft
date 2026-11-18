package com.quincaisoft.subscriptions.web;

import com.quincaisoft.subscriptions.api.SubscriptionLifecycleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/admin/subscriptions")
public class SubscriptionLifecycleController {
    public record PlanCommand(UUID operationId,SubscriptionLifecycleService.Plan plan){}
    public record Transition(UUID operationId,long expectedVersion,String reason){}
    public record Approval(UUID operationId){}
    private final SubscriptionLifecycleService service;
    public SubscriptionLifecycleController(SubscriptionLifecycleService service){this.service=service;}
    @GetMapping("/plans") @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')")
    public List<SubscriptionLifecycleService.Plan> plans(){return service.plans();}
    @PostMapping("/plans") @PreAuthorize("hasAuthority('SCOPE_platform:billing:write')")
    public SubscriptionLifecycleService.Plan create(@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody PlanCommand command){same(operation,command.operationId());return service.createPlan(operation,command.plan());}
    @GetMapping("/{tenantId}") @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')")
    public SubscriptionLifecycleService.Snapshot get(@PathVariable UUID tenantId){return service.verifyTrial(tenantId);}
    @PostMapping("/{tenantId}/expire") @PreAuthorize("hasAuthority('SCOPE_platform:billing:write')")
    public SubscriptionLifecycleService.Snapshot expire(@PathVariable UUID tenantId,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody Transition command){same(operation,command.operationId());return service.expireTrial(operation,tenantId,command.expectedVersion());}
    @PostMapping("/{tenantId}/cancel") @PreAuthorize("hasAuthority('SCOPE_platform:billing:write')")
    public SubscriptionLifecycleService.Snapshot cancel(@PathVariable UUID tenantId,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody Transition command){same(operation,command.operationId());return service.cancel(operation,tenantId,command.expectedVersion(),command.reason());}
    @PostMapping("/notices") @PreAuthorize("hasAuthority('SCOPE_platform:billing:write')")
    public SubscriptionLifecycleService.NoticeView notice(@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody SubscriptionLifecycleService.Notice command){same(operation,command.operationId());return service.recordNotice(command);}
    @PostMapping("/notices/{id}/approve") @PreAuthorize("hasAuthority('SCOPE_platform:billing:approve')")
    public SubscriptionLifecycleService.NoticeView approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody Approval command){same(operation,command.operationId());return service.approveNotice(operation,id);}
    @GetMapping("/{tenantId}/notices") @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')")
    public List<SubscriptionLifecycleService.NoticeView> notices(@PathVariable UUID tenantId){return service.notices(tenantId);}
    @GetMapping("/{tenantId}/payments") @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')")
    public List<SubscriptionLifecycleService.PaymentView> payments(@PathVariable UUID tenantId){return service.payments(tenantId);}
    @PostMapping("/payments") @PreAuthorize("hasAuthority('SCOPE_platform:billing:write')")
    public SubscriptionLifecycleService.PaymentView payment(@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody SubscriptionLifecycleService.Payment command){same(operation,command.operationId());return service.recordPayment(command);}
    @PostMapping("/payments/{id}/approve") @PreAuthorize("hasAuthority('SCOPE_platform:billing:approve')")
    public SubscriptionLifecycleService.PaymentView approvePayment(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody Approval command){same(operation,command.operationId());return service.approvePayment(operation,id);}
    @PostMapping("/payments/{id}/activate") @PreAuthorize("hasAuthority('SCOPE_platform:billing:approve')")
    public SubscriptionLifecycleService.Snapshot activate(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody Approval command){same(operation,command.operationId());return service.activatePayment(operation,id);}
    private static void same(UUID header,UUID body){if(!header.equals(body))throw new IllegalArgumentException("Operation mismatch");}
}
