package com.quincaisoft.subscriptions.web;
import com.quincaisoft.subscriptions.api.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.UUID;
@RestController
public class SubscriptionPrintController {
    private final SubscriptionBillingService invoices;private final SubscriptionLifecycleService lifecycle;
    public SubscriptionPrintController(SubscriptionBillingService invoices,SubscriptionLifecycleService lifecycle){this.invoices=invoices;this.lifecycle=lifecycle;}
    @GetMapping(value="/admin/billing/invoices/{id}/print",produces=MediaType.TEXT_HTML_VALUE) @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')")
    public ResponseEntity<String> platformInvoice(@PathVariable UUID id,@RequestParam(defaultValue="en")String locale){return html(invoices.printInvoice(id,true,locale));}
    @GetMapping(value="/api/v1/billing/invoices/{id}/print",produces=MediaType.TEXT_HTML_VALUE) @PreAuthorize("hasAnyAuthority('billing:read','SCOPE_billing:read')")
    public ResponseEntity<String> merchantInvoice(@PathVariable UUID id,@RequestParam(defaultValue="en")String locale){return html(invoices.printInvoice(id,false,locale));}
    @GetMapping(value="/admin/subscriptions/payments/{id}/print",produces=MediaType.TEXT_HTML_VALUE) @PreAuthorize("hasAuthority('SCOPE_platform:billing:read')")
    public ResponseEntity<String> platformReceipt(@PathVariable UUID id,@RequestParam(defaultValue="en")String locale){return html(lifecycle.printReceipt(id,true,locale));}
    @GetMapping(value="/api/v1/billing/payments/{id}/print",produces=MediaType.TEXT_HTML_VALUE) @PreAuthorize("hasAnyAuthority('billing:read','SCOPE_billing:read')")
    public ResponseEntity<String> merchantReceipt(@PathVariable UUID id,@RequestParam(defaultValue="en")String locale){return html(lifecycle.printReceipt(id,false,locale));}
    private static ResponseEntity<String> html(String body){return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
        .header("Cache-Control","no-store, private").header("X-Content-Type-Options","nosniff")
        .header("Referrer-Policy","no-referrer")
        .header("Content-Security-Policy","default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'").body(body);}
}
