package com.quincaisoft.expenses.api;
import java.util.*;import java.time.*;
public interface ExpenseService {
 record Draft(UUID operationId,LocalDate businessDate,String category,String payee,String amountXaf,String reason){}
 record Command(UUID operationId,long version,String reason,UUID cashSessionId){}
 record Expense(UUID id,LocalDate businessDate,String category,String payee,String amountXaf,String status,long version,UUID paidFactId){}
 record BankPay(UUID operationId,long version,String reason,UUID bankEvidenceId){}
 Expense payBank(UUID id,BankPay request);
 List<Expense> list();Expense draft(Draft request);Expense command(UUID id,String action,Command request);
}
