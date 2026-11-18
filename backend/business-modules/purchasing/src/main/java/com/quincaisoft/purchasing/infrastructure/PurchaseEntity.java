package com.quincaisoft.purchasing.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="purchase_order") @IdClass(PurchaseEntity.Key.class) public class PurchaseEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="id") public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="supplier_id") public UUID supplierId;
 @Column(name="warehouse_id") public UUID warehouseId;
 @Column(name="created_by") public UUID createdBy;
 @Column(name="approved_by") public UUID approvedBy;
 @Column(name="business_date") public LocalDate businessDate;
 @Column(name="status") public String status;
 @Column(name="version") public long version;
 @Column(name="total_xaf") public long totalXaf;
 public PurchaseEntity(){}
 public static class Key implements Serializable{public UUID tenantId,companyId,id;public Key(){}public Key(UUID t,UUID c,UUID i){tenantId=t;companyId=c;id=i;}public boolean equals(Object o){return o instanceof Key k&&Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}public int hashCode(){return Objects.hash(tenantId,companyId,id);}}
}