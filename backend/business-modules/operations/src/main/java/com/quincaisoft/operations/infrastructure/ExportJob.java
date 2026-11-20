package com.quincaisoft.operations.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.Instant;
@Entity @Table(name="operation_export_job") @IdClass(ScopeKey.class) public class ExportJob extends ScopedRecord {
 @Column(name="entity")public String entity;
 @Column(name="status")public String status;
 @Column(name="row_count")public int rowCount;
 @Column(name="digest")public String digest;
 @Column(name="content")public String content;
 @Column(name="created_at")public Instant createdAt;
 @Column(name="expires_at")public Instant expiresAt;
 @Column(name="authorization_epoch")public long authorizationEpoch;
 @Column(name="failure")public String failure;
}
