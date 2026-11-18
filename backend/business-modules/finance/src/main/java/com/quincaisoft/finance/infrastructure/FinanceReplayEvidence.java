package com.quincaisoft.finance.infrastructure;

import com.quincaisoft.shared.TenantContext;
import jakarta.persistence.EntityManager;
import java.util.UUID;

/** Binds historical durable responses to the immutable actor evidence emitted with them. */
public final class FinanceReplayEvidence {
    private FinanceReplayEvidence() {}

    public static void requireActor(EntityManager em, UUID operation, String eventType, boolean period) {
        var scope = TenantContext.current();
        // Older period requests identify their maker/reviewer in the result itself. Older
        // period-create events contain no actor evidence and deliberately fail closed.
        String actor = period
                ? "coalesce(e.payload->>'actorId',e.payload->>'decidedBy',e.payload->>'requestedBy')"
                : "e.payload->>'actorId'";
        var actors = em.createNativeQuery("select " + actor + " from outbox_event e join idempotency_result r"
                        + " on r.tenant_id=e.tenant_id and r.operation_id=e.operation_id"
                        + " and r.company_id=e.company_id and r.branch_id=e.branch_id and r.source_id=e.aggregate_id"
                        + " where e.tenant_id=:t and e.company_id=:c and e.branch_id=:b"
                        + " and e.operation_id=:o and e.event_type=:type")
                .setParameter("t", scope.tenantId()).setParameter("c", scope.companyId())
                .setParameter("b", scope.branchId()).setParameter("o", operation)
                .setParameter("type", eventType).getResultList();
        if (actors.size() != 1 || !scope.actorId().toString().equals(actors.getFirst())) {
            throw new IllegalStateException("Committed finance operation belongs to another actor or lacks verifiable actor evidence");
        }
    }
}
