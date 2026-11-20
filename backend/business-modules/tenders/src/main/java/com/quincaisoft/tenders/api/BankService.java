package com.quincaisoft.tenders.api;import java.util.*;import java.time.*;
public interface BankService {
 record Evidence(UUID id,LocalDate businessDate,String reference,String direction,String amountXaf,String status,UUID requestedBy,UUID approvedBy,UUID consumedBy,long version){}
 record Request(UUID operationId,LocalDate businessDate,String reference,String direction,String amountXaf,String reason){}
 record Approve(UUID operationId,long version,String reason){}
 record MatchRequest(UUID operationId,UUID evidenceId,LocalDate businessDate,String statementReference,String bankXaf,String feeXaf,String reason){}
 record Match(UUID id,UUID evidenceId,String statementReference,String bankXaf,String feeXaf,UUID journalId){}
 Evidence get(UUID id);List<Evidence> list();@com.quincaisoft.shared.TenantMutation Evidence request(Request request);@com.quincaisoft.shared.TenantMutation Evidence approve(UUID id,Approve request);List<Match> matches();@com.quincaisoft.shared.TenantMutation Match match(MatchRequest request);
 @com.quincaisoft.shared.TenantMutation void consume(UUID evidenceId,UUID sourceId,LocalDate date,long amount,String direction);
 /** Payroll payment must be persisted in the same transaction; this does not certify a provider transfer. */
 @com.quincaisoft.shared.TenantMutation void consumePayroll(UUID evidenceId,UUID payrollPaymentId,LocalDate date,long amount);
}