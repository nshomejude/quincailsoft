package com.quincaisoft.identity.api;

import java.util.Objects;
import java.util.UUID;

/** Privileged approval for one exact operation, never a replacement for business authorization. */
public interface StepUpService {
    enum Action { EXPORT_CUSTOMER_DATA, APPROVE_REFUND, REOPEN_FINANCIAL_PERIOD, CHANGE_MEMBERSHIP }
    record Binding(Action action, UUID resourceId, UUID operationId, String amountXaf) {
        public Binding {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(resourceId, "resourceId");
            Objects.requireNonNull(operationId, "operationId");
            if (amountXaf == null || !amountXaf.matches("0|[1-9][0-9]{0,18}"))
                throw new IllegalArgumentException("Canonical nonnegative integer XAF required");
            try { Long.parseLong(amountXaf); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("XAF exceeds checked integer range", e); }
        }
    }
    /** Requires fresh external MFA evidence bound to the current principal and exact operation. */
    void require(Binding binding);
}
