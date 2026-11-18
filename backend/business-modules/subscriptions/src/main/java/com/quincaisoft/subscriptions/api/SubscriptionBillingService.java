package com.quincaisoft.subscriptions.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Platform fee documents. Issuance and credit notes do not assert payment or refund settlement. */
public interface SubscriptionBillingService {
    record Issue(UUID operationId, UUID tenantId, UUID originalId,
                 String issuerName, String issuerTaxReference, String customerName,
                 String customerTaxReference, String description, String jurisdiction,
                 LocalDate periodStart, LocalDate periodEnd, LocalDate dueDate,
                 String subtotalXaf, String discountXaf, String taxXaf) {}
    record Invoice(UUID id, UUID tenantId, String kind, UUID originalId, String number,
                   String issuerName, String issuerTaxReference, String customerName,
                   String customerTaxReference, String description, String jurisdiction,
                   LocalDate periodStart, LocalDate periodEnd, LocalDate dueDate,
                   String subtotalXaf, String discountXaf, String taxXaf, String totalXaf,
                   Instant createdAt) {}
    Invoice issue(UUID requestId, Issue input);
    List<Invoice> platformInvoices(UUID tenantId);
    List<Invoice> merchantInvoices();
    String printInvoice(UUID id,boolean platform,String locale);
    record InvoicePage(List<Invoice> items,String nextCursor,Instant cutoff,String currentCursor) {}
    record CsvPage(String content,String nextCursor,Instant cutoff,String currentCursor) {}
    InvoicePage platformPage(UUID tenantId,String cursor,int limit);
    InvoicePage merchantPage(String cursor,int limit);
    CsvPage platformExport(UUID tenantId,String cursor,int limit);
    CsvPage merchantExport(String cursor,int limit);
}
