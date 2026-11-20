package com.quincaisoft.inventory.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="inventory_reservation") @IdClass(InventoryReservation.Key.class)
public class InventoryReservation {
@Id @Column(name="tenant_id") public UUID tenantId; @Id @Column(name="company_id") public UUID companyId; @Id public UUID id; @Column(name="branch_id") public UUID branchId;
@Column(name="warehouse_id") public UUID warehouseId;
@Column(name="product_id") public UUID productId;
@Column(name="quantity_milli") public long quantityMilli;
@Column(name="released_milli") public long releasedMilli;
@Column(name="status") public String status;
@Column(name="source_reference") public String sourceReference;
@Column(name="reason") public String reason;
@Column(name="created_by") public UUID createdBy;
@Column(name="created_at") public Instant createdAt;
@Version public long version;
public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable{}
}
