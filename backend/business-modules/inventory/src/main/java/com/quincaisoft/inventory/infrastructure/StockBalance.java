package com.quincaisoft.inventory.infrastructure;
import com.quincaisoft.inventory.api.StockService;
import jakarta.persistence.*;
import java.util.UUID;
import java.io.Serializable;
@Entity @Table(name="stock_balance") @IdClass(StockBalance.Key.class)
public class StockBalance {
 @Id @Column(name="tenant_id") public UUID tenantId; @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="warehouse_id") public UUID warehouseId; @Id @Column(name="product_id") public UUID productId;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="quantity_milli") public long quantityMilli; @Column(name="reserved_milli") public long reservedMilli;
 @Column(name="allocated_milli") public long allocatedMilli; @Column(name="value_xaf") public long valueXaf; @Column(name="quarantined_milli") public long quarantinedMilli; @Version public long version;
 public StockBalance(){}
 public record Key(UUID tenantId,UUID companyId,UUID warehouseId,UUID productId) implements Serializable{}
 public void receive(long quantity,long value){if(quantity<=0||value<0)throw new IllegalArgumentException("Positive quantity and nonnegative value required");long nextQuantity=Math.addExact(quantityMilli,quantity);long nextValue=Math.addExact(valueXaf,value);quantityMilli=nextQuantity;valueXaf=nextValue;}
 public long available(){return Math.subtractExact(Math.subtractExact(Math.subtractExact(quantityMilli,reservedMilli),allocatedMilli),quarantinedMilli);}
 public long issue(long quantity){long available=available();if(quantity<=0||quantity>available)throw new IllegalArgumentException("Insufficient unallocated serviceable merchant stock");long cost=StockCost.exactIssue(valueXaf,quantity,quantityMilli);quantityMilli=Math.subtractExact(quantityMilli,quantity);valueXaf=Math.subtractExact(valueXaf,cost);return cost;}
}
