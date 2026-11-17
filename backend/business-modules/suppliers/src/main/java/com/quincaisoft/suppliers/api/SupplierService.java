package com.quincaisoft.suppliers.api;
import java.util.*;
public interface SupplierService {
 record Supplier(UUID id,String code,String name,String phone,String email,boolean active,long version){}
 record Create(UUID operationId,String code,String name,String phone,String email){}
 List<Supplier> list();Supplier get(UUID id);Supplier create(Create request);void requireEligible(UUID id);
}