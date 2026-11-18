package com.quincaisoft.tenders.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="deposit_balance") @IdClass(DepositBalanceEntity.Key.class) public class DepositBalanceEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Column(name="branch_id") public UUID branchId;
 @Id @Column(name="id") public UUID id;
 @Column(name="customer_id") public UUID customerId;
 @Column(name="original_xaf") public long originalXaf;
 @Column(name="allocated_xaf") public long allocatedXaf;
 @Column(name="refunded_xaf") public long refundedXaf;
 @Version @Column(name="version") public long version;
 public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable {}
}
