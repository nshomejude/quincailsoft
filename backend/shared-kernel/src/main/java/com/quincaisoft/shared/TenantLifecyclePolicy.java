package com.quincaisoft.shared;
import java.util.UUID;
/** Tenant operational fencing is independent of SaaS billing and never deletes retained data. */
public interface TenantLifecyclePolicy {
 record State(UUID tenantId,String status,long epoch,UUID lastDecisionId){}
 State current();
 void requireNewMutation();
 State requireSuspended(UUID decisionId,long expectedEpoch);
}
