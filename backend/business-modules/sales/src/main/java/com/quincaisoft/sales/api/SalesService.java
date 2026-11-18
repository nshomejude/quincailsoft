package com.quincaisoft.sales.api;
import java.util.*;import java.time.*;
public interface SalesService {
 public record RequestedLine(UUID productId,String quantityMilli){}
 public record Draft(UUID operationId,UUID warehouseId,UUID customerId,LocalDate businessDate,List<RequestedLine> lines){}
 public record Post(UUID operationId,long version,String reason,String recognitionPolicy){public Post(UUID operationId,long version,String reason){this(operationId,version,reason,"HANDOVER");}}
 public record Line(UUID id,UUID productId,String quantityMilli,String unitPriceXaf,String lineTotalXaf){}
 public record Sale(UUID id,String status,LocalDate businessDate,String totalXaf,long version,UUID warehouseId,UUID customerId,List<Line> lines,String recognitionPolicy){}
 record ValuationLine(UUID productId,long quantityMilli,long revenueXaf,long costXaf){}
 record Valuation(UUID id,UUID customerId,UUID warehouseId,LocalDate businessDate,List<ValuationLine> lines){}
 record PostedSalesSummary(LocalDate from, LocalDate to, String totalXaf) {}
 PostedSalesSummary postedSalesSummary(LocalDate from, LocalDate to);
 Valuation valuation(UUID id);
 List<Sale> list(); Sale get(UUID id); @com.quincaisoft.shared.NewBusinessCommand Sale draft(Draft request); @com.quincaisoft.shared.NewBusinessCommand Sale post(UUID id,Post request);
 public static class RevisionMismatch extends com.quincaisoft.shared.RevisionConflict {public RevisionMismatch(){super("Sale revision changed");}}
}
