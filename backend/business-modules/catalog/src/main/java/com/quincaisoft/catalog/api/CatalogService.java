package com.quincaisoft.catalog.api;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.*;
public interface CatalogService {
 record Product(UUID id,String sku,String name,String baseUnit,@JsonFormat(shape=JsonFormat.Shape.STRING) long sellingPriceXaf,int taxBasisPoints,@JsonFormat(shape=JsonFormat.Shape.STRING) long version,boolean active,
                UUID categoryId,String brand,String barcode,String packSizeMilli,String reorderLevelMilli,boolean trackStock,boolean trackSerial,boolean trackExpiry,String description){
  public Product(UUID id,String sku,String name,String baseUnit,long sellingPriceXaf,int taxBasisPoints,long version,boolean active){this(id,sku,name,baseUnit,sellingPriceXaf,taxBasisPoints,version,active,null,"","","1000","0",true,false,false,"");}
 }
 record Save(UUID operationId,String sku,String name,String baseUnit,String sellingPriceXaf,int taxBasisPoints,String version,Boolean active,String reason,
             UUID categoryId,String brand,String barcode,String packSizeMilli,String reorderLevelMilli,Boolean trackStock,Boolean trackSerial,Boolean trackExpiry,String description){
  public Save(UUID operationId,String sku,String name,String baseUnit,String sellingPriceXaf,int taxBasisPoints,String version,Boolean active,String reason){this(operationId,sku,name,baseUnit,sellingPriceXaf,taxBasisPoints,version,active,reason,null,null,null,null,null,null,null,null,null);}
 }
 @com.quincaisoft.shared.TenantMutation Product create(Save command); @com.quincaisoft.shared.TenantMutation Product update(UUID id,Save command);
 record Page(List<Product> items,long total,int offset,int limit) {}
 Page page(String query,int offset,int limit);
 List<Product> list(); Product get(UUID id);
}
