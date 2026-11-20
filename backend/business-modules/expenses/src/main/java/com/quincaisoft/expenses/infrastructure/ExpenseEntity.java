package com.quincaisoft.expenses.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="operating_expense") @IdClass(ExpenseEntity.Key.class) public class ExpenseEntity {
 @Id @Column(name="tenant_id")public UUID tenantId;@Id @Column(name="company_id")public UUID companyId;@Id public UUID id;@Column(name="branch_id")public UUID branchId;@Column(name="created_by")public UUID createdBy;@Column(name="submitted_by")public UUID submittedBy;@Column(name="approved_by")public UUID approvedBy;@Column(name="business_date")public LocalDate businessDate;public String category;public String payee;@Column(name="amount_xaf")public long amountXaf;public String reason;public String status;@Column(name="paid_fact_id")public UUID paidFactId;@Column(name="paid_bank_settlement_id")public UUID paidBankSettlementId;@Version public long version;
 public record Key(UUID tenantId,UUID companyId,UUID id)implements Serializable{}
}
