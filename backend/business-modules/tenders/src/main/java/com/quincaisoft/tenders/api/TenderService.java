package com.quincaisoft.tenders.api;
import java.util.*;import java.time.*;
public interface TenderService {
 record Open(UUID operationId,String tillCode,LocalDate businessDate,String openingXaf,String reason){}
 record Close(UUID operationId,long version,LocalDate businessDate,String countedXaf,String reason){}
 record Receipt(UUID operationId,UUID saleId,UUID customerId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String reason){}
 record Deposit(UUID operationId,UUID customerId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String reason){}
 record Allocation(UUID operationId,UUID saleId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String reason){}
 record CashExpense(UUID operationId,UUID expenseId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String reason){}
 record SupplierCash(UUID operationId,UUID settlementId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String kind,String reason){}
 @com.quincaisoft.shared.TenantMutation Fact supplierDisbursement(SupplierCash request);
 @com.quincaisoft.shared.TenantMutation Fact expense(CashExpense request);
 record Session(UUID id,String tillCode,LocalDate businessDate,String status,String openingXaf,String expectedXaf,String countedXaf,long version){}
 record Fact(UUID id,String kind,UUID customerId,UUID saleId,UUID depositId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String cashDeltaXaf,UUID journalId,String reason){}
 record DepositView(UUID id,UUID customerId,String originalXaf,String allocatedXaf,String refundedXaf,String remainingXaf,long version){}
 List<Session> sessions();@com.quincaisoft.shared.TenantMutation Session open(Open request);@com.quincaisoft.shared.TenantMutation Session close(UUID id,Close request);List<Fact> facts();List<DepositView> deposits();@com.quincaisoft.shared.TenantMutation Fact receipt(Receipt request);@com.quincaisoft.shared.TenantMutation Fact deposit(Deposit request);@com.quincaisoft.shared.TenantMutation Fact allocate(UUID id,Allocation request);@com.quincaisoft.shared.TenantMutation Fact refund(UUID id,Allocation request);
}
