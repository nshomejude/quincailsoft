package com.quincaisoft.catalog.api;
import java.util.*;
public interface CatalogMasterService {
 record CategorySave(UUID operationId,String code,String name,UUID parentId,String brand,String description,boolean active,String version,String reason) {}
 record Category(UUID id,String code,String name,UUID parentId,String brand,String description,boolean active,String version) {}
 record UnitSave(UUID operationId,String code,String name,String baseUnit,String factorNumerator,String factorDenominator,int precision,boolean active,String version,String reason) {}
 record Unit(UUID id,String code,String name,String baseUnit,String factorNumerator,String factorDenominator,int precision,boolean active,String version) {}
 record ManufacturerSave(UUID operationId,String code,String name,String reason) {}
 record Manufacturer(UUID id,String code,String name) {}
 record FamilySave(UUID operationId,UUID manufacturerId,String productType,String modelNumber,String reason) {}
 record Family(UUID id,UUID manufacturerId,String productType,String modelNumber) {}
 Manufacturer createManufacturer(ManufacturerSave command);
 Family createFamily(FamilySave command);
 record VariantSave(UUID operationId,UUID productId,UUID familyId,String colour,String size,String reason) {}
 record Variant(UUID productId,UUID familyId,String colour,String size) {}
 record OfferSave(UUID operationId,UUID productId,UUID supplierId,String supplierSku,String quotedCostXaf,int leadTimeDays,String minimumOrderMilli,boolean preferred,String reason) {}
 record Offer(UUID productId,UUID supplierId,String supplierSku,String quotedCostXaf,int leadTimeDays,String minimumOrderMilli,boolean preferred) {}
 Variant attachVariant(VariantSave command);
 Offer createOffer(OfferSave command);
 List<Category> categories(); Category category(UUID id); Category saveCategory(UUID id,CategorySave command);
 List<Unit> units(); Unit unit(UUID id); Unit saveUnit(UUID id,UnitSave command);
}
