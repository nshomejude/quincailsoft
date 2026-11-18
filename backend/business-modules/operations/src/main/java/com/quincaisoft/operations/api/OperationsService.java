package com.quincaisoft.operations.api;

import java.time.Instant;
import java.util.*;

public interface OperationsService {
 enum Entity { PRODUCT, CUSTOMER, SUPPLIER, CATEGORY, UNIT }
 record PreviewRequest(UUID operationId,Entity entity,String csv,String delimiter,Map<String,String> mapping) {}
 record Row(int row,String name,String code,String baseUnit,String sellingPriceXaf,String taxBasisPoints,UUID relationId,String brand,String barcode,String description,String packSize,String reorderLevel,boolean trackStock,boolean trackSerial,boolean trackExpiry,String customerType,String contactPerson,String phone,String email,String address,String taxReference,String creditLimitXaf,int paymentTermsDays,boolean consent,String bankReference,String symbol,String conversionFactor,int roundingPrecision) {}
 record RowError(int row,String field,String error) {}
 record Preview(UUID id,Entity entity,String digest,int count,List<Row> rows,List<RowError> errors,boolean canCommit,Instant expiresAt,String version) {}
 record Commit(UUID operationId,String digest,String version,String reason) {}
 record ImportResult(UUID id,Entity entity,int count,List<UUID> records) {}
 record Command(UUID operationId,String version,String reason) {}
 record ExportRequest(UUID operationId,Entity entity,String reason) {}
 record Export(UUID id,Entity entity,String status,int rowCount,String digest,Instant createdAt,Instant expiresAt,String version,String failure) {}
 record Content(UUID id,String fileName,String contentType,String digest,String content) {}
 record Notice(UUID id,String kind,String message,UUID sourceId,Instant createdAt,Instant readAt,String version) {}
 record BackupRequest(UUID operationId,String reason) {}
 record Section(String name,long rows,String digest) {}
 record Backup(UUID id,String scope,String digest,List<Section> sections,Instant createdAt,String status) {}
 record Verify(UUID id,boolean valid,String digest,String scope) {}
 record Rehearse(UUID operationId,String expectedStateDigest,String reason) {}
 record Rehearsal(UUID id,UUID backupId,String status,String currentDigest,List<String> differences,List<String> requiredControls,boolean productionRestoreAllowed) {}
 record Integrity(boolean consistent,String stateDigest,List<Section> sections,List<String> checks,List<String> notChecked) {}
 @com.quincaisoft.shared.TenantMutation Preview preview(PreviewRequest request);Preview preview(UUID id);@com.quincaisoft.shared.TenantMutation ImportResult commit(UUID id,Commit request);
 List<Export> exports();Export export(ExportRequest request);Export export(UUID id);Export runExport(UUID id,Command request);Export cancelExport(UUID id,Command request);Content content(UUID id);
 List<Notice> notifications();@com.quincaisoft.shared.TenantMutation Notice read(UUID id,Command request);
 List<Backup> backups();Backup backup(BackupRequest request);Verify verify(UUID id);Rehearsal rehearse(UUID id,Rehearse request);Integrity integrity();
}
