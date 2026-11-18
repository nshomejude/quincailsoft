package com.quincaisoft.catalog.infrastructure;
import com.quincaisoft.catalog.api.CatalogService;
import jakarta.persistence.*;
import java.util.UUID;
import java.io.Serializable;
@Entity @Table(name="catalog_product") @IdClass(CatalogProduct.Key.class)
public class CatalogProduct {
 @Id @Column(name="tenant_id") public UUID tenantId; @Id @Column(name="company_id") public UUID companyId; @Id public UUID id;
 public String sku; public String name; @Column(name="base_unit") public String baseUnit; @Column(name="selling_price_xaf") public long sellingPriceXaf;
 @Column(name="tax_basis_points") public int taxBasisPoints; public boolean active; @Version public long version;
 public CatalogProduct(){}
 public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable {}
 public CatalogService.Product view(){return new CatalogService.Product(id,sku,name,baseUnit,sellingPriceXaf,taxBasisPoints,version,active);}
}
