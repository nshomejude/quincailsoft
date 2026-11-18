package com.quincaisoft.subscriptions.api;
import java.time.Instant;
import java.util.*;
public interface BillingNotificationDispatcher {
 record Delivery(UUID eventId,UUID tenantId,String status,int attempts,int attemptBudget,String lastOutcome,Integer lastHttpStatus,Instant nextAttemptAt,Instant completedAt){}
 record Attempt(UUID id,UUID eventId,int attemptNumber,String stage,String outcome,Integer httpStatus,Instant recordedAt){}
 /** Worker-only execution. HTTP routes must not expose this machine-authorized method. */
 boolean dispatchOne();
 List<Delivery> list(String status);
 List<Attempt> attempts(UUID eventId);
 Delivery redrive(UUID operationId,UUID eventId,String reason);
}
