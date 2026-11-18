package com.quincaisoft.customeraccounts.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.io.Serializable;
@Entity @Table(name="customer") @IdClass(CustomerEntity.Key.class)
public class CustomerEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 public String name;public boolean active;
 public CustomerEntity(){}
 public static class Key implements Serializable {
  public UUID tenantId,companyId,id;public Key(){}public Key(UUID t,UUID c,UUID i){tenantId=t;companyId=c;id=i;}
  public boolean equals(Object o){return o instanceof Key k&&Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}
  public int hashCode(){return Objects.hash(tenantId,companyId,id);}
 }
}
