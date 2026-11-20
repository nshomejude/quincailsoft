package com.quincaisoft.inventory.api;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.UUID;
public interface StockService {
 record StockResult(UUID movementId,@JsonFormat(shape=JsonFormat.Shape.STRING) long quantityMilli,@JsonFormat(shape=JsonFormat.Shape.STRING) long valueXaf){}
 void requireWarehouse(UUID warehouseId);
 @com.quincaisoft.shared.TenantMutation StockResult issue(UUID warehouseId,UUID productId,long quantityMilli,UUID operationId,String reason);
 @com.quincaisoft.shared.TenantMutation StockResult receive(UUID warehouseId,UUID productId,long quantityMilli,long valueXaf,UUID operationId,String reason);
 @com.quincaisoft.shared.TenantMutation StockResult issueToCustomerCustody(UUID warehouseId,UUID productId,long quantityMilli,UUID operationId,String reason,UUID saleId,UUID customerId,java.time.LocalDate businessDate);
 void requireCustodyCollected(UUID saleId);
}
