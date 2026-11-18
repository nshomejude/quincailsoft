package com.quincaisoft.subscriptions.api;
import java.util.*;import java.time.*;
/** Tenant-wide licensed counts; seats mean distinct active login identities, never payroll records. */
public interface SubscriptionQuotaService {
 enum Resource { STAFF_SEATS, BRANCHES, WAREHOUSES }
 record Usage(Resource resource,String used,String reserved,String limit,String available,String licenseMode){}
 record Reserve(UUID operationId,Resource resource,UUID subjectId){}
 record Reservation(UUID id,Resource resource,UUID subjectId,UUID companyId,String units,String status,Instant createdAt){}
 List<Usage> usage();
 @com.quincaisoft.shared.TenantMutation Reservation reserve(Reserve input);
 Reservation get(UUID id);
 @com.quincaisoft.shared.TenantMutation Reservation release(UUID operationId,UUID reservationId);
 /** Internal owning-command hook. Binding lasts only in the caller's transaction. */
 @com.quincaisoft.shared.TenantMutation void bind(UUID reservationId);
 /** Does not allocate usage; verifies the authoritative owning-table trigger consumed the hold. */
 Reservation committed(UUID reservationId);
 void requireCapacity(Resource resource,long requestedCount);
}
