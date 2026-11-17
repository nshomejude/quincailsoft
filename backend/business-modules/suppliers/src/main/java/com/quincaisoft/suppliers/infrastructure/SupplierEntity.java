package com.quincaisoft.suppliers.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="supplier") @IdClass(SupplierEntity.Key.class) public class SupplierEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="id") public UUID id;
 @Column(name="code") public String code;
 @Column(name="name") public String name;
 @Column(name="phone") public String phone;
 @Column(name="email") public String email;
 @Column(name="active") public boolean active;
 @Column(name="version") public long version;
 public SupplierEntity(){}
 public static class Key implements Serializable{public UUID tenantId,companyId,id;public Key(){}public Key(UUID t,UUID c,UUID i){tenantId=t;companyId=c;id=i;}public boolean equals(Object o){return o instanceof Key k&&Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}public int hashCode(){return Objects.hash(tenantId,companyId,id);}}
}