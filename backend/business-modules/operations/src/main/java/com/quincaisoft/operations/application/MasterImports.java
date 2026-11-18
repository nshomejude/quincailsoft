package com.quincaisoft.operations.application;

import com.quincaisoft.operations.api.OperationsService.*;
import com.quincaisoft.catalog.api.*;
import com.quincaisoft.customeraccounts.api.CustomerDirectory;
import com.quincaisoft.suppliers.api.SupplierService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.util.*;

@Component public class MasterImports {
 private final CatalogService products;private final CatalogMasterService masters;private final CustomerDirectory customers;private final SupplierService suppliers;private final EntityManager em;
 public MasterImports(CatalogService products,CatalogMasterService masters,CustomerDirectory customers,SupplierService suppliers,EntityManager em){this.products=products;this.masters=masters;this.customers=customers;this.suppliers=suppliers;this.em=em;}
 public void validate(Entity entity,Row row){
  if(row.relationId()!=null)masters.category(row.relationId());
  if(entity==Entity.PRODUCT){Number count=(Number)em.createNativeQuery("select count(*) from catalog_product where sku=:s").setParameter("s",row.code()).getSingleResult();if(count.longValue()!=0)throw new IllegalArgumentException("SKU already exists in this company");}
  if(entity==Entity.UNIT&&!row.symbol().equals(row.baseUnit())&&masters.units().stream().noneMatch(u->u.code().equals(row.baseUnit())&&u.active()&&u.code().equals(u.baseUnit())))throw new IllegalArgumentException("Active base unit in current company required");
  if(entity==Entity.UNIT&&row.symbol().equals(row.baseUnit())&&!Arrays.equals(ImportValidation.rational(row.conversionFactor()),new String[]{"1","1"}))throw new IllegalArgumentException("Base unit conversion must equal one");
 }
 public UUID create(Entity entity,Row r,UUID op,String reason){
  String code=r.code().isEmpty()?"IMP-"+op.toString().substring(0,30):r.code();
  return switch(entity){
   case PRODUCT->products.create(new CatalogService.Save(op,r.code(),r.name(),r.baseUnit(),r.sellingPriceXaf(),Integer.parseInt(r.taxBasisPoints()),"0",false,reason,r.relationId(),r.brand(),r.barcode(),r.packSize(),r.reorderLevel(),r.trackStock(),r.trackSerial(),r.trackExpiry(),r.description())).id();
   case CUSTOMER->customers.create(new CustomerDirectory.Save(op,r.name(),r.customerType(),r.phone(),r.email(),r.address(),r.taxReference(),r.creditLimitXaf(),r.paymentTermsDays(),r.consent(),false,"0",reason)).id();
   case SUPPLIER->suppliers.save(null,new SupplierService.Save(op,code,r.name(),r.contactPerson(),r.phone(),r.email(),r.address(),r.taxReference(),r.paymentTermsDays(),r.bankReference(),false,"0",reason)).id();
   case CATEGORY->masters.saveCategory(null,new CatalogMasterService.CategorySave(op,code,r.name(),r.relationId(),r.brand(),r.description(),false,"0",reason)).id();
   case UNIT->{String[] factor=ImportValidation.rational(r.conversionFactor());yield masters.saveUnit(null,new CatalogMasterService.UnitSave(op,r.symbol(),r.name(),r.baseUnit(),factor[0],factor[1],r.roundingPrecision(),false,"0",reason)).id();}
  };
 }
}
