package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="subscription_entitlement") @IdClass(TrialEntitlement.Key.class)
public class TrialEntitlement {
    public record Key(UUID tenantId,String feature) implements java.io.Serializable {}
    @Id @Column(name="tenant_id") public UUID tenantId;
    @Id public String feature;
    public UUID id;
    public boolean enabled;
    @Column(name="expires_at") public Instant expiresAt;
}
