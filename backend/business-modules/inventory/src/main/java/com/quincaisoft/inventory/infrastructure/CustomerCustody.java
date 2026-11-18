package com.quincaisoft.inventory.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="customer_custody") @IdClass(CustomerCustody.Key.class) public class CustomerCustody {
@Id @Column(name="tenant_id")public UUID tenantId;@Id @Column(name="company_id")public UUID companyId;@Id public UUID id;@Column(name="branch_id")public UUID branchId;
@Column(name="sale_id") public UUID saleId;
@Column(name="customer_id") public UUID customerId;
@Column(name="warehouse_id") public UUID warehouseId;
@Column(name="product_id") public UUID productId;
@Column(name="quantity_milli") public long quantityMilli;
@Column(name="collected_milli") public long collectedMilli;
@Column(name="frozen_cost_xaf") public long frozenCostXaf;
@Column(name="status") public String status;
@Column(name="operation_id") public UUID operationId;
@Column(name="created_at") public Instant createdAt;
@Version public long version;
public record Key(UUID tenantId,UUID companyId,UUID id)implements Serializable{}
}
