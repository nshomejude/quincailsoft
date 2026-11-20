package com.quincaisoft.tenders.web;
import com.quincaisoft.finance.api.FinanceService;
import com.quincaisoft.tenders.api.TenderService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1") public class TenderController {
 private final TenderService service;
 private final FinanceService finance;

 public TenderController(TenderService service, FinanceService finance) {
  this.service = service;
  this.finance = finance;
 }

 public record CollectionSummary(long pendingCount) {}

 @GetMapping("/tenders/collections/summary")
 @PreAuthorize("hasAnyAuthority('tenders:read','SCOPE_tenders:read')")
 public CollectionSummary collectionSummary() {
  return new CollectionSummary(finance.pendingCollectionCount());
 }
 private void key(String key,UUID op){if(op==null||!op.equals(UUID.fromString(key)))throw new IllegalArgumentException("Idempotency-Key must match operationId");}
 @GetMapping("/cash-sessions") @PreAuthorize("hasAnyAuthority('cash:read','SCOPE_cash:read')") public List<TenderService.Session> sessions(){return service.sessions();}
 @PostMapping("/cash-sessions/open") @PreAuthorize("hasAnyAuthority('cash:manage','SCOPE_cash:manage')") public TenderService.Session open(@RequestHeader("Idempotency-Key")String k,@RequestBody TenderService.Open p){key(k,p.operationId());return service.open(p);}
 @PostMapping("/cash-sessions/{id}/close") @PreAuthorize("hasAnyAuthority('cash:manage','SCOPE_cash:manage')") public TenderService.Session close(@PathVariable UUID id,@RequestHeader("Idempotency-Key")String k,@RequestBody TenderService.Close p){key(k,p.operationId());return service.close(id,p);}
 @GetMapping("/tenders") @PreAuthorize("hasAnyAuthority('tenders:read','SCOPE_tenders:read')") public List<TenderService.Fact> facts(){return service.facts();}
 @GetMapping("/deposits") @PreAuthorize("hasAnyAuthority('tenders:read','SCOPE_tenders:read')") public List<TenderService.DepositView> deposits(){return service.deposits();}
 @PostMapping("/tenders/receipts") @PreAuthorize("hasAnyAuthority('tenders:write','SCOPE_tenders:write')") public TenderService.Fact receipt(@RequestHeader("Idempotency-Key")String k,@RequestBody TenderService.Receipt p){key(k,p.operationId());return service.receipt(p);}
 @PostMapping("/deposits") @PreAuthorize("hasAnyAuthority('tenders:write','SCOPE_tenders:write')") public TenderService.Fact deposit(@RequestHeader("Idempotency-Key")String k,@RequestBody TenderService.Deposit p){key(k,p.operationId());return service.deposit(p);}
 @PostMapping("/deposits/{id}/allocate") @PreAuthorize("hasAnyAuthority('tenders:write','SCOPE_tenders:write')") public TenderService.Fact allocate(@PathVariable UUID id,@RequestHeader("Idempotency-Key")String k,@RequestBody TenderService.Allocation p){key(k,p.operationId());return service.allocate(id,p);}
 @PostMapping("/deposits/{id}/refund") @PreAuthorize("hasAnyAuthority('tenders:refund','SCOPE_tenders:refund')") public TenderService.Fact refund(@PathVariable UUID id,@RequestHeader("Idempotency-Key")String k,@RequestBody TenderService.Allocation p){key(k,p.operationId());return service.refund(id,p);}
}
