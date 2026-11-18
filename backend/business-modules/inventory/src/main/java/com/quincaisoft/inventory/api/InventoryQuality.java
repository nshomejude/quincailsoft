package com.quincaisoft.inventory.api;
import java.util.*;import java.time.*;
/** Lot registration classifies existing reconciled stock; it is not a goods receipt. */
public interface InventoryQuality {
 record RegisterLot(UUID operationId,UUID warehouseId,UUID productId,String lotNumber,String quantityMilli,LocalDate expiresOn,String sourceReference,String reason){}
 record Lot(UUID id,UUID warehouseId,UUID productId,String lotNumber,String quantityMilli,String quarantinedMilli,LocalDate expiresOn,String sourceReference,String version){}
 record DraftRecall(UUID operationId,UUID lotId,String quantityMilli,LocalDate businessDate,String reason){}
 record Action(UUID operationId,String version,String evidenceReference,String reason){}
 record Recall(UUID id,UUID lotId,String quantityMilli,String status,LocalDate businessDate,String evidenceReference,String version){}
 record Reserve(UUID operationId,UUID warehouseId,UUID productId,String quantityMilli,String sourceReference,String reason){}
 record Release(UUID operationId,String version,String quantityMilli,String reason){}
 record Reservation(UUID id,UUID warehouseId,UUID productId,String quantityMilli,String releasedMilli,String status,String sourceReference,String version){}
 List<Lot> lots();Lot registerLot(RegisterLot command);List<Recall> recalls();Recall draftRecall(DraftRecall command);
 Recall approve(UUID id,Action command);Recall block(UUID id,Action command);Recall recover(UUID id,Action command);Recall dispose(UUID id,Action command);
 List<Reservation> reservations();Reservation reserve(Reserve command);Reservation release(UUID id,Release command);
}
