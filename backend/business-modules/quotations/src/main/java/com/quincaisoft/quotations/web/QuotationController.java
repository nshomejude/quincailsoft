package com.quincaisoft.quotations.web;import com.quincaisoft.quotations.api.QuotationService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1/quotations") public class QuotationController {
 private final QuotationService service;public QuotationController(QuotationService service){this.service=service;}
 @GetMapping @PreAuthorize("hasAnyAuthority('quotations:read','SCOPE_quotations:read')")public List<QuotationService.Quote> list(){return service.list();}
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('quotations:read','SCOPE_quotations:read')")public QuotationService.Quote get(@PathVariable UUID id){return service.get(id);}
 @PostMapping @PreAuthorize("hasAnyAuthority('quotations:write','SCOPE_quotations:write')")public QuotationService.Quote draft(@RequestHeader("Idempotency-Key")UUID key,@RequestBody QuotationService.Draft d){match(key,d.operationId());return service.draft(d);}
 @PostMapping("/{id}/issue") @PreAuthorize("hasAnyAuthority('quotations:issue','SCOPE_quotations:issue')")public QuotationService.Quote issue(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody QuotationService.Command c){match(key,c.operationId());return service.issue(id,c);}
 @PostMapping("/{id}/convert") @PreAuthorize("hasAnyAuthority('quotations:write','SCOPE_quotations:write')")public QuotationService.Quote convert(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody QuotationService.Command c){match(key,c.operationId());return service.convert(id,c);}
 private void match(UUID k,UUID op){if(!k.equals(op))throw new IllegalArgumentException("Idempotency-Key mismatch");}
 @GetMapping("/{id}/decisions") @PreAuthorize("hasAnyAuthority('quotations:read','SCOPE_quotations:read')")
 public List<QuotationService.CustomerDecision> decisions(@PathVariable UUID id){return service.decisions(id);}
 @PostMapping("/{id}/accept") @PreAuthorize("hasAnyAuthority('quotations:write','SCOPE_quotations:write')")
 public QuotationService.Quote accept(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody QuotationService.Decision input){match(key,input.operationId());return service.accept(id,input);}
 @PostMapping("/{id}/reject") @PreAuthorize("hasAnyAuthority('quotations:write','SCOPE_quotations:write')")
 public QuotationService.Quote reject(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody QuotationService.Decision input){match(key,input.operationId());return service.reject(id,input);}
}
