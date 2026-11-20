package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;
@Entity @Table(name="subscription_quota_operation") @IdClass(QuotaOperation.Key.class)
public class QuotaOperation {
 public record Key(UUID tenantId,UUID operationId)implements java.io.Serializable{}
 @Id @Column(name="tenant_id")public UUID tenantId;@Id @Column(name="operation_id")public UUID operationId;
 @Column(name="company_id")public UUID companyId;@Column(name="branch_id")public UUID branchId;@Column(name="actor_id")public UUID actorId;
 @Column(name="request_hash")public String requestHash;@Column(name="response_text")public String responseText;@Column(name="created_at")public Instant createdAt;
}
