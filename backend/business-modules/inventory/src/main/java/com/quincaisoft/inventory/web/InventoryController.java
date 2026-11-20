package com.quincaisoft.inventory.web;
import com.quincaisoft.inventory.api.InventoryWorkflows;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1")
public class InventoryController {
 private final InventoryWorkflows service;public InventoryController(InventoryWorkflows service){this.service=service;}
 @GetMapping("/warehouses") @PreAuthorize("hasAnyAuthority('inventory:read','SCOPE_inventory:read','sales:write','SCOPE_sales:write','purchasing:write','SCOPE_purchasing:write','purchasing:receive','SCOPE_purchasing:receive')") public List<InventoryWorkflows.Warehouse> warehouses(){return service.warehouses();}
 public record LowStockSummary(long productCount) {}

 @GetMapping("/inventory/low-stock/summary")
 @PreAuthorize("hasAnyAuthority('inventory:read','SCOPE_inventory:read')")
 public LowStockSummary lowStockSummary() {
  return new LowStockSummary(service.lowStockCount());
 }
 @GetMapping("/inventory/balances") @PreAuthorize("hasAnyAuthority('inventory:read','SCOPE_inventory:read')") public List<InventoryWorkflows.Balance> balances(@RequestParam UUID warehouseId){return service.balances(warehouseId);}
 @GetMapping("/inventory/transfers") @PreAuthorize("hasAnyAuthority('inventory:read','SCOPE_inventory:read')") public List<InventoryWorkflows.Transfer> transfers(){return service.transfers();}
 @GetMapping("/inventory/transfers/{id}") @PreAuthorize("hasAnyAuthority('inventory:read','SCOPE_inventory:read')") public InventoryWorkflows.Transfer transfer(@PathVariable UUID id){return service.get(id);}
 @PostMapping("/inventory/transfers") @PreAuthorize("hasAnyAuthority('inventory:write','SCOPE_inventory:write')") public InventoryWorkflows.Transfer draft(@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryWorkflows.Draft c){match(key,c.operationId());return service.draft(c);}
 @PostMapping("/inventory/transfers/{id}/approve") @PreAuthorize("hasAnyAuthority('inventory:approve','SCOPE_inventory:approve')") public InventoryWorkflows.Transfer approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryWorkflows.Command c){match(key,c.operationId());return service.approve(id,c);}
 @PostMapping("/inventory/transfers/{id}/dispatch") @PreAuthorize("hasAnyAuthority('inventory:write','SCOPE_inventory:write')") public InventoryWorkflows.Transfer dispatch(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryWorkflows.Command c){match(key,c.operationId());return service.dispatch(id,c);}
 @PostMapping("/inventory/transfers/{id}/receive") @PreAuthorize("hasAnyAuthority('inventory:write','SCOPE_inventory:write')") public InventoryWorkflows.Transfer receive(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryWorkflows.Command c){match(key,c.operationId());return service.receive(id,c);}
 private void match(UUID a,UUID b){if(!a.equals(b))throw new IllegalArgumentException("Operation ID mismatch");}
}
