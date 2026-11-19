package com.quincaisoft.quotations.api;import java.util.*;import java.time.*;import com.quincaisoft.sales.api.SalesService;
public interface QuotationService {
 record Draft(UUID operationId,UUID customerId,UUID warehouseId,LocalDate businessDate,LocalDate validUntil,List<SalesService.RequestedLine> lines){}
 record Command(UUID operationId,long version,String reason){}
 record Decision(UUID operationId,long version,String reason,String evidenceReference){}
 record CustomerDecision(UUID id,String decision,UUID actorId,String reason,String evidenceReference,Instant recordedAt){}
 @com.quincaisoft.shared.TenantMutation Quote accept(UUID id,Decision decision);
 @com.quincaisoft.shared.TenantMutation Quote reject(UUID id,Decision decision);
 List<CustomerDecision> decisions(UUID id);
 record Quote(UUID id,String status,UUID customerId,UUID warehouseId,LocalDate businessDate,LocalDate validUntil,String totalXaf,long version,UUID convertedSaleId,List<SalesService.Line> lines){}
 List<Quote> list();Quote get(UUID id);@com.quincaisoft.shared.TenantMutation Quote draft(Draft request);@com.quincaisoft.shared.TenantMutation Quote issue(UUID id,Command command);@com.quincaisoft.shared.TenantMutation Quote convert(UUID id,Command command);
}
