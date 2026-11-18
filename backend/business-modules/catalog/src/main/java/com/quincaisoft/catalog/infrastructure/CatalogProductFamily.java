package com.quincaisoft.catalog.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "catalog_product_family")
@IdClass(CatalogProductFamily.Key.class)
public class CatalogProductFamily {
    @Id @Column(name = "tenant_id") public UUID tenantId;
    @Id @Column(name = "company_id") public UUID companyId;
    @Id public UUID id;
    @Column(name = "manufacturer_id") public UUID manufacturerId;
    @Column(name = "product_type") public String productType;
    @Column(name = "model_number") public String modelNumber;

    public record Key(UUID tenantId, UUID companyId, UUID id) implements Serializable {}
}
