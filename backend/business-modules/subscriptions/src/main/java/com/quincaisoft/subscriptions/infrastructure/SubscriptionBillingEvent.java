package com.quincaisoft.subscriptions.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Durable append-only publication fact; dispatch delivery must be tracked independently. */
@Entity @Table(name="subscription_billing_event")
public class SubscriptionBillingEvent {
    @Id public UUID id;
    @Column(name="tenant_id") public UUID tenantId;
    @Column(name="invoice_id") public UUID invoiceId;
    @Column(name="actor_id") public UUID actorId;
    @Column(name="request_id") public UUID requestId;
    @Column(name="event_type") public String eventType;
    @Column(name="occurred_at") public Instant occurredAt;
}
