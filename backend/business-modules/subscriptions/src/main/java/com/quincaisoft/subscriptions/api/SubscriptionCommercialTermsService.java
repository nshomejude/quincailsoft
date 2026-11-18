package com.quincaisoft.subscriptions.api;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
public interface SubscriptionCommercialTermsService {
 record Request(UUID operationId,UUID tenantId,UUID invoiceId,String planCode,String basis,String evidenceReference,String evidenceSha256,String reason){}
 record TermView(UUID id,UUID tenantId,UUID invoiceId,String planCode,String basis,String status,
                 String monthlyPriceXaf,String subtotalXaf,String discountXaf,String taxXaf,String totalXaf,
                 LocalDate periodStart,LocalDate periodEnd,String evidenceReference,String evidenceSha256,String reason,
                 UUID recordedBy,UUID reviewedBy,Instant reviewedAt){}
 TermView request(Request request);
 TermView approve(UUID operationId,UUID termId);
 List<TermView> list(UUID tenantId);
}
