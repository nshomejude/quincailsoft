package com.quincaisoft.sales.web;
import com.quincaisoft.sales.api.FulfillmentService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1/fulfillments")public class FulfillmentController{
 private final FulfillmentService service;public FulfillmentController(FulfillmentService service){this.service=service;}
 @GetMapping @PreAuthorize("hasAnyAuthority('sales:read','SCOPE_sales:read')")public List<FulfillmentService.Fulfillment> list(){return service.list();}
 public record PreparationSummary(long pendingCount) {}

 @GetMapping("/page")
 @PreAuthorize("hasAnyAuthority('sales:read','SCOPE_sales:read')")
 public FulfillmentService.QueuePage page(@RequestParam(defaultValue="PENDING") String status,
     @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="25") int limit,
     @RequestParam(required=false) String cursor) {
  return service.page(status,q,limit,cursor);
 }

 @GetMapping("/preparation-summary")
 @PreAuthorize("hasAnyAuthority('sales:read','SCOPE_sales:read')")
 public PreparationSummary preparationSummary() {
  return new PreparationSummary(service.pendingPreparationCount());
 }
 @GetMapping("/{id}/picklist")
 @PreAuthorize("hasAnyAuthority('sales:read','SCOPE_sales:read')")
 public FulfillmentService.PickList pickList(@PathVariable UUID id) {
  return service.pickList(id);
 }
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('sales:read','SCOPE_sales:read')")public FulfillmentService.Fulfillment get(@PathVariable UUID id){return service.get(id);}
 @PostMapping @PreAuthorize("hasAnyAuthority('sales:write','SCOPE_sales:write')")public FulfillmentService.Fulfillment draft(@RequestHeader("Idempotency-Key")UUID key,@RequestBody FulfillmentService.Draft c){match(key,c.operationId());return service.draft(c);}
 @GetMapping("/{id}/dispatches")
 @PreAuthorize("hasAnyAuthority('sales:read','SCOPE_sales:read')")
 public List<FulfillmentService.DispatchEntry> dispatchHistory(@PathVariable UUID id) {
  return service.dispatchHistory(id);
 }
 @PostMapping("/{id}/dispatch-lines")
 @PreAuthorize("hasAnyAuthority('sales:write','SCOPE_sales:write')")
 public FulfillmentService.Fulfillment dispatchLines(@PathVariable UUID id,
     @RequestHeader("Idempotency-Key") UUID key,@RequestBody FulfillmentService.DispatchLines command) {
  match(key,command.operationId());
  return service.dispatchLines(id,command);
 }
 @PostMapping("/{id}/dispatch") @PreAuthorize("hasAnyAuthority('sales:write','SCOPE_sales:write')")public FulfillmentService.Fulfillment dispatch(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody FulfillmentService.Action c){match(key,c.operationId());return service.dispatch(id,c);}
 @PostMapping("/{id}/complete") @PreAuthorize("hasAnyAuthority('sales:write','SCOPE_sales:write')")public FulfillmentService.Fulfillment complete(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody FulfillmentService.Action c){match(key,c.operationId());return service.complete(id,c);}
 private static void match(UUID a,UUID b){if(!a.equals(b))throw new IllegalArgumentException("Operation ID mismatch");}
}
