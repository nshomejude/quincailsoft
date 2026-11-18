package com.quincaisoft.returns.api;import java.util.*;import java.time.*;
public interface ReturnService {
 record RequestedLine(UUID productId,String quantityMilli){}
 record Draft(UUID operationId,UUID saleId,LocalDate businessDate,String condition,String evidenceReference,List<RequestedLine> lines){}
 record Command(UUID operationId,long version,String reason){}
 record Line(UUID productId,String quantityMilli,String creditXaf){}
 record CustomerReturn(UUID id,UUID saleId,String status,LocalDate businessDate,long version,String evidenceReference,String creditXaf,String customerCreditXaf,List<Line> lines){}
 List<CustomerReturn> list();CustomerReturn get(UUID id);@com.quincaisoft.shared.TenantMutation CustomerReturn draft(Draft request);@com.quincaisoft.shared.TenantMutation CustomerReturn approve(UUID id,Command command);@com.quincaisoft.shared.TenantMutation CustomerReturn post(UUID id,Command command);
}