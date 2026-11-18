package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="tenant_subscription")
public class TenantSubscription {
    @Id @Column(name="tenant_id") public UUID tenantId;
    @Column(name="plan_code") public String planCode;
    @Column(name="status") public String status;
    @Column(name="trial_ends_at") public Instant trialEndsAt;
    @Column(name="version") public long version;
    @Column(name="created_at") public Instant createdAt;
    @Column(name="updated_at") public Instant updatedAt;
    @Column(name="term_ends_at") public Instant termEndsAt;
    @Column(name="activation_payment_id") public UUID activationPaymentId;
}
