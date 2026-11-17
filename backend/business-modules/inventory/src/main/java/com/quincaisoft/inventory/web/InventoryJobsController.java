package com.quincaisoft.inventory.web;
import com.quincaisoft.inventory.api.InventoryJobs;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1/inventory/jobs")
public class InventoryJobsController {
 private final InventoryJobs jobs;public InventoryJobsController(InventoryJobs jobs){this.jobs=jobs;}
 @GetMapping @PreAuthorize("hasAnyAuthority('inventory:read','SCOPE_inventory:read')")public List<InventoryJobs.Job> list(@RequestParam String kind){return jobs.list(kind);}
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('inventory:read','SCOPE_inventory:read')")public InventoryJobs.Job get(@PathVariable UUID id){return jobs.get(id);}
 @PostMapping("/counts") @PreAuthorize("hasAnyAuthority('inventory:write','SCOPE_inventory:write')")public InventoryJobs.Job start(@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryJobs.CountStart c){match(key,c.operationId());return jobs.startCount(c);}
 @PostMapping("/transforms/{kind}") @PreAuthorize("hasAnyAuthority('inventory:write','SCOPE_inventory:write')")public InventoryJobs.Job draft(@PathVariable String kind,@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryJobs.Transform c){match(key,c.operationId());return jobs.draftTransform(kind,c);}
 @PostMapping("/{id}/capture") @PreAuthorize("hasAnyAuthority('inventory:write','SCOPE_inventory:write')")public InventoryJobs.Job capture(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryJobs.Action c){match(key,c.operationId());return jobs.capture(id,c);}
 @PostMapping("/{id}/approve") @PreAuthorize("hasAnyAuthority('inventory:approve','SCOPE_inventory:approve')")public InventoryJobs.Job approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryJobs.Action c){match(key,c.operationId());return jobs.approve(id,c);}
 @PostMapping("/{id}/post") @PreAuthorize("hasAnyAuthority('inventory:approve','SCOPE_inventory:approve')")public InventoryJobs.Job post(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody InventoryJobs.Action c){match(key,c.operationId());return jobs.post(id,c);}
 private void match(UUID a,UUID b){if(!a.equals(b))throw new IllegalArgumentException("Operation ID mismatch");}
}
