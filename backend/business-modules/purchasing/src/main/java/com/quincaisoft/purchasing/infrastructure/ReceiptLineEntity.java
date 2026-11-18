package com.quincaisoft.purchasing.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="purchase_receipt_line") @IdClass(ReceiptLineEntity.Key.class) public class ReceiptLineEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="id") public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="receipt_id") public UUID receiptId;
 @Column(name="purchase_line_id") public UUID purchaseLineId;
 @Column(name="product_id") public UUID productId;
 @Column(name="quantity_milli") public long quantityMilli;
 @Column(name="value_xaf") public long valueXaf;
 public ReceiptLineEntity(){}
 public static class Key implements Serializable{public UUID tenantId,companyId,id;public Key(){}public Key(UUID t,UUID c,UUID i){tenantId=t;companyId=c;id=i;}public boolean equals(Object o){return o instanceof Key k&&Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}public int hashCode(){return Objects.hash(tenantId,companyId,id);}}
}