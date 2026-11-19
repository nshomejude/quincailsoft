package com.quincaisoft.sales.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fulfillment_dispatch_line")
@IdClass(FulfillmentDispatchLine.Key.class)
public class FulfillmentDispatchLine {
 @Id @Column(name = "tenant_id") public UUID tenantId;
 @Id @Column(name = "company_id") public UUID companyId;
 @Id @Column(name = "fulfillment_id") public UUID fulfillmentId;
 @Id @Column(name = "operation_id") public UUID operationId;
 @Id @Column(name = "product_id") public UUID productId;
 @Column(name = "branch_id", nullable = false) public UUID branchId;
 @Column(name = "quantity_milli", nullable = false) public long quantityMilli;
 @Column(name = "dispatched_at", nullable = false) public Instant dispatchedAt;
 @Column(name = "dispatched_by", nullable = false) public UUID dispatchedBy;

 public record Key(UUID tenantId, UUID companyId, UUID fulfillmentId,
                   UUID operationId, UUID productId) implements Serializable {}
}
