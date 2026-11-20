package com.quincaisoft.documents.infrastructure;
import jakarta.persistence.*;import java.util.UUID;import java.time.Instant;
@Entity @Table(name="document_capture") @IdClass(DocumentSnapshot.Key.class)
public class CapturedDocument {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="id") public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="operation_id") public UUID operationId;
 @Column(name="actor_id") public UUID actorId;
 @Column(name="template_id") public String templateId;
 @Column(name="source_id") public UUID sourceId;
 @Column(name="content") public String content;
 @Column(name="sha256") public String sha256;
 @Column(name="captured_at") public Instant capturedAt;
}
