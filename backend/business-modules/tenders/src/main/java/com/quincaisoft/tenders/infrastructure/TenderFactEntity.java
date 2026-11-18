package com.quincaisoft.tenders.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="tender_fact") @IdClass(TenderFactEntity.Key.class) public class TenderFactEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Column(name="branch_id") public UUID branchId;
 @Id @Column(name="id") public UUID id;
 @Column(name="operation_id") public UUID operationId;
 @Column(name="kind") public String kind;
 @Column(name="customer_id") public UUID customerId;
 @Column(name="sale_id") public UUID saleId;
 @Column(name="deposit_id") public UUID depositId;
 @Column(name="cash_session_id") public UUID cashSessionId;
 @Column(name="business_date") public LocalDate businessDate;
 @Column(name="amount_xaf") public long amountXaf;
 @Column(name="cash_delta_xaf") public long cashDeltaXaf;
 @Column(name="journal_id") public UUID journalId;
 @Column(name="reason") public String reason;
 @Column(name="actor_id") public UUID actorId;
 public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable {}
}
