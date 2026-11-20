package com.quincaisoft.operations.infrastructure;
import jakarta.persistence.*;import java.util.*;import com.quincaisoft.shared.TenantContext;
@MappedSuperclass public abstract class ScopedRecord {
 @Id @Column(name="tenant_id")public UUID tenantId;
 @Id @Column(name="company_id")public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id")public UUID branchId;
 @Column(name="actor_id")public UUID actorId;
 @Version public long version;
 public void scope(UUID id){var s=TenantContext.current();this.id=id;tenantId=s.tenantId();companyId=s.companyId();branchId=s.branchId();actorId=s.actorId();}
}
