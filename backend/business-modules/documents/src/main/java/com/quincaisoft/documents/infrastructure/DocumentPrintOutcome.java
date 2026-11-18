package com.quincaisoft.documents.infrastructure;
import jakarta.persistence.*;import java.util.UUID;import java.time.Instant;
@Entity @Table(name="document_print_outcome") @IdClass(DocumentSnapshot.Key.class)
public class DocumentPrintOutcome {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="id") public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="operation_id") public UUID operationId;
 @Column(name="actor_id") public UUID actorId;
 @Column(name="attempt_id") public UUID attemptId;
 @Column(name="status") public String status;
 @Column(name="reason") public String reason;
 @Column(name="recorded_at") public Instant recordedAt;
}
