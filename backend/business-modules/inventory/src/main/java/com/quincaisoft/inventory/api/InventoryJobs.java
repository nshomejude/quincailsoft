package com.quincaisoft.inventory.api;
import java.util.*;import java.time.LocalDate;
public interface InventoryJobs {
 record CountStart(UUID operationId,UUID warehouseId,UUID productId,LocalDate businessDate,String reason){}
 record Transform(UUID operationId,UUID warehouseId,UUID sourceProduct,UUID outputProduct,String sourceQuantityMilli,String outputQuantityMilli,String parentLengthMm,String outputLengthMm,LocalDate businessDate,String evidenceReference,String reason){}
 record Action(UUID operationId,String version,String quantityMilli,String evidenceReference,String reason){}
 record Job(UUID id,String kind,String status,UUID warehouseId,UUID sourceProduct,UUID outputProduct,String sourceQuantityMilli,String outputQuantityMilli,String snapshotVersion,String snapshotValueXaf,String postedValueXaf,String version,LocalDate businessDate,String evidenceReference){}
 List<Job> list(String kind);Job get(UUID id);Job startCount(CountStart command);Job capture(UUID id,Action command);Job draftTransform(String kind,Transform command);Job approve(UUID id,Action command);Job post(UUID id,Action command);
}
