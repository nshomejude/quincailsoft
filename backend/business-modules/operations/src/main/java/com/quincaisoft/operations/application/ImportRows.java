package com.quincaisoft.operations.application;
import com.quincaisoft.operations.api.OperationsService.Row;import com.quincaisoft.operations.infrastructure.ImportRow;import java.util.UUID;
final class ImportRows {
 static ImportRow entity(UUID preview,Row row){var r=new ImportRow();r.scope(UUID.randomUUID());r.previewId=preview;r.rowNumber=row.row();
  r.name=row.name();
  r.code=row.code();
  r.baseUnit=row.baseUnit();
  r.sellingPriceXaf=row.sellingPriceXaf();
  r.taxBasisPoints=row.taxBasisPoints();
  r.relationId=row.relationId();
  r.brand=row.brand();
  r.barcode=row.barcode();
  r.description=row.description();
  r.packSize=row.packSize();
  r.reorderLevel=row.reorderLevel();
  r.trackStock=row.trackStock();
  r.trackSerial=row.trackSerial();
  r.trackExpiry=row.trackExpiry();
  r.customerType=row.customerType();
  r.contactPerson=row.contactPerson();
  r.phone=row.phone();
  r.email=row.email();
  r.address=row.address();
  r.taxReference=row.taxReference();
  r.creditLimitXaf=row.creditLimitXaf();
  r.paymentTermsDays=row.paymentTermsDays();
  r.consent=row.consent();
  r.bankReference=row.bankReference();
  r.symbol=row.symbol();
  r.conversionFactor=row.conversionFactor();
  r.roundingPrecision=row.roundingPrecision();
  return r;
 }
 static Row view(ImportRow r){return new Row(r.rowNumber,r.name,r.code,r.baseUnit,r.sellingPriceXaf,r.taxBasisPoints,r.relationId,r.brand,r.barcode,r.description,r.packSize,r.reorderLevel,r.trackStock,r.trackSerial,r.trackExpiry,r.customerType,r.contactPerson,r.phone,r.email,r.address,r.taxReference,r.creditLimitXaf,r.paymentTermsDays,r.consent,r.bankReference,r.symbol,r.conversionFactor,r.roundingPrecision);}
}
