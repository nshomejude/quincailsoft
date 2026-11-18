package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="subscription_notification_attempt")
public class NotificationAttempt{
 @Id @Column(name="id") public UUID id;
 @Column(name="tenant_id") public UUID tenantId;
 @Column(name="event_id") public UUID eventId;
 @Column(name="attempt_number") public int attemptNumber;
 @Column(name="stage") public String stage;
 @Column(name="lease_token") public UUID leaseToken;
 @Column(name="actor_id") public UUID actorId;
 @Column(name="outcome") public String outcome;
 @Column(name="http_status") public Integer httpStatus;
 @Column(name="recorded_at") public Instant recordedAt;
}
