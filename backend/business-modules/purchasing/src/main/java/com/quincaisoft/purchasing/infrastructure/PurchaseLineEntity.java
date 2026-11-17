package com.quincaisoft.purchasing.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="purchase_line") @IdClass(PurchaseLineEntity.Key.class) public class PurchaseLineEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="id") public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="purchase_id") public UUID purchaseId;
 @Column(name="product_id") public UUID productId;
 @Column(name="quantity_milli") public long quantityMilli;
 @Column(name="received_milli") public long receivedMilli;
 @Column(name="unit_price_xaf") public long unitPriceXaf;
 @Column(name="total_xaf") public long totalXaf;
 public PurchaseLineEntity(){}
 public static class Key implements Serializable{public UUID tenantId,companyId,id;public Key(){}public Key(UUID t,UUID c,UUID i){tenantId=t;companyId=c;id=i;}public boolean equals(Object o){return o instanceof Key k&&Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}public int hashCode(){return Objects.hash(tenantId,companyId,id);}}
}