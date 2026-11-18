package com.quincaisoft.platformcontrol.api;
import java.util.*;import java.time.*;
/** Atomic empty-company trial provisioning. Does not assert paid-provider acceptance. */
public interface TenantProvisioningService {
 record Trial(UUID operationId,long configurationVersion,String branchName,String warehouseName,LocalDate businessDate){}
 record Result(UUID requestId,UUID tenantId,UUID companyId,UUID branchId,UUID warehouseId,UUID ownerSubject,
               String status,String version,Instant trialEndsAt,List<String> completedSteps,String authorizationEpoch){}
 Result provisionTrial(UUID requestId,UUID correlationId,Trial input);
 Result get(UUID requestId);
}
