package com.quincaisoft.sales.api;
import java.util.*;import java.time.*;
public interface FulfillmentService {
 record DeliveryDetails(String method,LocalDate scheduledDate,String address,
                        String recipientName,String recipientPhone,String notes) {}
 record Draft(UUID operationId,UUID saleId,UUID customerId,LocalDate businessDate,String reason,
              DeliveryDetails delivery) {
  public Draft(UUID operationId,UUID saleId,UUID customerId,LocalDate businessDate,String reason) {
   this(operationId,saleId,customerId,businessDate,reason,null);
  }

  // Preserve the request fingerprint for existing draft retries.
  public String legacyFingerprintMaterial() {
   return "Draft[operationId="+operationId+", saleId="+saleId+", customerId="+customerId+
       ", businessDate="+businessDate+", reason="+reason+"]";
  }
 }
 record Action(UUID operationId,String version,String proofReference,String reason){}
 record Fulfillment(UUID id,UUID saleId,UUID customerId,String status,LocalDate businessDate,String proofReference,String version,DeliveryDetails delivery){}
 record DispatchLines(UUID operationId,String version,String reason,List<PickLine> lines) {}
 @com.quincaisoft.shared.TenantMutation Fulfillment dispatchLines(UUID id,DispatchLines command);
 record DispatchEntry(UUID operationId,UUID productId,String quantityMilli,Instant dispatchedAt,UUID dispatchedBy) {}
 List<DispatchEntry> dispatchHistory(UUID fulfillmentId);
 record PickLine(UUID productId,String quantityMilli) {}
 record PickList(UUID fulfillmentId,UUID saleId,UUID warehouseId,String status,List<PickLine> lines) {}
 PickList pickList(UUID fulfillmentId);
 long pendingPreparationCount();
 record QueueEntry(Fulfillment fulfillment,String customerName,UUID warehouseId) {}
 record QueuePage(List<QueueEntry> items,String nextCursor) {}
 QueuePage page(String status,String query,int limit,String cursor);
 List<Fulfillment> list();Fulfillment get(UUID id);@com.quincaisoft.shared.TenantMutation Fulfillment draft(Draft c);@com.quincaisoft.shared.TenantMutation Fulfillment dispatch(UUID id,Action c);@com.quincaisoft.shared.TenantMutation Fulfillment complete(UUID id,Action c);

}
