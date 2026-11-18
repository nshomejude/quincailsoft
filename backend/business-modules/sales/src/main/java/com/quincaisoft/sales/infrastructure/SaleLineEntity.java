package com.quincaisoft.sales.infrastructure;
import jakarta.persistence.*;
import java.util.*;
import java.time.*;
import java.io.Serializable;
@Entity @Table(name="sale_line") @IdClass(SaleLineEntity.Key.class)
public class SaleLineEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="sale_id") public UUID saleId;
 @Id @Column(name="id") public UUID id;
 @Column(name="product_id") public UUID productId;
 @Column(name="quantity_milli") public long quantityMilli;
 @Column(name="unit_price_xaf") public long unitPriceXaf;
 @Column(name="line_total_xaf") public long lineTotalXaf;
 @Column(name="cost_xaf") public long costXaf;
 public SaleLineEntity() {}
 public static class Key implements Serializable {
  public UUID tenantId;
  public UUID companyId;
  public UUID saleId;
  public UUID id;
  public Key() {}
  public Key(UUID tenantId,UUID companyId,UUID saleId,UUID id) {this.tenantId=tenantId;this.companyId=companyId;this.saleId=saleId;this.id=id;}
  public boolean equals(Object o) {return o instanceof Key k && Objects.equals(tenantId,k.tenantId) && Objects.equals(companyId,k.companyId) && Objects.equals(saleId,k.saleId) && Objects.equals(id,k.id);}
  public int hashCode(){return Objects.hash(tenantId,companyId,saleId,id);}
 }
}
