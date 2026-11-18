package com.quincaisoft.platformcontrol.api;
import com.quincaisoft.shared.TenantLifecyclePolicy;import java.util.*;import java.time.*;
public interface PlatformGovernanceService {
 record Consent(UUID operationId,UUID operatorId,String ticketReference,String reason,Instant expiresAt){}
 record Command(UUID operationId,long expectedVersion,String reason){}
 record Grant(UUID id,UUID tenantId,UUID companyId,UUID branchId,UUID ownerActorId,UUID operatorId,UUID approvedBy,String status,Instant expiresAt,String version,String ticketReference,String reason){}
 record Diagnostics(UUID grantId,UUID tenantId,UUID companyId,UUID branchId,Instant generatedAt,String productCount,String postedSaleCount,String stockPositionCount){}
 enum Kind{SUSPEND,RESUME,PLACE_LEGAL_HOLD,RELEASE_LEGAL_HOLD}
 record Request(UUID operationId,Kind kind,long expectedVersion,UUID holdId,String reference,String reason){}
 record Review(UUID id,UUID tenantId,Kind kind,String status,UUID requestedBy,UUID approvedBy,long expectedTargetVersion,String version,String reference,String reason,UUID holdId){}
 record Hold(UUID id,UUID tenantId,String reference,String reason,String status,UUID placedBy,UUID releasedBy,String version){}
 List<Grant> merchantGrants();Grant consent(Consent input);Grant revoke(UUID id,Command input);
 List<Grant> platformGrants();Grant approveGrant(UUID id,Command input);Diagnostics diagnostics(UUID id,UUID correlationId);
 TenantLifecyclePolicy.State tenantState(UUID tenantId);List<Hold> holds(UUID tenantId);List<Review> requests(UUID tenantId);
 Review request(UUID tenantId,Request input);Review approve(UUID requestId,Command input);
}
