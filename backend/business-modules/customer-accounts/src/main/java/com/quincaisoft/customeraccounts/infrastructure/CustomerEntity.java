package com.quincaisoft.customeraccounts.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.io.Serializable;
@Entity @Table(name="customer") @IdClass(CustomerEntity.Key.class)
public class CustomerEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 public String name;public boolean active;
 @Column(name="customer_type") public String type="Individual";
 public String phone="";public String email="";public String address="";
 @Column(name="tax_reference") public String taxReference="";
 @Column(name="credit_limit_xaf") public long creditLimitXaf;
 @Column(name="payment_terms_days") public int paymentTermsDays;
 public boolean consent;
 @Version public long version;
 public CustomerEntity(){}
 public static class Key implements Serializable {
  public UUID tenantId,companyId,id;public Key(){}public Key(UUID t,UUID c,UUID i){tenantId=t;companyId=c;id=i;}
  public boolean equals(Object o){return o instanceof Key k&&Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}
  public int hashCode(){return Objects.hash(tenantId,companyId,id);}
 }
}
