package com.quincaisoft.finance.api;import java.time.*;import java.util.*;
public interface PeriodService {
 record Period(UUID id,LocalDate startDate,LocalDate endDate,String status,long version){}
 record Create(UUID operationId,LocalDate startDate,LocalDate endDate,String reason){}
 record Command(UUID operationId,long version,String reason){}
 record Request(UUID id,UUID periodId,String action,String status,UUID requestedBy,UUID decidedBy,long version,String reason,String decisionReason){}
 List<Period> list();List<Request> requests();@com.quincaisoft.shared.TenantMutation Period create(Create p);@com.quincaisoft.shared.TenantMutation Request request(UUID periodId,String action,Command p);@com.quincaisoft.shared.TenantMutation Request decide(UUID requestId,boolean approve,Command p);
}
