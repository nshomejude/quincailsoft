package com.quincaisoft.suppliers.web;
import com.quincaisoft.suppliers.api.SupplierService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1/suppliers") public class SupplierController {
 private final SupplierService service;public SupplierController(SupplierService service){this.service=service;}
 @GetMapping @PreAuthorize("hasAnyAuthority('purchasing:read','SCOPE_purchasing:read')") public List<SupplierService.Supplier> list(){return service.list();}
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('purchasing:read','SCOPE_purchasing:read')") public SupplierService.Supplier get(@PathVariable UUID id){return service.get(id);}
 @PostMapping @PreAuthorize("hasAnyAuthority('purchasing:write','SCOPE_purchasing:write')") public SupplierService.Supplier create(@RequestHeader("Idempotency-Key") UUID key,@RequestHeader("X-Request-ID") UUID requestId,@RequestBody SupplierService.Create body){if(!key.equals(body.operationId()))throw new IllegalArgumentException("Idempotency-Key mismatch");return service.create(body);}
}