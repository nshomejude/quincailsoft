package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="subscription_lifecycle_operation")
public class LifecycleOperation {
    @Id @Column(name="id") public UUID id;
    @Column(name="actor_id") public UUID actorId;
    @Column(name="tenant_id") public UUID tenantId;
    @Column(name="kind") public String kind;
    @Column(name="request_hash") public String requestHash;
    @Column(name="request_json") public String requestJson;
    @Column(name="response_json") public String responseJson;
    @Column(name="occurred_at") public Instant occurredAt;
}
