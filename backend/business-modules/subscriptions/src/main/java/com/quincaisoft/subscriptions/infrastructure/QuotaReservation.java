package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;
@Entity @org.hibernate.annotations.DynamicUpdate @Table(name="subscription_quota_reservation") @IdClass(QuotaReservation.Key.class)
public class QuotaReservation {
 public record Key(UUID tenantId,UUID id)implements java.io.Serializable{}
 @Id @Column(name="tenant_id")public UUID tenantId;@Id public UUID id;
 @Column(name="company_id")public UUID companyId;@Column(name="branch_id")public UUID branchId;
 @Column(name="actor_id")public UUID actorId;public String resource;@Column(name="subject_id")public UUID subjectId;
 @Column(name="subject_key")public String subjectKey;public long units;public String status;
 @Column(name="created_at")public Instant createdAt;
}
