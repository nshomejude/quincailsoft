package com.quincaisoft.operations.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.Instant;
@Entity @Table(name="operation_backup_manifest") @IdClass(ScopeKey.class) public class BackupManifest extends ScopedRecord {
 @Column(name="digest")public String digest;
 @Column(name="created_at")public Instant createdAt;
 @Column(name="reason")public String reason;
}
