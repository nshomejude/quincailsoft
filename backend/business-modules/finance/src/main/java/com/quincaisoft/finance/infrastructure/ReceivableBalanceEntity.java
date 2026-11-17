package com.quincaisoft.finance.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.io.Serializable;
@Entity @Table(name="receivable_balance") @IdClass(ReceivableBalanceEntity.Key.class)
public class ReceivableBalanceEntity {
 @Id @Column(name="tenant_id") public UUID tenantId; @Id @Column(name="company_id") public UUID companyId; @Id @Column(name="sale_id") public UUID saleId;
 @Column(name="branch_id") public UUID branchId; @Column(name="outstanding_xaf") public long outstandingXaf; @Version public long version;
 public record Key(UUID tenantId,UUID companyId,UUID saleId) implements Serializable {}
}
