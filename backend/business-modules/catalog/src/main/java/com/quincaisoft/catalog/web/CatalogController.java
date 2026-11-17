package com.quincaisoft.catalog.web;
import com.quincaisoft.catalog.api.CatalogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;
@RestController @RequestMapping("/api/v1/products")
public class CatalogController {
 private final CatalogService catalog;
 public CatalogController(CatalogService catalog){this.catalog=catalog;}
 @GetMapping @PreAuthorize("hasAuthority('SCOPE_catalog:read')") public List<CatalogService.Product> list(){return catalog.list();}
 @GetMapping("/{id}") @PreAuthorize("hasAuthority('SCOPE_catalog:read')") public CatalogService.Product get(@PathVariable UUID id){return catalog.get(id);}
}
