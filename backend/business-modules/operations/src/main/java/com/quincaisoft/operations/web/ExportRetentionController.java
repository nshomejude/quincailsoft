package com.quincaisoft.operations.web;
import com.quincaisoft.operations.api.ExportRetentionService;import java.util.UUID;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/operations/retention/exports")
public class ExportRetentionController {
 private final ExportRetentionService service;public ExportRetentionController(ExportRetentionService service){this.service=service;}
 @GetMapping("/{id}") public ExportRetentionService.Decision get(@PathVariable UUID id){return service.get(id);}
 @PostMapping public ExportRetentionService.Decision request(@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody ExportRetentionService.Request body){match(op,body.operationId());return service.request(body);}
 @PostMapping("/{id}/approve") public ExportRetentionService.Decision approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody ExportRetentionService.Command body){match(op,body.operationId());return service.approve(id,body);}
 @PostMapping("/{id}/execute") public ExportRetentionService.Decision execute(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody ExportRetentionService.Command body){match(op,body.operationId());return service.execute(id,body);}
 private static void match(UUID header,UUID body){if(!header.equals(body))throw new IllegalArgumentException("Operation mismatch");}
}
