package com.quincaisoft.integrations.api;
import java.util.UUID;
public interface CollectionProvider {
 record Intent(UUID reference,String externalId,String phone,String amount,String currency){}
 enum Outcome {PENDING,UNKNOWN,FAILED,SANDBOX_VERIFIED,VERIFIED}
 record Result(Outcome outcome,String financialTransactionId){}
 Result initiate(Intent intent);Result query(Intent intent);
}
