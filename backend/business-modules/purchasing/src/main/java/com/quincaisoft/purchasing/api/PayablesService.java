package com.quincaisoft.purchasing.api;import java.util.*;
/** AP ownership remains within purchasing. Mutations must be coordinated with funding+journal in the caller transaction. */
public interface PayablesService {
 record Balance(UUID id,UUID supplierId,String originalXaf,String paidXaf,String outstandingXaf,long version){}
 record Advance(UUID id,UUID supplierId,String originalXaf,String allocatedXaf,String remainingXaf,long version){}
 List<Balance> balances();List<Advance> advances();@com.quincaisoft.shared.TenantMutation void applyPayment(UUID invoiceId,UUID supplierId,long amount);@com.quincaisoft.shared.TenantMutation void createAdvance(UUID id,UUID supplierId,long amount);@com.quincaisoft.shared.TenantMutation void allocateAdvance(UUID advanceId,UUID invoiceId,long amount);
}