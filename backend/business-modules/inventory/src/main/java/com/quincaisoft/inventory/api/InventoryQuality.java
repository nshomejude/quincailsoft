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
 List<Lot> lots();@com.quincaisoft.shared.TenantMutation Lot registerLot(RegisterLot command);List<Recall> recalls();@com.quincaisoft.shared.TenantMutation Recall draftRecall(DraftRecall command);
 @com.quincaisoft.shared.TenantMutation Recall approve(UUID id,Action command);@com.quincaisoft.shared.TenantMutation Recall block(UUID id,Action command);@com.quincaisoft.shared.TenantMutation Recall recover(UUID id,Action command);@com.quincaisoft.shared.TenantMutation Recall dispose(UUID id,Action command);
 List<Reservation> reservations();@com.quincaisoft.shared.TenantMutation Reservation reserve(Reserve command);@com.quincaisoft.shared.TenantMutation Reservation release(UUID id,Release command);
}
