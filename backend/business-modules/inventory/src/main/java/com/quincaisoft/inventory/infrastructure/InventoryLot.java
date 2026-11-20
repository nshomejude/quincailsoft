package com.quincaisoft.inventory.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="inventory_lot") @IdClass(InventoryLot.Key.class)
public class InventoryLot {
@Id @Column(name="tenant_id") public UUID tenantId; @Id @Column(name="company_id") public UUID companyId; @Id public UUID id; @Column(name="branch_id") public UUID branchId;
@Column(name="warehouse_id") public UUID warehouseId;
@Column(name="product_id") public UUID productId;
@Column(name="lot_number") public String lotNumber;
@Column(name="source_reference") public String sourceReference;
@Column(name="quantity_milli") public long quantityMilli;
@Column(name="quarantined_milli") public long quarantinedMilli;
@Column(name="expires_on") public LocalDate expiresOn;
@Column(name="created_by") public UUID createdBy;
@Column(name="created_at") public Instant createdAt;
@Version public long version;
public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable{}
}
