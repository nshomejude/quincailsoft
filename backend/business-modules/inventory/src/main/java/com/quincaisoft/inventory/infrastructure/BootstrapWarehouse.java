package com.quincaisoft.inventory.infrastructure;
import jakarta.persistence.*;import java.util.UUID;
@Entity @Table(name="warehouse") @IdClass(BootstrapWarehouse.Key.class)
public class BootstrapWarehouse {
 public record Key(UUID tenantId,UUID companyId,UUID id) implements java.io.Serializable{}
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;public String name;public boolean active;
 @Column(name="lock_version") public long lockVersion;
}
