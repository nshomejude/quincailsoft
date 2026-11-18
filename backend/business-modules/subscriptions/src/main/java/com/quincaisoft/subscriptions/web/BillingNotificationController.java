package com.quincaisoft.subscriptions.web;
import com.quincaisoft.subscriptions.api.BillingNotificationDispatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.*;
@RestController @RequestMapping("/admin/billing/notifications") @ConditionalOnProperty(name="quincai.billing.notifications.enabled",havingValue="true")
public class BillingNotificationController {
 public record Redrive(UUID operationId,String reason){}
 private final BillingNotificationDispatcher dispatcher;
 public BillingNotificationController(BillingNotificationDispatcher dispatcher){this.dispatcher=dispatcher;}
 @GetMapping @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')") public List<BillingNotificationDispatcher.Delivery> list(@RequestParam(required=false)String status){return dispatcher.list(status);}
 @GetMapping("/{id}/attempts") @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')") public List<BillingNotificationDispatcher.Attempt> attempts(@PathVariable UUID id){return dispatcher.attempts(id);}
 @PostMapping("/{id}/redrive") @PreAuthorize("hasAuthority('SCOPE_platform:billing:dispatch')") public BillingNotificationDispatcher.Delivery redrive(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody Redrive input){if(!operation.equals(input.operationId()))throw new IllegalArgumentException("Operation mismatch");return dispatcher.redrive(operation,id,input.reason());}
}
