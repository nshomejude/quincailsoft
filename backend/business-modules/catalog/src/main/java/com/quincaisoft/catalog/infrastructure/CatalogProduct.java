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
 @Column(name="category_id") public UUID categoryId;public String brand="";public String barcode="";
 @Column(name="pack_size_milli") public long packSizeMilli=1000;@Column(name="reorder_level_milli") public long reorderLevelMilli;
 @Column(name="track_stock") public boolean trackStock=true;@Column(name="track_serial") public boolean trackSerial;@Column(name="track_expiry") public boolean trackExpiry;public String description="";
 public CatalogProduct(){}
 public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable {}
 public CatalogService.Product view(){return new CatalogService.Product(id,sku,name,baseUnit,sellingPriceXaf,taxBasisPoints,version,active,categoryId,brand,barcode,Long.toString(packSizeMilli),Long.toString(reorderLevelMilli),trackStock,trackSerial,trackExpiry,description);}
}
