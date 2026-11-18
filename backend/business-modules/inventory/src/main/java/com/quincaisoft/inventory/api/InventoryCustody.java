package com.quincaisoft.inventory.api;
import java.util.*;import java.time.*;
public interface InventoryCustody {
 record PolicyRequest(UUID operationId,String mode,LocalDate effectiveFrom,String contractReference,String reason){}
 record Approve(UUID operationId,String version,String reason){}
 record Policy(UUID id,String mode,LocalDate effectiveFrom,String contractReference,String status,String version){}
 record Position(UUID id,UUID saleId,UUID customerId,UUID warehouseId,UUID productId,String quantityMilli,String collectedMilli,String frozenCostXaf,String status,String version){}
 record Collect(UUID operationId,String version,String quantityMilli,String proofReference,String reason){}
 record Collection(UUID id,UUID custodyId,String quantityMilli,String proofReference,UUID actorId,Instant collectedAt){}
 List<Policy> policies();Policy requestPolicy(PolicyRequest request);Policy approvePolicy(UUID id,Approve request);
 List<Position> positions();Position collect(UUID id,Collect request);List<Collection> collections(UUID custodyId);
}
