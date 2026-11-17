package com.quincaisoft.purchasing.api;
import java.util.*;import java.time.*;
public interface PurchasingService {
 record RequestedLine(UUID productId,String quantityMilli,String unitPriceXaf){}
 record Draft(UUID operationId,UUID supplierId,UUID warehouseId,LocalDate businessDate,List<RequestedLine> lines){}
 record Decision(UUID operationId,long version,String reason){}
 record ReceiveLine(UUID lineId,String quantityMilli){}
 record Receive(UUID operationId,long version,String reason,LocalDate businessDate,List<ReceiveLine> lines){}
 record InvoiceRequest(UUID operationId,String supplierReference,LocalDate businessDate,String reason){}
 record Line(UUID id,UUID productId,String quantityMilli,String receivedMilli,String unitPriceXaf,String totalXaf){}
 record Purchase(UUID id,UUID supplierId,UUID warehouseId,LocalDate businessDate,String status,long version,String totalXaf,List<Line> lines){}
 record ReceiptLine(UUID id,UUID purchaseLineId,UUID productId,String quantityMilli,String valueXaf){}
 record Receipt(UUID id,UUID purchaseId,LocalDate businessDate,String totalXaf,List<ReceiptLine> lines){}
 record Invoice(UUID id,UUID receiptId,UUID supplierId,String supplierReference,LocalDate businessDate,String totalXaf){}
 record ReturnLineRequest(UUID receiptLineId,String quantityMilli){}
 record ReturnRequest(UUID operationId,java.time.LocalDate businessDate,String reason,List<ReturnLineRequest> lines){}
 record SupplierReturn(UUID id,UUID receiptId,java.time.LocalDate businessDate,String totalXaf){}
 SupplierReturn returnToSupplier(UUID receiptId,ReturnRequest request);List<SupplierReturn> supplierReturns(UUID receiptId);
 List<Purchase> list();Purchase get(UUID id);Purchase draft(Draft request);Purchase submit(UUID id,Decision request);Purchase approve(UUID id,Decision request);
 Receipt receive(UUID id,Receive request);List<Receipt> receipts(UUID id);Invoice invoice(UUID receiptId,InvoiceRequest request);List<Invoice> invoices();
}