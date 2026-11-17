package com.quincaisoft.catalog.api;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.*;
public interface CatalogService {
 record Product(UUID id,String sku,String name,String baseUnit,@JsonFormat(shape=JsonFormat.Shape.STRING) long sellingPriceXaf,int taxBasisPoints,@JsonFormat(shape=JsonFormat.Shape.STRING) long version,boolean active){}
 List<Product> list(); Product get(UUID id);
}
