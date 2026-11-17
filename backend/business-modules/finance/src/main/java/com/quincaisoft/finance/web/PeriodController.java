package com.quincaisoft.finance.web;
import com.quincaisoft.finance.api.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1")public class PeriodController {
 private final PeriodService service;public PeriodController(PeriodService service){this.service=service;}private void key(String k,UUID o){if(!UUID.fromString(k).equals(o))throw new IllegalArgumentException("Idempotency-Key mismatch");}
 @GetMapping("/periods")@PreAuthorize("hasAnyAuthority('finance:read','SCOPE_finance:read')")public List<PeriodService.Period> list(){return service.list();}
 @GetMapping("/period-requests")@PreAuthorize("hasAnyAuthority('finance:read','SCOPE_finance:read')")public List<PeriodService.Request> requests(){return service.requests();}
 @PostMapping("/periods")@PreAuthorize("hasAnyAuthority('periods:manage','SCOPE_periods:manage')")public PeriodService.Period create(@RequestHeader("Idempotency-Key")String k,@RequestBody PeriodService.Create p){key(k,p.operationId());return service.create(p);}
 @PostMapping("/periods/{id}/{action:close|reopen}")@PreAuthorize("hasAnyAuthority('periods:manage','SCOPE_periods:manage')")public PeriodService.Request request(@PathVariable UUID id,@PathVariable String action,@RequestHeader("Idempotency-Key")String k,@RequestBody PeriodService.Command p){key(k,p.operationId());return service.request(id,action.toUpperCase(Locale.ROOT),p);}
 @PostMapping("/period-requests/{id}/{action:approve|reject}")@PreAuthorize("hasAnyAuthority('periods:approve','SCOPE_periods:approve')")public PeriodService.Request decide(@PathVariable UUID id,@PathVariable String action,@RequestHeader("Idempotency-Key")String k,@RequestBody PeriodService.Command p){key(k,p.operationId());return service.decide(id,action.equals("approve"),p);}
}
