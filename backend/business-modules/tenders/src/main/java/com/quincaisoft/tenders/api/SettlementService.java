package com.quincaisoft.tenders.api;import java.util.*;import java.time.*;
public interface SettlementService{
 record Funding(UUID cashSessionId,UUID bankEvidenceId){}
 record Payment(UUID operationId,UUID partyId,UUID sourceId,LocalDate businessDate,String amountXaf,Funding funding,String reason){}
 record Allocate(UUID operationId,UUID advanceId,UUID invoiceId,LocalDate businessDate,String amountXaf,String reason){}
 record Settlement(UUID id,String kind,UUID partyId,UUID sourceId,UUID relatedId,String amountXaf,LocalDate businessDate,UUID bankEvidenceId,UUID cashFactId,UUID journalId){}
 Settlement get(UUID id);List<Settlement> list();@com.quincaisoft.shared.TenantMutation Settlement supplierPayment(Payment p);@com.quincaisoft.shared.TenantMutation Settlement supplierAdvance(Payment p);@com.quincaisoft.shared.TenantMutation Settlement allocateAdvance(Allocate p);@com.quincaisoft.shared.TenantMutation Settlement bankReceipt(Payment p);@com.quincaisoft.shared.TenantMutation Settlement bankDeposit(Payment p);@com.quincaisoft.shared.TenantMutation Settlement bankExpense(Payment p);
}