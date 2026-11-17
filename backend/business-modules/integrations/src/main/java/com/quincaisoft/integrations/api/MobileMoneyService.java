package com.quincaisoft.integrations.api;
import java.util.*;import java.time.*;
public interface MobileMoneyService {
 record Prepare(UUID operationId,UUID saleId,String amountXaf,String phone,LocalDate businessDate,String reason){}
 record Intent(UUID id,UUID saleId,String amount,String currency,String environment,String status,String externalReference,UUID journalId,long version){}
 List<Intent> list();Intent prepare(Prepare request);Intent reconcile(UUID id);void callbackHint(UUID id);
}
