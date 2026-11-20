package com.quincaisoft.catalog.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "catalog_manufacturer")
@IdClass(CatalogManufacturer.Key.class)
public class CatalogManufacturer {
    @Id @Column(name = "tenant_id") public UUID tenantId;
    @Id @Column(name = "company_id") public UUID companyId;
    @Id public UUID id;
    public String code;
    public String name;

    public record Key(UUID tenantId, UUID companyId, UUID id) implements Serializable {}
}
