package com.quincaisoft.suppliers.api;
import java.util.*;
public interface SupplierService {
 record Supplier(UUID id,String code,String name,String phone,String email,boolean active,long version,String contactPerson,String address,String taxReference,int paymentTermsDays,String bankReference){
  public Supplier(UUID id,String code,String name,String phone,String email,boolean active,long version){this(id,code,name,phone,email,active,version,"","","",0,"");}
 }
 record Create(UUID operationId,String code,String name,String phone,String email){}
 record Save(UUID operationId,String code,String name,String contactPerson,String phone,String email,String address,String taxReference,int paymentTermsDays,String bankReference,boolean active,String version,String reason){}
 record Page(List<Supplier> items,long total,int offset,int limit){}
 List<Supplier> list();Supplier get(UUID id);@com.quincaisoft.shared.TenantMutation Supplier create(Create request);void requireEligible(UUID id);
 @com.quincaisoft.shared.TenantMutation Supplier save(UUID id,Save request);Page page(String query,int offset,int limit);
}
