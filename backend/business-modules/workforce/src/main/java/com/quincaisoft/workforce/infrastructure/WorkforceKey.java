package com.quincaisoft.workforce.infrastructure;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
public class WorkforceKey implements Serializable {
    public UUID tenantId, companyId, id;
    public WorkforceKey() {}
    public WorkforceKey(UUID tenantId, UUID companyId, UUID id) { this.tenantId=tenantId; this.companyId=companyId; this.id=id; }
    @Override public boolean equals(Object value) { return value instanceof WorkforceKey key && Objects.equals(tenantId,key.tenantId) && Objects.equals(companyId,key.companyId) && Objects.equals(id,key.id); }
    @Override public int hashCode() { return Objects.hash(tenantId,companyId,id); }
}
