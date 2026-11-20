package com.quincaisoft.quotations.infrastructure;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="quotation_decision") @IdClass(QuotationEntity.Key.class)
public class QuotationDecisionEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="quotation_id") public UUID quotationId;
 @Column(name="operation_id") public UUID operationId;
 @Column(name="expected_version") public long expectedVersion;
 public String decision;
 @Column(name="actor_id") public UUID actorId;
 public String reason;
 @Column(name="evidence_reference") public String evidenceReference;
 @Column(name="recorded_at") public Instant recordedAt;
}
