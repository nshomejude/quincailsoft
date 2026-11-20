package com.quincaisoft.tenders.web;import com.quincaisoft.tenders.api.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1") public class BankController{
 private final BankService bank;public BankController(BankService bank){this.bank=bank;}private void key(String k,UUID op){if(!UUID.fromString(k).equals(op))throw new IllegalArgumentException("Idempotency-Key mismatch");}
 @GetMapping("/bank-evidence")@PreAuthorize("hasAnyAuthority('bank:read','SCOPE_bank:read')")public List<BankService.Evidence> list(){return bank.list();}
 @PostMapping("/bank-evidence")@PreAuthorize("hasAnyAuthority('bank:write','SCOPE_bank:write')")public BankService.Evidence request(@RequestHeader("Idempotency-Key")String k,@RequestBody BankService.Request p){key(k,p.operationId());return bank.request(p);}
 @PostMapping("/bank-evidence/{id}/approve")@PreAuthorize("hasAnyAuthority('bank:approve','SCOPE_bank:approve')")public BankService.Evidence approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key")String k,@RequestBody BankService.Approve p){key(k,p.operationId());return bank.approve(id,p);}
 @GetMapping("/bank-matches")@PreAuthorize("hasAnyAuthority('bank:read','SCOPE_bank:read')")public List<BankService.Match> matches(){return bank.matches();}
 @PostMapping("/bank-matches")@PreAuthorize("hasAnyAuthority('bank:approve','SCOPE_bank:approve')")public BankService.Match match(@RequestHeader("Idempotency-Key")String k,@RequestBody BankService.MatchRequest p){key(k,p.operationId());return bank.match(p);}
 @GetMapping("/bank-evidence/{id}")@PreAuthorize("hasAnyAuthority('bank:read','SCOPE_bank:read')")public BankService.Evidence get(@PathVariable UUID id){return bank.get(id);}
}
