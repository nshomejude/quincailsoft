package com.quincaisoft.operations.application;

import com.quincaisoft.operations.api.ExportService;
import com.quincaisoft.operations.api.OperationsService.*;
import com.quincaisoft.operations.infrastructure.*;
import com.quincaisoft.audit.api.AuditService;
import com.quincaisoft.shared.*;
import jakarta.persistence.EntityManager;import jakarta.persistence.LockModeType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

@Service
public class DefaultExportService implements ExportService {
 private final EntityManager em;
 private final ScopedTransactions tx;
 private final TenantLifecyclePolicy lifecycle;
 private final OperationalReads reads;
 private final AuditService audit;
 private final OperationReceipts receipts;
 public DefaultExportService(EntityManager em,ScopedTransactions tx,TenantLifecyclePolicy lifecycle,OperationalReads reads,AuditService audit,JsonMapper json){this.em=em;this.tx=tx;this.lifecycle=lifecycle;this.reads=reads;this.audit=audit;this.receipts=new OperationReceipts(em,json);}
 private static void capability(String permission){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null||!auth.isAuthenticated()||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals(permission)||a.getAuthority().equals("SCOPE_"+permission)))throw new AccessDeniedException("Export permission required");}
 private void authorize(){capability("operations:export");var s=TenantContext.current();var roles=em.createNativeQuery("select role_code from tenant_memberships where tenant_id=:t and company_id=:c and branch_id=:b and actor_id=:a and active and authorization_epoch=:e").setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("a",s.actorId()).setParameter("e",TenantContext.authorizationEpoch()).getResultList();if(roles.size()!=1||!Set.of("owner","finance").contains(roles.getFirst()))throw new AccessDeniedException("Current owner or finance membership required");}
 private static void entityAccess(Entity entity){
  // No action-bound step-up verifier exists yet. Never downgrade customer privacy to a broad scope.
  if(entity==Entity.CUSTOMER)throw new AccessDeniedException("Customer export requires action-bound step-up; unavailable");
  capability(entity==Entity.SUPPLIER?"suppliers:read":"catalog:read");
 }
 public List<Export> list(){return tx.execute(()->{authorize();var s=TenantContext.current();return em.createQuery("select e from ExportJob e where e.tenantId=:t and e.companyId=:c and e.branchId=:b and e.actorId=:a order by e.createdAt desc,e.id",ExportJob.class).setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("a",s.actorId()).setMaxResults(100).getResultList().stream().filter(e->e.authorizationEpoch==TenantContext.authorizationEpoch()).filter(e->{try{entityAccess(Entity.valueOf(e.entity));return true;}catch(AccessDeniedException denied){return false;}}).map(DefaultExportService::view).toList();});}
 public Export get(UUID id){return tx.execute(()->{authorize();var job=find(id,LockModeType.NONE);access(job);return view(job);});}
 public Export request(ExportRequest input){if(input==null||input.operationId()==null||input.entity()==null)throw new IllegalArgumentException("Operation and entity required");reason(input.reason());return tx.execute(()->{authorize();entityAccess(input.entity());lifecycle.requireNewMutation();return once(input.operationId(),"ExportRequested",input,input.reason(),()->{
  em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k","export-admission:"+TenantContext.current().tenantId()).getSingleResult();
  var job=new ExportJob();job.scope(UUID.randomUUID());job.entity=input.entity().name();job.status="QUEUED";job.rowCount=0;job.digest="";job.content="";job.failure="";job.createdAt=now();job.expiresAt=job.createdAt.plus(1,ChronoUnit.HOURS);job.authorizationEpoch=TenantContext.authorizationEpoch();em.persist(job);em.flush();return view(job);
 });});}
 public Export run(UUID id,Command input){validate(input);return tx.execute(()->{authorize();var job=find(id,LockModeType.PESSIMISTIC_WRITE);access(job);lifecycle.requireNewMutation();return once(input.operationId(),"ExportCompleted",List.of(id,input),input.reason(),()->{
  queued(job,input);if(!job.expiresAt.isAfter(Instant.now())){job.status="FAILED";job.failure="Export expired before execution";}else{
   try{var rendered=reads.csv(Entity.valueOf(job.entity));job.content=rendered.content();job.rowCount=rendered.rows();job.digest=OperationReceipts.digest(job.content);job.status="READY";}
   catch(IllegalArgumentException bounded){job.status="FAILED";job.failure="Export exceeds bounded row or UTF-8 payload limit; use a smaller dataset";}
  }
  em.flush();return view(job);
 });});}
 public Export cancel(UUID id,Command input){validate(input);return tx.execute(()->{authorize();var job=find(id,LockModeType.PESSIMISTIC_WRITE);entityAccess(Entity.valueOf(job.entity));lifecycle.requireNewMutation();return once(input.operationId(),"ExportCancelled",List.of(id,input),input.reason(),()->{queued(job,input);job.status="CANCELLED";em.flush();return view(job);});});}
 public Content download(UUID id){return tx.execute(()->{authorize();var job=find(id,LockModeType.NONE);access(job);if(!"READY".equals(job.status)||!job.expiresAt.isAfter(Instant.now()))throw new IllegalStateException("Ready unexpired export required");if(!OperationReceipts.digest(job.content).equals(job.digest))throw new IllegalStateException("Export integrity mismatch");return new Content(job.id,job.entity.toLowerCase(Locale.ROOT)+"-"+job.id+".csv","text/csv; charset=UTF-8",job.digest,job.content);});}
 private Export once(UUID operation,String kind,Object request,String reason,Supplier<Export> action){return receipts.once(operation,kind,request,Export.class,()->{var result=action.get();audit.append(operation,kind,"export_job",result.id(),reason);return result;},Export::id);}
 private ExportJob find(UUID id,LockModeType lock){var s=TenantContext.current();var job=em.find(ExportJob.class,new ScopeKey(s.tenantId(),s.companyId(),Objects.requireNonNull(id)),lock);if(job==null||!job.branchId.equals(s.branchId())||!job.actorId.equals(s.actorId()))throw new NoSuchElementException("Export not found");return job;}
 private static void access(ExportJob job){entityAccess(Entity.valueOf(job.entity));if(job.authorizationEpoch!=TenantContext.authorizationEpoch())throw new AccessDeniedException("Export authorization changed; request a new export");}
 private static void queued(ExportJob job,Command command){if(!"QUEUED".equals(job.status)||!Long.toString(job.version).equals(command.version()))throw new IllegalStateException("Queued export and current version required");}
 private static void reason(String value){if(value==null||value.isBlank()||value.length()>2000)throw new IllegalArgumentException("Bounded reason required");}
 private static void validate(Command command){if(command==null||command.operationId()==null||command.version()==null)throw new IllegalArgumentException("Operation/version required");reason(command.reason());}
 private static Instant now(){return Instant.now().truncatedTo(ChronoUnit.MICROS);}
 private static Export view(ExportJob job){return new Export(job.id,Entity.valueOf(job.entity),job.status,job.rowCount,job.digest,job.createdAt,job.expiresAt,Long.toString(job.version),job.failure);}
}
