package com.quincaisoft.platformcontrol.application;
import com.quincaisoft.shared.*;import com.quincaisoft.platformcontrol.infrastructure.TenantLifecycle;
import jakarta.persistence.*;import org.springframework.stereotype.Service;import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;import java.util.UUID;
@Service public class DefaultTenantLifecyclePolicy implements TenantLifecyclePolicy{
 private final EntityManager em;private final ScopedTransactions tx;
 public DefaultTenantLifecyclePolicy(EntityManager em,ScopedTransactions tx){this.em=em;this.tx=tx;}
 private State state(){var tenant=TenantContext.current().tenantId();var s=em.find(TenantLifecycle.class,tenant);if(s!=null)em.refresh(s);return s==null?new State(tenant,"ACTIVE",0,null):new State(tenant,s.status,s.epoch,s.lastDecisionId);}
 private void fence(){em.createNativeQuery("select 1 from pg_advisory_xact_lock_shared(hashtextextended(:k,0))").setParameter("k","tenant-lifecycle:"+TenantContext.current().tenantId()).getSingleResult();}
 public State current(){return tx.execute(this::state);}
 public void requireNewMutation(){tx.execute(()->{fence();var state=state();if(!"ACTIVE".equals(state.status()))throw new AccessDeniedException("Tenant is operationally suspended; retained reads and recovery remain available");
  if(state.epoch()>0){var auth=SecurityContextHolder.getContext().getAuthentication();if(!(auth instanceof JwtAuthenticationToken jwt))throw new AccessDeniedException("Fresh tenant scope required");long tokenEpoch;try{Object claim=jwt.getToken().getClaim("tenant_epoch");tokenEpoch=Long.parseLong(String.valueOf(claim));}catch(RuntimeException e){throw new AccessDeniedException("Refresh tenant scope after lifecycle change");}if(tokenEpoch!=state.epoch())throw new AccessDeniedException("Stale tenant lifecycle scope");}return null;});}
 public State requireSuspended(UUID decisionId,long expectedEpoch){return tx.execute(()->{fence();var state=state();if(decisionId==null||!"SUSPENDED".equals(state.status())||state.epoch()!=expectedEpoch||!decisionId.equals(state.lastDecisionId()))throw new IllegalStateException("Current independently approved tenant suspension receipt required");return state;});}
}
