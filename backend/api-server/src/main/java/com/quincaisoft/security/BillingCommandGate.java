package com.quincaisoft.security;

import com.quincaisoft.shared.*;
import com.quincaisoft.subscriptions.api.SubscriptionLifecycleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.lang.reflect.RecordComponent;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public final class BillingCommandGate {
    private final ScopedTransactions transactions;
    private final JdbcTemplate jdbc;
    private final SubscriptionLifecycleService subscriptions;
    private final TenantLifecyclePolicy lifecycle;
    public BillingCommandGate(ScopedTransactions transactions,JdbcTemplate jdbc,SubscriptionLifecycleService subscriptions,TenantLifecyclePolicy lifecycle) {
        this.transactions=transactions;this.jdbc=jdbc;this.subscriptions=subscriptions;this.lifecycle=lifecycle;
    }
    public <T>T execute(Object[] arguments,Supplier<T> command) {return execute(arguments,true,true,true,command);}
    public <T>T execute(Object[] arguments,boolean billing,boolean tenant,boolean verifiedCommonReceipt,Supplier<T> command) {
        return transactions.execute(()-> {
            var scope=TenantContext.current();UUID operation=operation(arguments);
            // The domain service still validates the original request hash and permissions.
            // A retained receipt is history, not a new posting, and remains retryable after expiry.
            boolean retained=operation!=null && Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from idempotency_result
                  where tenant_id=? and company_id=? and branch_id=? and actor_id=?
                    and operation_id=? and status='ACCEPTED' and response_json is not null)
                """,Boolean.class,scope.tenantId(),scope.companyId(),scope.branchId(),scope.actorId(),operation));
            // Only explicitly audited methods use the common receipt contract.
            // TenantMutation-only APIs may keep private receipts, so an unrelated global
            // operation must never exempt their lifecycle check. Reads stay available.
            if(tenant&&(!retained||!verifiedCommonReceipt))lifecycle.requireNewMutation();
            if(billing&&(!retained||!verifiedCommonReceipt))subscriptions.requireAllowed(SubscriptionLifecycleService.Operation.NEW_BUSINESS_POSTING);
            return command.get();
        });
    }
    private static UUID operation(Object[] args) {return operation(args,0);}
    private static UUID operation(Object[] args,int depth) {
        for(Object value:args) if(value!=null&&value.getClass().isRecord()) {
            for(RecordComponent part:value.getClass().getRecordComponents()) {
                if(part.getName().equals("operationId")&&part.getType()==UUID.class) {
                    try {return (UUID)part.getAccessor().invoke(value);}
                    catch(ReflectiveOperationException e){throw new IllegalArgumentException("Operation identity unavailable",e);}
                }
                if(depth<2&&part.getType().isRecord())try {UUID nested=operation(new Object[]{part.getAccessor().invoke(value)},depth+1);if(nested!=null)return nested;}catch(ReflectiveOperationException e){throw new IllegalArgumentException("Operation identity unavailable",e);}
            }
        }
        return null;
    }
}
