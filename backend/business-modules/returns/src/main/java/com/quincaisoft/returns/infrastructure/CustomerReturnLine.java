package com.quincaisoft.returns.infrastructure;import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="customer_return_line") @IdClass(CustomerReturnLine.Key.class) public class CustomerReturnLine {
@Id @Column(name="tenant_id") public UUID tenantId;
@Id @Column(name="company_id") public UUID companyId;
@Id @Column(name="id") public UUID id;
@Column(name="return_id") public UUID returnId;
@Column(name="product_id") public UUID productId;
@Column(name="quantity_milli") public long quantityMilli;
@Column(name="credit_xaf") public long creditXaf;
@Column(name="cost_xaf") public long costXaf;
public static class Key implements Serializable {public UUID tenantId,companyId,id;public Key(){}public Key(UUID t,UUID c,UUID i){tenantId=t;companyId=c;id=i;}public boolean equals(Object o){return o instanceof Key k&&Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}public int hashCode(){return Objects.hash(tenantId,companyId,id);}}}