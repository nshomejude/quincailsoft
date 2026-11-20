package com.quincaisoft.inventory.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="inventory_recognition_policy") @IdClass(InventoryRecognitionPolicy.Key.class) public class InventoryRecognitionPolicy {
@Id @Column(name="tenant_id")public UUID tenantId;@Id @Column(name="company_id")public UUID companyId;@Id public UUID id;@Column(name="branch_id")public UUID branchId;
@Column(name="mode") public String mode;
@Column(name="effective_from") public LocalDate effectiveFrom;
@Column(name="contract_reference") public String contractReference;
@Column(name="status") public String status;
@Column(name="created_by") public UUID createdBy;
@Column(name="approved_by") public UUID approvedBy;
@Column(name="created_at") public Instant createdAt;
@Version public long version;
public record Key(UUID tenantId,UUID companyId,UUID id)implements Serializable{}
}
