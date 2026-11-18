package com.quincaisoft.operations.infrastructure;
import jakarta.persistence.*;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="operation_export_retention") @IdClass(ScopeKey.class)
public class ExportRetention extends ScopedRecord {
 @Column(name="export_id") public UUID exportId;
 public String digest;public String reason;public String status;
 @Column(name="reviewed_by") public UUID reviewedBy;
 @Column(name="expires_at") public Instant expiresAt;
 @Column(name="created_at") public Instant createdAt;
 @Column(name="purged_at") public Instant purgedAt;
}
