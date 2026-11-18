package com.quincaisoft.catalog.api;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.*;
public interface CatalogService {
 record Product(UUID id,String sku,String name,String baseUnit,@JsonFormat(shape=JsonFormat.Shape.STRING) long sellingPriceXaf,int taxBasisPoints,@JsonFormat(shape=JsonFormat.Shape.STRING) long version,boolean active){}
 record Save(UUID operationId,String sku,String name,String baseUnit,String sellingPriceXaf,int taxBasisPoints,String version,Boolean active,String reason){}
 Product create(Save command); Product update(UUID id,Save command);
 List<Product> list(); Product get(UUID id);
}
