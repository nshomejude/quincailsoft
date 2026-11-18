package com.quincaisoft.shared;
import java.util.UUID;
import java.util.Objects;
/** Stable identifiers of one independently approved platform bootstrap. Not merchant-supplied authority. */
public record BootstrapScope(UUID provisioningId, UUID tenantId, UUID companyId, UUID branchId, UUID ownerId) {
 public BootstrapScope { Objects.requireNonNull(provisioningId); Objects.requireNonNull(tenantId); Objects.requireNonNull(companyId); Objects.requireNonNull(branchId); Objects.requireNonNull(ownerId); }
 public static final String AUTHORIZATION_SQL = "select count(*) from platform_provisioning p where p.id=:p and p.tenant_id=:t and p.company_id=:c and p.branch_id=:b and p.owner_subject=:o and p.status='PROVISIONING' and p.actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid and p.id=nullif(current_setting('app.provisioning_id',true),'')::uuid and p.tenant_id=app_tenant() and p.company_id=app_company() and p.branch_id=app_branch() and exists(select 1 from platform_operator a where a.actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid and a.active)";
}
