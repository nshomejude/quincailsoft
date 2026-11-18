package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="subscription_commercial_term")
public class SubscriptionCommercialTerm {
 @Id public UUID id;
 @Column(name="tenant_id") public UUID tenantId;
 @Column(name="invoice_id") public UUID invoiceId;
 @Column(name="plan_code") public String planCode;
 public String basis;
 public String status;
 @Column(name="monthly_price_xaf") public long monthlyPriceXaf;
 @Column(name="subtotal_xaf") public long subtotalXaf;
 @Column(name="discount_xaf") public long discountXaf;
 @Column(name="tax_xaf") public long taxXaf;
 @Column(name="total_xaf") public long totalXaf;
 @Column(name="period_start") public LocalDate periodStart;
 @Column(name="period_end") public LocalDate periodEnd;
 @Column(name="evidence_reference") public String evidenceReference;
 @Column(name="evidence_sha256") public String evidenceSha256;
 public String reason;
 @Column(name="recorded_by") public UUID recordedBy;
 @Column(name="reviewed_by") public UUID reviewedBy;
 @Column(name="reviewed_at") public Instant reviewedAt;
}
