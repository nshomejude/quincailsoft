package com.quincaisoft.operations.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.Instant;
@Entity @Table(name="operation_notification") @IdClass(ScopeKey.class) public class Notification extends ScopedRecord {
 @Column(name="kind")public String kind;
 @Column(name="message")public String message;
 @Column(name="source_id")public UUID sourceId;
 @Column(name="created_at")public Instant createdAt;
 @Column(name="read_at")public Instant readAt;
}
