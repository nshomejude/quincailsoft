package com.quincaisoft.catalog.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity @Table(name="catalog_supplier_offer") @IdClass(CatalogSupplierOffer.Key.class)
public class CatalogSupplierOffer {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="product_id") public UUID productId;
 @Id @Column(name="supplier_id") public UUID supplierId;
 @Column(name="supplier_sku") public String supplierSku;
 @Column(name="quoted_cost_xaf") public long quotedCostXaf;
 @Column(name="lead_time_days") public int leadTimeDays;
 @Column(name="minimum_order_milli") public long minimumOrderMilli;
 public boolean preferred;
 public record Key(UUID tenantId, UUID companyId, UUID productId, UUID supplierId) implements Serializable {}
}
