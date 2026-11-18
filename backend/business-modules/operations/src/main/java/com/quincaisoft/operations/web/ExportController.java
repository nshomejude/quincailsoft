package com.quincaisoft.operations.web;

import com.quincaisoft.operations.api.ExportService;
import com.quincaisoft.operations.api.OperationsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/operations/exports")
public class ExportController {
 private final ExportService service;
 public ExportController(ExportService service){this.service=service;}
 @GetMapping public List<OperationsService.Export> list(){return service.list();}
 @GetMapping("/{id}") public OperationsService.Export get(@PathVariable UUID id){return service.get(id);}
 @PostMapping public OperationsService.Export request(@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody OperationsService.ExportRequest body){match(operation,body.operationId());return service.request(body);}
 @PostMapping("/{id}/run") public OperationsService.Export run(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody OperationsService.Command body){match(operation,body.operationId());return service.run(id,body);}
 @PostMapping("/{id}/cancel") public OperationsService.Export cancel(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID correlation,@RequestBody OperationsService.Command body){match(operation,body.operationId());return service.cancel(id,body);}
 @GetMapping(value="/{id}/content",produces="text/csv") public ResponseEntity<String> download(@PathVariable UUID id){var content=service.download(id);return ResponseEntity.ok().header("Content-Type",content.contentType()).header("Content-Disposition","attachment; filename=\""+content.fileName()+"\"").header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").body(content.content());}
 private static void match(UUID header,UUID body){if(!header.equals(body))throw new IllegalArgumentException("Operation mismatch");}
}
