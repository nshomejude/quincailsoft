package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="subscription_manual_payment")
public class ManualSubscriptionPayment {
    @Id public UUID id;
    @Column(name="tenant_id") public UUID tenantId;
    @Column(name="invoice_id") public UUID invoiceId;
    @Column(name="plan_code") public String planCode;
    @Column(name="amount_xaf") public long amountXaf;
    @Column(name="bank_reference") public String bankReference;
    @Column(name="evidence_sha256") public String evidenceSha256;
    public String reason;
    public String status;
    @Column(name="recorded_by") public UUID recordedBy;
    @Column(name="reviewed_by") public UUID reviewedBy;
    @Column(name="reviewed_at") public Instant reviewedAt;
}
