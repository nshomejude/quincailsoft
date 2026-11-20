package com.quincaisoft.documents.infrastructure;
import jakarta.persistence.*;import java.util.UUID;import java.time.Instant;
@Entity @Table(name="document_print_attempt") @IdClass(DocumentSnapshot.Key.class)
public class DocumentPrintAttempt {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="id") public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="operation_id") public UUID operationId;
 @Column(name="actor_id") public UUID actorId;
 @Column(name="original_id") public UUID originalId;
 @Column(name="capture_id") public UUID captureId;
 @Column(name="paper") public String paper;
 @Column(name="duplicate") public boolean duplicate;
 @Column(name="reason") public String reason;
 @Column(name="document_hash") public String documentHash;
 @Column(name="requested_at") public Instant requestedAt;
}
