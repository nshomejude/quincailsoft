package com.quincaisoft.returns.web;import com.quincaisoft.returns.api.ReturnService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1/returns") public class ReturnController {
 private final ReturnService service;public ReturnController(ReturnService service){this.service=service;}
 @GetMapping @PreAuthorize("hasAnyAuthority('returns:read','SCOPE_returns:read')")public List<ReturnService.CustomerReturn> list(){return service.list();}
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('returns:read','SCOPE_returns:read')")public ReturnService.CustomerReturn get(@PathVariable UUID id){return service.get(id);}
 @PostMapping @PreAuthorize("hasAnyAuthority('returns:write','SCOPE_returns:write')")public ReturnService.CustomerReturn draft(@RequestHeader("Idempotency-Key")UUID key,@RequestBody ReturnService.Draft d){match(key,d.operationId());return service.draft(d);}
 @PostMapping("/{id}/approve") @PreAuthorize("hasAnyAuthority('returns:approve','SCOPE_returns:approve')")public ReturnService.CustomerReturn approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody ReturnService.Command c){match(key,c.operationId());return service.approve(id,c);}
 @PostMapping("/{id}/post") @PreAuthorize("hasAnyAuthority('returns:post','SCOPE_returns:post')")public ReturnService.CustomerReturn post(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody ReturnService.Command c){match(key,c.operationId());return service.post(id,c);}
 private void match(UUID k,UUID op){if(!k.equals(op))throw new IllegalArgumentException("Idempotency-Key mismatch");}
}