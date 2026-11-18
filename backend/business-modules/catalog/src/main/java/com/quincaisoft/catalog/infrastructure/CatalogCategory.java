package com.quincaisoft.catalog.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.io.Serializable;
@Entity @Table(name="catalog_category") @IdClass(CatalogCategory.Key.class)
public class CatalogCategory {
 @Id @Column(name="tenant_id") public UUID tenantId; @Id @Column(name="company_id") public UUID companyId; @Id public UUID id;
 public String code;public String name;@Column(name="parent_id") public UUID parentId;public String brand;public String description;public boolean active;@Version public long version;
 public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable {}
}
