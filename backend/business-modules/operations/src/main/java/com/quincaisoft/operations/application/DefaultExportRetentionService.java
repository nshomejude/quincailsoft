package com.quincaisoft.operations.application;
import com.quincaisoft.operations.api.ExportRetentionService;
import com.quincaisoft.operations.infrastructure.*;
import com.quincaisoft.shared.*;
import com.quincaisoft.audit.api.AuditService;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;import java.time.temporal.ChronoUnit;import java.util.*;import java.util.function.Supplier;
@Service public class DefaultExportRetentionService implements ExportRetentionService {
 private final EntityManager em;private final ScopedTransactions tx;private final TenantLifecyclePolicy lifecycle;private final OperationReceipts receipts;private final AuditService audit;
 public DefaultExportRetentionService(EntityManager em,ScopedTransactions tx,TenantLifecyclePolicy lifecycle,AuditService audit,JsonMapper json){this.em=em;this.tx=tx;this.lifecycle=lifecycle;this.audit=audit;this.receipts=new OperationReceipts(em,json);}
 private void authorize(String capability){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null||!auth.isAuthenticated()||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals(capability)||a.getAuthority().equals("SCOPE_"+capability)))throw new AccessDeniedException("Retention capability required");var s=TenantContext.current();var roles=em.createNativeQuery("select role_code from tenant_memberships where tenant_id=:t and company_id=:c and branch_id=:b and actor_id=:a and active and authorization_epoch=:e").setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("a",s.actorId()).setParameter("e",TenantContext.authorizationEpoch()).getResultList();if(roles.size()!=1||!Set.of("owner","finance").contains(roles.getFirst()))throw new AccessDeniedException("Current owner or finance membership required");}
 private Decision command(UUID op,String kind,Object request,Supplier<Decision> work){return receipts.once(op,kind,request,Decision.class,()->{var result=work.get();audit.append(op,kind,"export_retention",result.id(),"Reviewed operational export expiry");return result;},Decision::id);}
 public Decision request(Request input){if(input==null||input.exportId()==null)throw new IllegalArgumentException("Export required");reason(input.reason());return tx.execute(()->{authorize("operations:retention:write");return command(input.operationId(),"ExportRetentionRequested",input,()->{var job=job(input.exportId());eligible(job);var r=new ExportRetention();r.scope(UUID.randomUUID());r.exportId=job.id;r.digest=job.digest;r.expiresAt=job.expiresAt;r.reason=input.reason();r.status="REQUESTED";r.createdAt=now();em.persist(r);em.flush();return view(r);});});}
 public Decision approve(UUID id,Command input){validate(input);return tx.execute(()->{authorize("operations:retention:approve");return command(input.operationId(),"ExportRetentionApproved",List.of(id,input),()->{var r=find(id,LockModeType.PESSIMISTIC_WRITE);version(r,input);if(!r.status.equals("REQUESTED"))throw new IllegalStateException("Pending request required");if(r.actorId.equals(TenantContext.current().actorId()))throw new AccessDeniedException("Independent reviewer required");r.reviewedBy=TenantContext.current().actorId();r.status="APPROVED";em.flush();return view(r);});});}
 public Decision execute(UUID id,Command input){validate(input);return tx.execute(()->{authorize("operations:retention:write");lifecycle.requireNewMutation();return command(input.operationId(),"ExportPayloadPurged",List.of(id,input),()->{
  // Same shared fence as legal-hold placement's exclusive fence, held until commit.
  em.createNativeQuery("select 1 from pg_advisory_xact_lock_shared(hashtextextended(:k,0))").setParameter("k","tenant-lifecycle:"+TenantContext.current().tenantId()).getSingleResult();
  Number holds=(Number)em.createNativeQuery("select count(*) from platform_legal_hold where tenant_id=:t and status='ACTIVE'").setParameter("t",TenantContext.current().tenantId()).getSingleResult();if(holds.longValue()!=0)throw new IllegalStateException("Active legal hold: payload preserved");
  var r=find(id,LockModeType.PESSIMISTIC_WRITE);version(r,input);if(!r.status.equals("APPROVED")||!r.actorId.equals(TenantContext.current().actorId())||r.reviewedBy==null)throw new AccessDeniedException("Approved request and original payload owner required");
  var job=job(r.exportId);eligible(job);if(!r.digest.equals(job.digest))throw new IllegalStateException("Export changed since request");
  em.createNativeQuery("select set_config('app.retention_request',:r,true)").setParameter("r",r.id.toString()).getSingleResult();job.content="";job.status="EXPIRED";em.flush();r.status="PURGED";r.purgedAt=now();em.flush();return view(r);
 });});}
 public Decision get(UUID id){return tx.execute(()->{authorize("operations:retention:read");return view(find(id,LockModeType.NONE));});}
 private ExportJob job(UUID id){var s=TenantContext.current();var j=em.find(ExportJob.class,new ScopeKey(s.tenantId(),s.companyId(),id),LockModeType.PESSIMISTIC_WRITE);if(j==null||!j.branchId.equals(s.branchId())||!j.actorId.equals(s.actorId()))throw new NoSuchElementException("Export not found");return j;}
 private static void eligible(ExportJob j){if(!j.status.equals("READY")||j.expiresAt.isAfter(Instant.now())||j.content.isEmpty())throw new IllegalStateException("Expired completed export payload required");}
 private ExportRetention find(UUID id,LockModeType lock){var s=TenantContext.current();var r=em.find(ExportRetention.class,new ScopeKey(s.tenantId(),s.companyId(),Objects.requireNonNull(id)),lock);if(r==null||!r.branchId.equals(s.branchId()))throw new NoSuchElementException("Retention request not found");return r;}
 private static void reason(String s){if(s==null||s.isBlank()||s.length()>2000)throw new IllegalArgumentException("Bounded reason required");}
 private static void validate(Command c){if(c==null||c.operationId()==null||c.version()==null)throw new IllegalArgumentException("Operation/version required");reason(c.reason());}
 private static void version(ExportRetention r,Command c){if(!Long.toString(r.version).equals(c.version()))throw new IllegalStateException("Stale retention version");}
 private static Instant now(){return Instant.now().truncatedTo(ChronoUnit.MICROS);}
 private static Decision view(ExportRetention r){return new Decision(r.id,r.exportId,r.digest,r.status,r.actorId,r.reviewedBy,Long.toString(r.version),r.expiresAt,r.reason,r.purgedAt);}
}
