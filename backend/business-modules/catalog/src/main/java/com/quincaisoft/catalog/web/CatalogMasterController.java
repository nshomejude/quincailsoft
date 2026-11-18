package com.quincaisoft.catalog.web;
import com.quincaisoft.catalog.api.CatalogMasterService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1/catalog")
public class CatalogMasterController {
 private final CatalogMasterService masters;public CatalogMasterController(CatalogMasterService masters){this.masters=masters;}
 @GetMapping("/categories") @PreAuthorize("hasAnyAuthority('catalog:read','SCOPE_catalog:read')") public List<CatalogMasterService.Category> categories(){return masters.categories();}
 @GetMapping("/categories/{id}") @PreAuthorize("hasAnyAuthority('catalog:read','SCOPE_catalog:read')") public CatalogMasterService.Category category(@PathVariable UUID id){return masters.category(id);}
 @PostMapping("/categories") @PreAuthorize("hasAnyAuthority('catalog:write','SCOPE_catalog:write')") public CatalogMasterService.Category createCategory(@RequestHeader("Idempotency-Key") UUID key,@RequestBody CatalogMasterService.CategorySave c){match(key,c.operationId());return masters.saveCategory(null,c);}
 @PutMapping("/categories/{id}") @PreAuthorize("hasAnyAuthority('catalog:write','SCOPE_catalog:write')") public CatalogMasterService.Category updateCategory(@PathVariable UUID id,@RequestHeader("Idempotency-Key") UUID key,@RequestBody CatalogMasterService.CategorySave c){match(key,c.operationId());return masters.saveCategory(id,c);}
 @GetMapping("/units") @PreAuthorize("hasAnyAuthority('catalog:read','SCOPE_catalog:read')") public List<CatalogMasterService.Unit> units(){return masters.units();}
 @GetMapping("/units/{id}") @PreAuthorize("hasAnyAuthority('catalog:read','SCOPE_catalog:read')") public CatalogMasterService.Unit unit(@PathVariable UUID id){return masters.unit(id);}
 @PostMapping("/units") @PreAuthorize("hasAnyAuthority('catalog:write','SCOPE_catalog:write')") public CatalogMasterService.Unit createUnit(@RequestHeader("Idempotency-Key") UUID key,@RequestBody CatalogMasterService.UnitSave c){match(key,c.operationId());return masters.saveUnit(null,c);}
 @PutMapping("/units/{id}") @PreAuthorize("hasAnyAuthority('catalog:write','SCOPE_catalog:write')") public CatalogMasterService.Unit updateUnit(@PathVariable UUID id,@RequestHeader("Idempotency-Key") UUID key,@RequestBody CatalogMasterService.UnitSave c){match(key,c.operationId());return masters.saveUnit(id,c);}
 @PostMapping("/manufacturers")
 @PreAuthorize("hasAnyAuthority('catalog:write','SCOPE_catalog:write')")
 public CatalogMasterService.Manufacturer createManufacturer(@RequestHeader("Idempotency-Key") UUID key,
     @RequestBody CatalogMasterService.ManufacturerSave command) {
  match(key, command.operationId());
  return masters.createManufacturer(command);
 }

 @PostMapping("/families")
 @PreAuthorize("hasAnyAuthority('catalog:write','SCOPE_catalog:write')")
 public CatalogMasterService.Family createFamily(@RequestHeader("Idempotency-Key") UUID key,
     @RequestBody CatalogMasterService.FamilySave command) {
  match(key, command.operationId());
  return masters.createFamily(command);
 }

 @PostMapping("/variants")
 @PreAuthorize("hasAnyAuthority('catalog:write','SCOPE_catalog:write')")
 public CatalogMasterService.Variant attachVariant(@RequestHeader("Idempotency-Key") UUID key,
     @RequestBody CatalogMasterService.VariantSave command) {
  match(key, command.operationId());
  return masters.attachVariant(command);
 }

 @PostMapping("/supplier-offers")
 @PreAuthorize("hasAnyAuthority('catalog:write','SCOPE_catalog:write')")
 public CatalogMasterService.Offer createOffer(@RequestHeader("Idempotency-Key") UUID key,
     @RequestBody CatalogMasterService.OfferSave command) {
  match(key, command.operationId());
  return masters.createOffer(command);
 }

 private void match(UUID key,UUID operation){if(!key.equals(operation))throw new IllegalArgumentException("Operation ID mismatch");}
}
