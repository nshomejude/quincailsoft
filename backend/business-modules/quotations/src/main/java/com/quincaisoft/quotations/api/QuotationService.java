package com.quincaisoft.quotations.api;import java.util.*;import java.time.*;import com.quincaisoft.sales.api.SalesService;
public interface QuotationService {
 record Draft(UUID operationId,UUID customerId,UUID warehouseId,LocalDate businessDate,LocalDate validUntil,List<SalesService.RequestedLine> lines){}
 record Command(UUID operationId,long version,String reason){}
 record Quote(UUID id,String status,UUID customerId,UUID warehouseId,LocalDate businessDate,LocalDate validUntil,String totalXaf,long version,UUID convertedSaleId,List<SalesService.Line> lines){}
 List<Quote> list();Quote get(UUID id);Quote draft(Draft request);Quote issue(UUID id,Command command);Quote convert(UUID id,Command command);
}