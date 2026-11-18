package com.quincaisoft.integrations.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="provider_intent") @IdClass(ProviderIntentEntity.Key.class) public class ProviderIntentEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;@Id @Column(name="company_id") public UUID companyId;@Id public UUID id;
 @Column(name="branch_id") public UUID branchId;@Column(name="sale_id") public UUID saleId;@Column(name="actor_id") public UUID actorId;@Column(name="operation_id") public UUID operationId;@Column(name="amount_xaf") public long amountXaf;public String currency;public String environment;public String phone;public String status;@Column(name="business_date") public LocalDate businessDate;public String reason;@Column(name="financial_transaction_id") public String financialTransactionId;@Column(name="journal_id") public UUID journalId;@Column(name="query_due") public boolean queryDue;@Version public long version;
 public record Key(UUID tenantId,UUID companyId,UUID id)implements Serializable{}
}
