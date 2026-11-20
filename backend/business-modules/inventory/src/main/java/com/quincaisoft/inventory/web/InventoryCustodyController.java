package com.quincaisoft.inventory.web;
import com.quincaisoft.inventory.api.InventoryCustody;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1/inventory") public class InventoryCustodyController {
private final InventoryCustody custody;public InventoryCustodyController(InventoryCustody custody){this.custody=custody;}
@GetMapping("/custody") @PreAuthorize("hasAnyAuthority('inventory:read','SCOPE_inventory:read')") public List<InventoryCustody.Position> positions(){return custody.positions();}
@GetMapping("/custody/{id}/collections") @PreAuthorize("hasAnyAuthority('inventory:read','SCOPE_inventory:read')") public List<InventoryCustody.Collection> collections(@PathVariable UUID id){return custody.collections(id);}
@PostMapping("/custody/{id}/collect") @PreAuthorize("hasAnyAuthority('inventory:write','SCOPE_inventory:write')") public InventoryCustody.Position collect(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryCustody.Collect c){match(key,c.operationId());return custody.collect(id,c);}
@GetMapping("/recognition-policies") @PreAuthorize("hasAnyAuthority('inventory:read','SCOPE_inventory:read')") public List<InventoryCustody.Policy> policies(){return custody.policies();}
@PostMapping("/recognition-policies") @PreAuthorize("hasAnyAuthority('finance:write','SCOPE_finance:write')") public InventoryCustody.Policy request(@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryCustody.PolicyRequest c){match(key,c.operationId());return custody.requestPolicy(c);}
@PostMapping("/recognition-policies/{id}/approve") @PreAuthorize("hasAnyAuthority('finance:approve','SCOPE_finance:approve')") public InventoryCustody.Policy approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryCustody.Approve c){match(key,c.operationId());return custody.approvePolicy(id,c);}
private static void match(UUID a,UUID b){if(!a.equals(b))throw new IllegalArgumentException("Operation ID mismatch");}
}
