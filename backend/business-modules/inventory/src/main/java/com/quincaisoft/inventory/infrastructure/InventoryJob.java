package com.quincaisoft.inventory.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.io.Serializable;import java.time.*;
@Entity @Table(name="inventory_job") @IdClass(InventoryJob.Key.class)
public class InventoryJob {
 @Id @Column(name="tenant_id")public UUID tenantId;@Id @Column(name="company_id")public UUID companyId;@Id public UUID id;
 @Column(name="branch_id")public UUID branchId;@Column(name="warehouse_id")public UUID warehouseId;@Column(name="source_product")public UUID sourceProduct;@Column(name="output_product")public UUID outputProduct;
 public String kind;public String status;@Column(name="source_quantity_milli")public long sourceQuantityMilli;@Column(name="output_quantity_milli")public long outputQuantityMilli;
 @Column(name="snapshot_version")public long snapshotVersion;@Column(name="snapshot_value_xaf")public long snapshotValueXaf;@Column(name="posted_value_xaf")public long postedValueXaf;
 @Column(name="parent_length_mm")public long parentLengthMm;@Column(name="output_length_mm")public long outputLengthMm;
 @Column(name="business_date")public LocalDate businessDate;@Column(name="evidence_reference")public String evidenceReference;public String reason;
 @Column(name="created_by")public UUID createdBy;@Column(name="captured_by")public UUID capturedBy;@Column(name="approved_by")public UUID approvedBy;@Column(name="created_at")public Instant createdAt;@Version public long version;
 public InventoryJob(){}public record Key(UUID tenantId,UUID companyId,UUID id)implements Serializable{}
}
