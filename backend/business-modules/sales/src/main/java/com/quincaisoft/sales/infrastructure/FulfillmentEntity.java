package com.quincaisoft.sales.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="sale_fulfillment") @IdClass(FulfillmentEntity.Key.class)public class FulfillmentEntity{
 @Id @Column(name="tenant_id")public UUID tenantId;@Id @Column(name="company_id")public UUID companyId;@Id public UUID id;@Column(name="branch_id")public UUID branchId;
 @Column(name="sale_id")public UUID saleId;@Column(name="customer_id")public UUID customerId;public String status;@Column(name="proof_reference")public String proofReference;
 @Column(name="business_date")public LocalDate businessDate;@Column(name="created_by")public UUID createdBy;@Column(name="created_at")public Instant createdAt;@Version public long version;
 public String method;
 @Column(name="scheduled_date") public LocalDate scheduledDate;
 @Column(name="delivery_address") public String address;
 @Column(name="recipient_name") public String recipientName;
 @Column(name="recipient_phone") public String recipientPhone;
 public String notes;
 public record Key(UUID tenantId,UUID companyId,UUID id)implements Serializable{}
}
