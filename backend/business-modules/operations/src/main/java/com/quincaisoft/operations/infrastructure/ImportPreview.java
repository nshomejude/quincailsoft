package com.quincaisoft.operations.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.Instant;
@Entity @Table(name="operation_import_preview") @IdClass(ScopeKey.class) public class ImportPreview extends ScopedRecord {
 @Column(name="entity")public String entity;
 @Column(name="digest")public String digest;
 @Column(name="expires_at")public Instant expiresAt;
 @Column(name="committed")public boolean committed;
 @Column(name="created_at")public Instant createdAt;
}
