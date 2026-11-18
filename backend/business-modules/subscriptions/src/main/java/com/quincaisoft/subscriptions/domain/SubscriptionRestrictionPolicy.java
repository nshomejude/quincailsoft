package com.quincaisoft.subscriptions.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Billing-only policy under architecture 5.4 and 8.10; never grants access.
 * Callers must independently enforce current identity, scope, permissions and
 * non-billing security holds. Inputs must come from trusted persisted records,
 * with current subscription version checked in the same command transaction.
 * This component neither verifies provider payments nor changes subscription state.
 */
public final class SubscriptionRestrictionPolicy {
    private SubscriptionRestrictionPolicy() {}

    public enum Status { TRIAL, ACTIVE, GRACE, PAST_DUE, SUSPENDED, CANCELLED, EXPIRED }
    public enum Operation { READ, EXPORT, RECONCILE, BACKUP, STATUTORY_ACCESS, NEW_BUSINESS_POSTING }
    public enum Decision {
        NOT_RESTRICTED_BY_BILLING,
        RESTRICTION_REQUIRES_REVIEW,
        RESTRICT_NEW_POSTING
    }

    public record Subscription(UUID tenantId, UUID subscriptionId, long version, Status status) {
        public Subscription {
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(subscriptionId, "subscriptionId");
            Objects.requireNonNull(status, "status");
            if (version < 0) throw new IllegalArgumentException("Negative subscription version");
        }
    }

    /** Evidence references identify audited notice delivery and approved contract review. */
    public record RestrictionEvidence(
            UUID tenantId, UUID subscriptionId, long subscriptionVersion,
            UUID noticeId, Instant noticeDeliveredAt, Instant graceEndsAt,
            UUID contractReviewId, Instant contractReviewedAt) {
        public RestrictionEvidence {
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(subscriptionId, "subscriptionId");
            Objects.requireNonNull(noticeId, "noticeId");
            Objects.requireNonNull(noticeDeliveredAt, "noticeDeliveredAt");
            Objects.requireNonNull(graceEndsAt, "graceEndsAt");
            Objects.requireNonNull(contractReviewId, "contractReviewId");
            Objects.requireNonNull(contractReviewedAt, "contractReviewedAt");
            if (subscriptionVersion < 0) throw new IllegalArgumentException("Negative evidence version");
            if (!graceEndsAt.isAfter(noticeDeliveredAt))
                throw new IllegalArgumentException("Communicated grace period must have positive duration");
        }
    }

    /**
     * No hidden clock or configured grace duration: evaluate persisted, approved terms
     * at an explicit instant. Expiry alone cannot fabricate a notice or contract review.
     * A review result is not permission to restrict access or post an economic command.
     */
    public static Decision evaluate(Subscription subscription, Operation operation,
                                    RestrictionEvidence evidence, Instant now) {
        Objects.requireNonNull(subscription, "subscription");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(now, "now");
        if (operation != Operation.NEW_BUSINESS_POSTING)
            return Decision.NOT_RESTRICTED_BY_BILLING;
        if (subscription.status() == Status.TRIAL || subscription.status() == Status.ACTIVE
                || subscription.status() == Status.GRACE)
            return Decision.NOT_RESTRICTED_BY_BILLING;
        if (evidence == null
                || !subscription.tenantId().equals(evidence.tenantId())
                || !subscription.subscriptionId().equals(evidence.subscriptionId())
                || subscription.version() != evidence.subscriptionVersion()
                || now.isBefore(evidence.graceEndsAt())
                || now.isBefore(evidence.contractReviewedAt()))
            return Decision.RESTRICTION_REQUIRES_REVIEW;
        return Decision.RESTRICT_NEW_POSTING;
    }
}
