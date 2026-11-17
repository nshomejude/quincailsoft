package com.quincaisoft.security;
import com.quincaisoft.shared.*;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
import java.util.function.Supplier;
/** JDBC context commands participate in the same DataSource connection as JpaTransactionManager. */
@Component
public class PostgresScopedTransactions implements ScopedTransactions {
 private final JdbcTemplate jdbc;private final TransactionTemplate tx;
 public PostgresScopedTransactions(JdbcTemplate jdbc,PlatformTransactionManager manager){this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);}
 public <T>T execute(Supplier<T> operation){var scope=TenantContext.current();return tx.execute(status->{
  jdbc.queryForObject("select set_config('app.tenant_id', ?, true)",String.class,scope.tenantId().toString());
  jdbc.queryForObject("select set_config('app.company_id', ?, true)",String.class,scope.companyId().toString());
  jdbc.queryForObject("select set_config('app.branch_id', ?, true)",String.class,scope.branchId().toString());
  jdbc.queryForObject("select set_config('app.actor_id', ?, true)",String.class,scope.actorId().toString());
  Integer memberships=jdbc.queryForObject("select count(*) from tenant_memberships where tenant_id=? and company_id=? and branch_id=? and actor_id=? and active=true and authorization_epoch=?",Integer.class,scope.tenantId(),scope.companyId(),scope.branchId(),scope.actorId(),TenantContext.authorizationEpoch());
  if(memberships==null||memberships!=1)throw new AccessDeniedException("Active current company/branch membership required");
  return operation.get();
 });}
}
