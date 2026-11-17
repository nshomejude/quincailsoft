package com.quincaisoft.quotations.infrastructure;
import jakarta.persistence.*;
import java.util.*;
import java.time.*;
import java.io.Serializable;
@Entity @Table(name="quotation") @IdClass(QuotationEntity.Key.class)
public class QuotationEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="id") public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="operation_id") public UUID operationId;
 @Column(name="warehouse_id") public UUID warehouseId;
 @Column(name="customer_id") public UUID customerId;
 @Column(name="actor_id") public UUID actorId;
 @Column(name="business_date") public LocalDate businessDate;
 @Column(name="status") public String status;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) @Column(name="currency",length=3) public String currency;
 @Column(name="total_xaf") public long totalXaf;
 @Column(name="valid_until") public LocalDate validUntil;
 @Column(name="converted_sale_id") public UUID convertedSaleId;
 @Column(name="version") public long version;
 @Column(name="created_at") public Instant createdAt;
 public QuotationEntity() {}
 public static class Key implements Serializable {
  public UUID tenantId;
  public UUID companyId;
  public UUID id;
  public Key() {}
  public Key(UUID tenantId,UUID companyId,UUID id) {this.tenantId=tenantId;this.companyId=companyId;this.id=id;}
  public boolean equals(Object o) {return o instanceof Key k && Objects.equals(tenantId,k.tenantId) && Objects.equals(companyId,k.companyId) && Objects.equals(id,k.id);}
  public int hashCode(){return Objects.hash(tenantId,companyId,id);}
 }
}
