package com.quincaisoft.inventory.api;
import java.util.*;import com.fasterxml.jackson.annotation.JsonFormat;
public interface InventoryWorkflows {
 record Warehouse(UUID id,String name,boolean active,@JsonFormat(shape=JsonFormat.Shape.STRING) long version){}
 record Balance(UUID warehouseId,UUID productId,@JsonFormat(shape=JsonFormat.Shape.STRING) long quantityMilli,@JsonFormat(shape=JsonFormat.Shape.STRING) long reservedMilli,@JsonFormat(shape=JsonFormat.Shape.STRING) long allocatedMilli,@JsonFormat(shape=JsonFormat.Shape.STRING) long valueXaf,@JsonFormat(shape=JsonFormat.Shape.STRING) long version,@JsonFormat(shape=JsonFormat.Shape.STRING) long quarantinedMilli,@JsonFormat(shape=JsonFormat.Shape.STRING) long availableMilli){}
 record Draft(UUID operationId,UUID originWarehouse,UUID destinationWarehouse,UUID productId,String quantityMilli,java.time.LocalDate businessDate,String reason){}
 record Command(UUID operationId,String version,String quantityMilli,String reason){}
 record Transfer(UUID id,UUID originWarehouse,UUID destinationWarehouse,UUID productId,String quantityMilli,String receivedMilli,String valueXaf,String receivedValueXaf,String status,String version){}
 long lowStockCount();
 List<Warehouse> warehouses(); List<Balance> balances(UUID warehouseId); List<Transfer> transfers();Transfer get(UUID id);
 @com.quincaisoft.shared.TenantMutation Transfer draft(Draft command);@com.quincaisoft.shared.TenantMutation Transfer approve(UUID id,Command command);@com.quincaisoft.shared.TenantMutation Transfer dispatch(UUID id,Command command);@com.quincaisoft.shared.TenantMutation Transfer receive(UUID id,Command command);
}
