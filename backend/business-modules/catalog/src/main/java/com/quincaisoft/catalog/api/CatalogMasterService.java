package com.quincaisoft.catalog.api;
import java.util.*;
public interface CatalogMasterService {
 record CategorySave(UUID operationId,String code,String name,UUID parentId,String brand,String description,boolean active,String version,String reason) {}
 record Category(UUID id,String code,String name,UUID parentId,String brand,String description,boolean active,String version) {}
 record UnitSave(UUID operationId,String code,String name,String baseUnit,String factorNumerator,String factorDenominator,int precision,boolean active,String version,String reason) {}
 record Unit(UUID id,String code,String name,String baseUnit,String factorNumerator,String factorDenominator,int precision,boolean active,String version) {}
 List<Category> categories(); Category category(UUID id); Category saveCategory(UUID id,CategorySave command);
 List<Unit> units(); Unit unit(UUID id); Unit saveUnit(UUID id,UnitSave command);
}
