package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="subscription_notice")
public class SubscriptionNotice {
    @Id @Column(name="id") public UUID id;
    @Column(name="tenant_id") public UUID tenantId;
    @Column(name="subscription_version") public long subscriptionVersion;
    @Column(name="delivery_reference") public String deliveryReference;
    @Column(name="delivered_at") public Instant deliveredAt;
    @Column(name="grace_ends_at") public Instant graceEndsAt;
    @Column(name="contract_reference") public String contractReference;
    @Column(name="reason") public String reason;
    @Column(name="status") public String status;
    @Column(name="recorded_by") public UUID recordedBy;
    @Column(name="reviewed_by") public UUID reviewedBy;
    @Column(name="reviewed_at") public Instant reviewedAt;
}
