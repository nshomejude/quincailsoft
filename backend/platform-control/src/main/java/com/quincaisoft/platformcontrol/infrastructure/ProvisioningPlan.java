package com.quincaisoft.platformcontrol.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;
@Entity @Table(name="platform_provisioning")
public class ProvisioningPlan {
 @Id public UUID id;
 @Column(name="request_id") public UUID requestId;
 @Column(name="operation_id") public UUID operationId;
 @Column(name="correlation_id") public UUID correlationId;
 @Column(name="actor_id") public UUID actorId;
 @Column(name="request_hash") public String requestHash;
 @Column(name="tenant_id") public UUID tenantId;
 @Column(name="company_id") public UUID companyId;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="warehouse_id") public UUID warehouseId;
 @Column(name="period_id") public UUID periodId;
 @Column(name="owner_subject") public UUID ownerSubject;
 @Column(name="branch_name") public String branchName;
 @Column(name="warehouse_name") public String warehouseName;
 @Column(name="business_date") public LocalDate businessDate;
 @Column(name="storage_prefix") public String storagePrefix;
 @Column(name="trial_ends_at") public Instant trialEndsAt;
 @Column(name="created_at") public Instant createdAt;
 @Column(name="activated_at") public Instant activatedAt;
 @Column(name="failure_code") public String failureCode;
 @Column(name="response_text") public String responseText;
 public String status;
 @Version public long version;
}
