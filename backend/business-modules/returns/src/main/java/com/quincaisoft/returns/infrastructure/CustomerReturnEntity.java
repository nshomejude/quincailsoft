package com.quincaisoft.returns.infrastructure;import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="customer_return") @IdClass(CustomerReturnEntity.Key.class) public class CustomerReturnEntity {
@Id @Column(name="tenant_id") public UUID tenantId;
@Id @Column(name="company_id") public UUID companyId;
@Id @Column(name="id") public UUID id;
@Column(name="branch_id") public UUID branchId;
@Column(name="sale_id") public UUID saleId;
@Column(name="warehouse_id") public UUID warehouseId;
@Column(name="customer_id") public UUID customerId;
@Column(name="created_by") public UUID createdBy;
@Column(name="approved_by") public UUID approvedBy;
@Column(name="operation_id") public UUID operationId;
@Column(name="status") public String status;
@Column(name="condition_code") public String conditionCode;
@Column(name="evidence_reference") public String evidenceReference;
@Column(name="business_date") public LocalDate businessDate;
@Column(name="version") public long version;
@Column(name="credit_xaf") public long creditXaf;
@Column(name="customer_credit_xaf") public long customerCreditXaf;
@Column(name="created_at") public Instant createdAt;
public static class Key implements Serializable {public UUID tenantId,companyId,id;public Key(){}public Key(UUID t,UUID c,UUID i){tenantId=t;companyId=c;id=i;}public boolean equals(Object o){return o instanceof Key k&&Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}public int hashCode(){return Objects.hash(tenantId,companyId,id);}}}