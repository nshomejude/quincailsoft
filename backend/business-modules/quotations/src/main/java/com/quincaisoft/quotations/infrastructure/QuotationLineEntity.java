package com.quincaisoft.quotations.infrastructure;
import jakarta.persistence.*;
import java.util.*;
import java.time.*;
import java.io.Serializable;
@Entity @Table(name="quotation_line") @IdClass(QuotationLineEntity.Key.class)
public class QuotationLineEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="quotation_id") public UUID quotationId;
 @Id @Column(name="id") public UUID id;
 @Column(name="product_id") public UUID productId;
 @Column(name="quantity_milli") public long quantityMilli;
 @Column(name="unit_price_xaf") public long unitPriceXaf;
 @Column(name="line_total_xaf") public long lineTotalXaf;
 @Column(name="cost_xaf") public long costXaf;
 public QuotationLineEntity() {}
 public static class Key implements Serializable {
  public UUID tenantId;
  public UUID companyId;
  public UUID quotationId;
  public UUID id;
  public Key() {}
  public Key(UUID tenantId,UUID companyId,UUID quotationId,UUID id) {this.tenantId=tenantId;this.companyId=companyId;this.quotationId=quotationId;this.id=id;}
  public boolean equals(Object o) {return o instanceof Key k && Objects.equals(tenantId,k.tenantId) && Objects.equals(companyId,k.companyId) && Objects.equals(quotationId,k.quotationId) && Objects.equals(id,k.id);}
  public int hashCode(){return Objects.hash(tenantId,companyId,quotationId,id);}
 }
}
