package com.quincaisoft.documents.api;
import java.time.*;import java.util.*;
public interface DocumentService {
 record Item(UUID productId,String sku,String description,String baseUnit,String quantityMilli,String unitPriceXaf,String totalXaf){}
 record Issue(UUID sourceId,UUID operationId,String kind,LocalDate businessDate,UUID customerId,String totalXaf,List<Item> items,UUID originalSourceId){public Issue(UUID sourceId,UUID operationId,String kind,LocalDate businessDate,UUID customerId,String totalXaf,List<Item> items){this(sourceId,operationId,kind,businessDate,customerId,totalXaf,items,null);}}
 record Settings(String issuerName,String issuerAddress,String registrationNumber,String fiscalSeries,String locale,long version){}
 record Snapshot(UUID id,String number,String kind,UUID sourceId,LocalDate businessDate,String totalXaf,String sha256,String content,Instant issuedAt){}
 Settings settings(); Settings configure(Settings settings); Settings configure(UUID operationId,Settings settings);
 Snapshot issue(Issue request); Snapshot get(UUID id); List<Snapshot> list(UUID sourceId); String render(UUID id,boolean copy);
}
