package com.quincaisoft.tenders.api;
import java.util.*;import java.time.*;
public interface TenderService {
 record Open(UUID operationId,String tillCode,LocalDate businessDate,String openingXaf,String reason){}
 record Close(UUID operationId,long version,LocalDate businessDate,String countedXaf,String reason){}
 record Receipt(UUID operationId,UUID saleId,UUID customerId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String reason){}
 record Deposit(UUID operationId,UUID customerId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String reason){}
 record Allocation(UUID operationId,UUID saleId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String reason){}
 record CashExpense(UUID operationId,UUID expenseId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String reason){}
 Fact expense(CashExpense request);
 record Session(UUID id,String tillCode,LocalDate businessDate,String status,String openingXaf,String expectedXaf,String countedXaf,long version){}
 record Fact(UUID id,String kind,UUID customerId,UUID saleId,UUID depositId,UUID cashSessionId,LocalDate businessDate,String amountXaf,String cashDeltaXaf,UUID journalId,String reason){}
 record DepositView(UUID id,UUID customerId,String originalXaf,String allocatedXaf,String refundedXaf,String remainingXaf,long version){}
 List<Session> sessions();Session open(Open request);Session close(UUID id,Close request);List<Fact> facts();List<DepositView> deposits();Fact receipt(Receipt request);Fact deposit(Deposit request);Fact allocate(UUID id,Allocation request);Fact refund(UUID id,Allocation request);
}
