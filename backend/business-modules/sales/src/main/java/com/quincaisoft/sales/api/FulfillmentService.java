package com.quincaisoft.sales.api;
import java.util.*;import java.time.*;
public interface FulfillmentService {
 record Draft(UUID operationId,UUID saleId,UUID customerId,LocalDate businessDate,String reason){}
 record Action(UUID operationId,String version,String proofReference,String reason){}
 record Fulfillment(UUID id,UUID saleId,UUID customerId,String status,LocalDate businessDate,String proofReference,String version){}
 List<Fulfillment> list();Fulfillment get(UUID id);Fulfillment draft(Draft c);Fulfillment dispatch(UUID id,Action c);Fulfillment complete(UUID id,Action c);
}
