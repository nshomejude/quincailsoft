package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="subscription_notification_delivery")
public class NotificationDelivery{
 @Id @Column(name="event_id") public UUID eventId;
 @Column(name="tenant_id") public UUID tenantId;
 @Column(name="status") public String status;
 @Column(name="attempts") public int attempts;
 @Column(name="attempt_budget") public int attemptBudget;
 @Column(name="destination_key") public String destinationKey;
 @Column(name="lease_token") public UUID leaseToken;
 @Column(name="lease_expires_at") public Instant leaseExpiresAt;
 @Column(name="next_attempt_at") public Instant nextAttemptAt;
 @Column(name="last_outcome") public String lastOutcome;
 @Column(name="last_http_status") public Integer lastHttpStatus;
 @Column(name="completed_at") public Instant completedAt;
 @Version @Column(name="version") public long version;
}
