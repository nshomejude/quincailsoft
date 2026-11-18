package com.quincaisoft.shared;
import java.util.UUID;
import java.util.Objects;
public record TenantScope(UUID tenantId, UUID companyId, UUID branchId, UUID actorId) {
 public TenantScope {Objects.requireNonNull(tenantId);Objects.requireNonNull(companyId);Objects.requireNonNull(branchId);Objects.requireNonNull(actorId);}
}
