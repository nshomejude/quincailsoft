package com.quincaisoft.inventory.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="inventory_recall") @IdClass(InventoryRecall.Key.class)
public class InventoryRecall {
@Id @Column(name="tenant_id") public UUID tenantId; @Id @Column(name="company_id") public UUID companyId; @Id public UUID id; @Column(name="branch_id") public UUID branchId;
@Column(name="lot_id") public UUID lotId;
@Column(name="quantity_milli") public long quantityMilli;
@Column(name="status") public String status;
@Column(name="business_date") public LocalDate businessDate;
@Column(name="reason") public String reason;
@Column(name="evidence_reference") public String evidenceReference;
@Column(name="created_by") public UUID createdBy;
@Column(name="approved_by") public UUID approvedBy;
@Column(name="created_at") public Instant createdAt;
@Version public long version;
public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable{}
}
