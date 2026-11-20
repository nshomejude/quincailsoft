package com.quincaisoft.subscriptions.web;
import com.quincaisoft.subscriptions.api.SubscriptionBillingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import java.util.UUID;
@RestController
public class SubscriptionExportController {
 private final SubscriptionBillingService service;
 public SubscriptionExportController(SubscriptionBillingService service){this.service=service;}
 @GetMapping("/admin/billing/invoices/page") @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')")
 public SubscriptionBillingService.InvoicePage platformPage(@RequestParam UUID tenantId,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="50")int limit){return service.platformPage(tenantId,cursor,limit);}
 @GetMapping("/api/v1/billing/invoices/page") @PreAuthorize("hasAnyAuthority('billing:read','SCOPE_billing:read')")
 public SubscriptionBillingService.InvoicePage merchantPage(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="50")int limit){return service.merchantPage(cursor,limit);}
 @GetMapping(value="/admin/billing/invoices/export.csv",produces="text/csv") @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')")
 public ResponseEntity<String> platformExport(@RequestParam UUID tenantId,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="200")int limit){return csv(service.platformExport(tenantId,cursor,limit));}
 @GetMapping(value="/api/v1/billing/invoices/export.csv",produces="text/csv") @PreAuthorize("hasAnyAuthority('billing:read','SCOPE_billing:read')")
 public ResponseEntity<String> merchantExport(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="200")int limit){return csv(service.merchantExport(cursor,limit));}
 private static ResponseEntity<String> csv(SubscriptionBillingService.CsvPage page){var response=ResponseEntity.ok().header("Content-Type","text/csv; charset=UTF-8").header("Cache-Control","no-store, private").header("X-Content-Type-Options","nosniff").header("Content-Disposition","attachment; filename=subscription-invoices-page.csv").header("X-Export-Cutoff",page.cutoff().toString()).header("X-Current-Cursor",page.currentCursor());if(page.nextCursor()!=null)response.header("X-Next-Cursor",page.nextCursor());return response.body(page.content());}
}
