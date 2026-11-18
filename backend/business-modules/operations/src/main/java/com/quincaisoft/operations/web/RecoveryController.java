package com.quincaisoft.operations.web;
import com.quincaisoft.operations.api.RecoveryService;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/v1/operations/recovery")
public class RecoveryController {
 private final RecoveryService service;public RecoveryController(RecoveryService service){this.service=service;}
 @GetMapping("/plans") public List<RecoveryService.Plan> list(@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="25")int limit){return service.list(offset,limit);}
 @GetMapping("/plans/{id}") public RecoveryService.Plan get(@PathVariable UUID id){return service.get(id);}
 @PostMapping("/plans") public RecoveryService.Plan request(@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody RecoveryService.Request body){match(op,body.operationId());return service.request(body);}
 @PostMapping("/plans/{id}/approve-scope") public RecoveryService.Plan approveScope(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody RecoveryService.Review body){match(op,body.operationId());return service.approveScope(id,body);}
 @GetMapping("/retained-receipts") public org.springframework.http.ResponseEntity<RecoveryService.ReceiptPage> receipts(@RequestParam Instant asOf,@RequestParam(required=false)UUID after,@RequestParam(defaultValue="100")int limit){return org.springframework.http.ResponseEntity.ok().header("Cache-Control","no-store").body(service.retainedReceipts(asOf,after,limit));}
 @PostMapping("/plans/{id}/candidate-batches") public RecoveryService.Plan batch(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody RecoveryService.Batch body){match(op,body.operationId());return service.append(id,body);}
 @PostMapping("/plans/{id}/compare") public RecoveryService.Plan compare(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody RecoveryService.Compare body){match(op,body.operationId());return service.compare(id,body);}
 @GetMapping("/plans/{id}/differences") public List<RecoveryService.Difference> differences(@PathVariable UUID id,@RequestParam(required=false)UUID after,@RequestParam(defaultValue="100")int limit){return service.differences(id,after,limit);}
 @PostMapping("/plans/{id}/approve-receipt-review") public RecoveryService.Plan approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody RecoveryService.Review body){match(op,body.operationId());return service.approveReceiptReview(id,body);}
 @PostMapping("/plans/{id}/cancel") public RecoveryService.Plan cancel(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody RecoveryService.Review body){match(op,body.operationId());return service.cancel(id,body);}
 private static void match(UUID supplied,UUID body){if(!supplied.equals(body))throw new IllegalArgumentException("Operation mismatch");}
}
