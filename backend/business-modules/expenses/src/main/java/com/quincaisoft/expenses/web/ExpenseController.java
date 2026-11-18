package com.quincaisoft.expenses.web;
import com.quincaisoft.expenses.api.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import java.util.*;
@RestController @RequestMapping("/api/v1/expenses")public class ExpenseController{
 private final ExpenseService service;public ExpenseController(ExpenseService service){this.service=service;}private void key(String k,UUID o){if(!UUID.fromString(k).equals(o))throw new IllegalArgumentException("Idempotency-Key mismatch");}
 @GetMapping @PreAuthorize("hasAnyAuthority('expenses:read','SCOPE_expenses:read')")public List<ExpenseService.Expense> list(){return service.list();}
 @PostMapping @PreAuthorize("hasAnyAuthority('expenses:write','SCOPE_expenses:write')")public ExpenseService.Expense create(@RequestHeader("Idempotency-Key")String k,@RequestBody ExpenseService.Draft p){key(k,p.operationId());return service.draft(p);}
 @PostMapping("/{id}/{action:submit|cancel}")@PreAuthorize("hasAnyAuthority('expenses:write','SCOPE_expenses:write')")public ExpenseService.Expense submit(@PathVariable UUID id,@PathVariable String action,@RequestHeader("Idempotency-Key")String k,@RequestBody ExpenseService.Command p){key(k,p.operationId());return service.command(id,action,p);}
 @PostMapping("/{id}/{action:approve|reject}")@PreAuthorize("hasAnyAuthority('expenses:approve','SCOPE_expenses:approve')")public ExpenseService.Expense approve(@PathVariable UUID id,@PathVariable String action,@RequestHeader("Idempotency-Key")String k,@RequestBody ExpenseService.Command p){key(k,p.operationId());return service.command(id,action,p);}
 @PostMapping("/{id}/pay")@PreAuthorize("hasAnyAuthority('expenses:pay','SCOPE_expenses:pay')")public ExpenseService.Expense pay(@PathVariable UUID id,@RequestHeader("Idempotency-Key")String k,@RequestBody ExpenseService.Command p){key(k,p.operationId());return service.command(id,"pay",p);}
}
