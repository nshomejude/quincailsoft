package com.quincaisoft.operations.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.Instant;
@Entity @Table(name="operation_recovery_rehearsal") @IdClass(ScopeKey.class) public class RecoveryRehearsal extends ScopedRecord {
 @Column(name="backup_id")public UUID backupId;
 @Column(name="status")public String status;
 @Column(name="current_digest")public String currentDigest;
 @Column(name="differences")public String differences;
 @Column(name="reason")public String reason;
 @Column(name="created_at")public Instant createdAt;
}
