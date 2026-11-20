package com.quincaisoft.platformcontrol.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;
@Entity @Table(name="platform_provisioning_step") @IdClass(ProvisioningStep.Key.class)
public class ProvisioningStep {
 public record Key(UUID provisioningId,String stepCode) implements java.io.Serializable{}
 @Id @Column(name="provisioning_id") public UUID provisioningId;
 @Id @Column(name="step_code") public String stepCode;
 @Column(name="completed_at") public Instant completedAt;
}
