package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="subscription_invoice_activation")
public class SubscriptionActivation {
    @Id @Column(name="invoice_id") public UUID invoiceId;
    @Column(name="tenant_id") public UUID tenantId;
    @Column(name="payment_id") public UUID paymentId;
    @Column(name="activated_at") public Instant activatedAt;
}
