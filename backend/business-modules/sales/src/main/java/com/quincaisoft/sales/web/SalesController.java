package com.quincaisoft.sales.web;
import com.quincaisoft.sales.api.SalesService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
@RestController @RequestMapping("/api/v1/sales")
public class SalesController {
 private final SalesService sales;
 public SalesController(SalesService sales){this.sales=sales;}
 @GetMapping @PreAuthorize("hasAnyAuthority('sales:read','SCOPE_sales:read')") public List<SalesService.Sale> list(){return sales.list();}
 @GetMapping("/summary")
 @PreAuthorize("hasAnyAuthority('sales:read','SCOPE_sales:read')")
 public SalesService.PostedSalesSummary summary(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
  return sales.postedSalesSummary(from, to);
 }
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('sales:read','SCOPE_sales:read')") public SalesService.Sale get(@PathVariable UUID id){return sales.get(id);}
 @PostMapping @PreAuthorize("hasAnyAuthority('sales:write','SCOPE_sales:write')") public SalesService.Sale draft(@RequestHeader("Idempotency-Key") UUID key,@RequestHeader("X-Request-ID") UUID requestId,@Valid @RequestBody SalesService.Draft body){match(key,body.operationId());return sales.draft(body);}
 @PostMapping("/{id}/post") @PreAuthorize("hasAnyAuthority('sales:post','SCOPE_sales:post')") public ResponseEntity<SalesService.Sale> post(@PathVariable UUID id,@RequestHeader("Idempotency-Key") UUID key,@RequestHeader("X-Request-ID") UUID requestId,@Valid @RequestBody SalesService.Post body){match(key,body.operationId());return ResponseEntity.status(HttpStatus.CREATED).body(sales.post(id,body));}
 private void match(UUID key,UUID operation){if(!key.equals(operation))throw new IllegalArgumentException("Idempotency-Key must match operationId");}
}
