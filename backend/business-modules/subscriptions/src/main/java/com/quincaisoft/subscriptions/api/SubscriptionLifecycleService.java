package com.quincaisoft.subscriptions.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SubscriptionLifecycleService {
    enum Operation { READ, EXPORT, RECONCILE, BACKUP, STATUTORY_ACCESS, NEW_BUSINESS_POSTING }
    enum Resource { STAFF_SEATS, BRANCHES, WAREHOUSES }
    record Plan(String code,String name,String monthlyPriceXaf,int seatLimit,int branchLimit,int warehouseLimit,
                boolean commerceAccess,Instant effectiveAt) {}
    record Snapshot(UUID tenantId,String planCode,String status,Instant trialEndsAt,long version,
                    boolean mobileAccess,boolean commerceAccess,int seatLimit,int branchLimit,int warehouseLimit,Instant termEndsAt) {}
    record Notice(UUID operationId,UUID tenantId,long subscriptionVersion,String deliveryReference,
                  Instant deliveredAt,Instant graceEndsAt,String contractReference,String reason) {}
    record NoticeView(UUID id,UUID tenantId,long subscriptionVersion,String status,UUID recordedBy,
                      UUID reviewedBy,Instant deliveredAt,Instant graceEndsAt,Instant reviewedAt,
                      String deliveryReference,String contractReference,String reason) {}
    record Payment(UUID operationId,UUID tenantId,UUID invoiceId,String planCode,String amountXaf,
                   String bankReference,String evidenceSha256,String reason) {}
    record PaymentView(UUID id,UUID tenantId,UUID invoiceId,String planCode,String amountXaf,
                       String bankReference,String evidenceSha256,String reason,String status,
                       UUID recordedBy,UUID reviewedBy,Instant reviewedAt) {}
    Snapshot bootstrapTrial(UUID operationId,UUID tenantId,String planCode,Instant trialEndsAt);
    Snapshot verifyTrial(UUID tenantId);
    List<Plan> plans();
    Plan createPlan(UUID operationId,Plan plan);
    Snapshot current();
    Snapshot expireTrial(UUID operationId,UUID tenantId,long expectedVersion);
    Snapshot cancel(UUID operationId,UUID tenantId,long expectedVersion,String reason);
    NoticeView recordNotice(Notice notice);
    NoticeView approveNotice(UUID operationId,UUID noticeId);
    List<NoticeView> notices(UUID tenantId);
    PaymentView recordPayment(Payment payment);
    PaymentView approvePayment(UUID operationId,UUID paymentId);
    Snapshot activatePayment(UUID operationId,UUID paymentId);
    List<PaymentView> payments(UUID tenantId);
    String printReceipt(UUID paymentId,boolean platform,String locale);
    void requireAllowed(Operation operation);
    void requireAllocation(Resource resource,long existingCount,long requestedCount);
}
