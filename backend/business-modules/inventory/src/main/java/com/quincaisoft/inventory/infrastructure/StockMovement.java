package com.quincaisoft.inventory.infrastructure;
import com.quincaisoft.inventory.api.StockService;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.io.Serializable;
@Entity @Table(name="stock_movement") @IdClass(StockMovement.Key.class)
public class StockMovement {
 @Id @Column(name="tenant_id") public UUID tenantId; @Column(name="company_id") public UUID companyId; @Id public UUID id;
 @Column(name="branch_id") public UUID branchId; @Column(name="warehouse_id") public UUID warehouseId; @Column(name="product_id") public UUID productId;
 @Column(name="quantity_milli") public long quantityMilli; @Column(name="value_xaf") public long valueXaf;
 @Column(name="actor_id") public UUID actorId;
 @Column(name="operation_id") public UUID operationId; public String reason; @Column(name="occurred_at") public Instant occurredAt;
 public StockMovement(){}
 public record Key(UUID tenantId,UUID id) implements Serializable{}
}
