package com.quincaisoft.catalog.web;
import com.quincaisoft.catalog.api.CatalogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;
@RestController @RequestMapping("/api/v1/products")
public class CatalogController {
 private final CatalogService catalog;
 public CatalogController(CatalogService catalog){this.catalog=catalog;}
 @GetMapping @PreAuthorize("hasAnyAuthority('catalog:read','SCOPE_catalog:read')") public List<CatalogService.Product> list(){return catalog.list();}
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('catalog:read','SCOPE_catalog:read')") public CatalogService.Product get(@PathVariable UUID id){return catalog.get(id);}
 @PostMapping @PreAuthorize("hasAnyAuthority('catalog:write','SCOPE_catalog:write')") public CatalogService.Product create(@RequestHeader("Idempotency-Key") UUID key,@RequestBody CatalogService.Save body){if(!key.equals(body.operationId()))throw new IllegalArgumentException("Operation ID mismatch");return catalog.create(body);}
 @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('catalog:write','SCOPE_catalog:write')") public CatalogService.Product update(@PathVariable UUID id,@RequestHeader("Idempotency-Key") UUID key,@RequestBody CatalogService.Save body){if(!key.equals(body.operationId()))throw new IllegalArgumentException("Operation ID mismatch");return catalog.update(id,body);}
}
