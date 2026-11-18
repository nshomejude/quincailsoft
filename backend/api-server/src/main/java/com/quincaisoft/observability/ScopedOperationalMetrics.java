package com.quincaisoft.observability;

import com.quincaisoft.shared.ScopedTransactions;
import com.quincaisoft.shared.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Private, explicitly enabled, RLS-scoped diagnostics. Never enumerates tenants. */
@RestController
@ConditionalOnProperty(name="quincai.observability.enabled", havingValue="true")
public class ScopedOperationalMetrics {
 private final JdbcTemplate jdbc;
 private final ScopedTransactions transactions;
 private final int limit;
 public ScopedOperationalMetrics(JdbcTemplate jdbc, ScopedTransactions transactions,
   @Value("${quincai.observability.sample-limit:1000}") int limit) {
  if(limit<1 || limit>10000) throw new IllegalArgumentException("Metrics sample limit must be 1..10000");
  this.jdbc=jdbc;this.transactions=transactions;this.limit=limit;
 }
 @GetMapping(value="/api/v1/observability/metrics", produces="text/plain;version=0.0.4;charset=utf-8")
 public ResponseEntity<String> metrics() {
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth==null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(a->
    a.getAuthority().equals("observability:read") || a.getAuthority().equals("SCOPE_observability:read")))
   throw new AccessDeniedException("Operational metrics permission required");
  return transactions.execute(()-> {
   var scope=TenantContext.current();
   Integer allowed=jdbc.queryForObject("select count(*) from tenant_memberships where tenant_id=? and company_id=? and branch_id=? and actor_id=? and active and authorization_epoch=? and role_code in ('owner','finance')", Integer.class,
     scope.tenantId(),scope.companyId(),scope.branchId(),scope.actorId(),TenantContext.authorizationEpoch());
   if(allowed==null || allowed!=1) throw new AccessDeniedException("Current owner or finance membership required");
   jdbc.queryForObject("select set_config('statement_timeout','2000',true)",String.class);
   jdbc.queryForObject("select set_config('lock_timeout','500',true)",String.class);
   var row=jdbc.queryForMap("""
    with pending as materialized (
      select e.created_at from outbox_event e
      where e.tenant_id=app_tenant() and e.company_id=app_company() and e.branch_id=app_branch()
      and (not exists(select from outbox_delivery d where d.tenant_id=e.tenant_id and d.event_id=e.id)
       or exists(select from outbox_delivery d where d.tenant_id=e.tenant_id and d.event_id=e.id and d.completed_at is null))
      order by e.created_at,e.id limit ?
    ), unknowns as materialized (
      select environment from provider_intent where tenant_id=app_tenant() and company_id=app_company()
      and branch_id=app_branch() and status='UNKNOWN' order by id limit ?
    ), journals as materialized (
      select tenant_id,company_id,id from journal_entry where tenant_id=app_tenant() and company_id=app_company()
      and branch_id=app_branch() order by business_date desc,id desc limit ?
    ), invalid as (
      select j.id from journals j left join journal_line l on l.tenant_id=j.tenant_id and l.company_id=j.company_id and l.entry_id=j.id
      group by j.id having count(l.id)<2 or coalesce(sum(l.debit_xaf-l.credit_xaf),0)<>0
    )
    select (select count(*) from pending) outbox_sample,
      coalesce((select greatest(0,extract(epoch from now()-min(created_at))) from pending),0) outbox_oldest_seconds,
      (select count(*) from unknowns) unknown_sample,
      (select count(*) from unknowns where environment='sandbox') unknown_sandbox,
      (select count(*) from unknowns where environment<>'sandbox') unknown_live,
      (select count(*) from journals) journal_sample,
      (select count(*) from invalid) journal_imbalanced,
      extract(epoch from now()) collected_timestamp_seconds
    """,limit,limit,limit);
   var body=new StringBuilder();
   for(var entry:row.entrySet()) {
    String name="quincai_scoped_"+entry.getKey();
    body.append("# TYPE ").append(name).append(" gauge\n").append(name).append(' ').append(entry.getValue()).append('\n');
    if(entry.getKey().endsWith("_sample")) body.append(name).append("_complete ")
      .append(((Number)entry.getValue()).longValue()<limit?1:0).append('\n');
   }
   body.append("quincai_scoped_sample_limit ").append(limit).append('\n');
   return ResponseEntity.ok().header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").body(body.toString());
  });
 }
}
