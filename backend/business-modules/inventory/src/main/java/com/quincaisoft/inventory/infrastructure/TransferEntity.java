package com.quincaisoft.inventory.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.io.Serializable;import java.time.Instant;
@Entity @Table(name="inventory_transfer") @IdClass(TransferEntity.Key.class)
public class TransferEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;@Id @Column(name="company_id") public UUID companyId;@Id public UUID id;
 @Column(name="branch_id") public UUID branchId;@Column(name="origin_warehouse") public UUID originWarehouse;@Column(name="destination_warehouse") public UUID destinationWarehouse;@Column(name="product_id") public UUID productId;
 @Column(name="quantity_milli") public long quantityMilli;@Column(name="received_milli") public long receivedMilli;@Column(name="value_xaf") public long valueXaf;@Column(name="received_value_xaf") public long receivedValueXaf;@Column(name="business_date") public java.time.LocalDate businessDate;public String status;public String reason;
 @Column(name="created_by") public UUID createdBy;@Column(name="approved_by") public UUID approvedBy;@Column(name="created_at") public Instant createdAt;@Version public long version;
 public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable{}
 public TransferEntity(){}
}
