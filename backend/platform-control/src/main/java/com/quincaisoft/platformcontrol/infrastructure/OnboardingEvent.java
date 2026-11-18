package com.quincaisoft.platformcontrol.infrastructure;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
/** Immutable platform audit and durable outbox fact; excludes tokens and owner contact data. */
@Entity @Table(name="platform_onboarding_event")
public class OnboardingEvent {
    @Id public UUID id;
    @Column(name="request_id") public UUID requestId;
    @Column(name="operation_id") public UUID operationId;
    @Column(name="actor_id") public UUID actorId;
    @Column(name="event_type") public String eventType;
    @Column(name="configuration_version") public long configurationVersion;
    @Column(name="created_at") public Instant createdAt;
}
