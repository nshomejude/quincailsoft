package com.quincaisoft.catalog.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity @Table(name="catalog_variant_identity") @IdClass(CatalogVariantIdentity.Key.class)
public class CatalogVariantIdentity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="product_id") public UUID productId;
 @Column(name="family_id") public UUID familyId;
 public String colour;
 public String size;
 public record Key(UUID tenantId, UUID companyId, UUID productId) implements Serializable {}
}
