package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="subscription_notification_redrive")
public class NotificationRedrive{
 @Id @Column(name="id") public UUID id;
 @Column(name="event_id") public UUID eventId;
 @Column(name="actor_id") public UUID actorId;
 @Column(name="reason") public String reason;
 @Column(name="request_hash") public String requestHash;
 @Column(name="response_json") public String responseJson;
 @Column(name="recorded_at") public Instant recordedAt;
}
