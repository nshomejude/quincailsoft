package com.quincaisoft.platformcontrol.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;
@Entity @Table(name="platform_provisioning_event") public class ProvisioningEvent {
 @Id public UUID id;
 @Column(name="provisioning_id") public UUID provisioningId;
 @Column(name="actor_id") public UUID actorId;
 @Column(name="correlation_id") public UUID correlationId;
 @Column(name="event_type") public String eventType;
 @Column(name="created_at") public Instant createdAt;
}
