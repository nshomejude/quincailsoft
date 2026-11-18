package com.quincaisoft.subscriptions.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="subscription_invoice")
public class SubscriptionInvoice {
    @Id public UUID id;
    @Column(name="tenant_id") public UUID tenantId;
    @Column(name="actor_id") public UUID actorId;
    @Column(name="request_id") public UUID requestId;
    @Column(name="request_hash") public String requestHash;
    public String kind;
    @Column(name="original_id") public UUID originalId;
    public String number;
    @Column(name="issuer_name") public String issuerName;
    @Column(name="issuer_tax_reference") public String issuerTaxReference;
    @Column(name="customer_name") public String customerName;
    @Column(name="customer_tax_reference") public String customerTaxReference;
    public String description;
    public String jurisdiction;
    @Column(name="period_start") public LocalDate periodStart;
    @Column(name="period_end") public LocalDate periodEnd;
    @Column(name="due_date") public LocalDate dueDate;
    @Column(name="subtotal_xaf") public long subtotalXaf;
    @Column(name="discount_xaf") public long discountXaf;
    @Column(name="tax_xaf") public long taxXaf;
    @Column(name="total_xaf") public long totalXaf;
    @Column(name="created_at") public Instant createdAt;
}
