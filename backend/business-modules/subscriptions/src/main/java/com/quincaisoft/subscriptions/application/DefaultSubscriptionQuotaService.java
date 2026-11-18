package com.quincaisoft.subscriptions.application;
import com.quincaisoft.subscriptions.api.SubscriptionQuotaService;
import com.quincaisoft.subscriptions.infrastructure.*;
import com.quincaisoft.shared.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;import java.time.*;import java.time.temporal.ChronoUnit;import java.util.function.Supplier;

@Service public class DefaultSubscriptionQuotaService implements SubscriptionQuotaService {
 private final EntityManager em;private final ScopedTransactions tx;private final JsonMapper json;
 public DefaultSubscriptionQuotaService(EntityManager em,ScopedTransactions tx,JsonMapper json){this.em=em;this.tx=tx;this.json=json;}
 private void authorize(boolean write){
  var auth=SecurityContextHolder.getContext().getAuthentication();String capability=write?"billing:write":"billing:read";
  if(auth==null||!auth.isAuthenticated()||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals(capability)||a.getAuthority().equals("SCOPE_"+capability)))throw new AccessDeniedException("Billing capacity capability required");
  var s=TenantContext.current();Number owners=(Number)em.createNativeQuery("select count(*) from tenant_memberships where tenant_id=:t and company_id=:c and branch_id=:b and actor_id=:a and active and role_code='owner' and authorization_epoch=:e")
    .setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("a",s.actorId()).setParameter("e",TenantContext.authorizationEpoch()).getSingleResult();
  if(owners.longValue()!=1)throw new AccessDeniedException("Current active owner required for tenant capacity");
 }
 private void lock(Resource r){em.createNativeQuery("select 1 from quota_resource_lock(:t,:r)").setParameter("t",TenantContext.current().tenantId()).setParameter("r",r.name()).getSingleResult();}
 private Usage current(Resource r){
  var tenant=TenantContext.current().tenantId();Object limit=em.createNativeQuery("select subscription_quota_limit(:t,:r)").setParameter("t",tenant).setParameter("r",r.name()).getSingleResult();
  var rows=em.createNativeQuery("select used,reserved from subscription_quota_state where tenant_id=:t and resource=:r").setParameter("t",tenant).setParameter("r",r.name()).getResultList();
  long used=0,reserved=0;if(!rows.isEmpty()){Object[] a=(Object[])rows.getFirst();used=((Number)a[0]).longValue();reserved=((Number)a[1]).longValue();}
  if(limit==null)return new Usage(r,Long.toString(used),Long.toString(reserved),null,null,"NO_SAAS_CONTRACT");
  long allowed=((Number)limit).longValue();long remaining=Math.max(0,Math.subtractExact(Math.subtractExact(allowed,used),reserved));
  return new Usage(r,Long.toString(used),Long.toString(reserved),Long.toString(allowed),Long.toString(remaining),"SUBSCRIBED");
 }
 public List<Usage> usage(){return tx.execute(()->{authorize(false);return Arrays.stream(Resource.values()).map(this::current).toList();});}
 public void requireCapacity(Resource resource,long requested){
  if(resource==null||requested<0)throw new IllegalArgumentException("Resource and nonnegative requested allocation required");
  tx.execute(()->{authorize(true);lock(resource);var u=current(resource);if(u.limit()!=null&&requested>Long.parseLong(u.available()))throw new IllegalStateException("Licensed capacity exhausted");return null;});
 }
 public Reservation reserve(Reserve input){
  if(input==null||input.operationId()==null||input.resource()==null||input.subjectId()==null)throw new IllegalArgumentException("Operation, resource and target identity required");
  return once(input.operationId(),"RESERVE",input,()->{
   lock(input.resource());var s=TenantContext.current();if(current(input.resource()).limit()==null)throw new IllegalStateException("Explicit SaaS contract required for capacity reservation");
   var r=new QuotaReservation();r.tenantId=s.tenantId();r.id=UUID.randomUUID();r.companyId=s.companyId();r.branchId=s.branchId();r.actorId=s.actorId();
   r.resource=input.resource().name();r.subjectId=input.subjectId();r.subjectKey=input.resource()==Resource.STAFF_SEATS?r.subjectId.toString():s.companyId()+":"+r.subjectId;
   r.units=1;r.status="RESERVED";r.createdAt=Instant.now().truncatedTo(ChronoUnit.MICROS);em.persist(r);em.flush();em.refresh(r);return view(r);
  });
 }
 public Reservation get(UUID id){return tx.execute(()->{authorize(false);return view(find(id,false));});}
 public Reservation release(UUID operation,UUID id){if(operation==null||id==null)throw new IllegalArgumentException("Operation and reservation required");return once(operation,"RELEASE",id,()->{
  var existing=find(id,false);lock(Resource.valueOf(existing.resource));var r=find(id,true);
  if(!r.status.equals("RESERVED"))throw new IllegalStateException("Only a pending capacity reservation can be released");r.status="RELEASED";em.flush();return view(r);
 });}
 public void bind(UUID id){
  if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Capacity binding requires the owning command's transaction");
  tx.execute(()->{authorize(true);var existing=find(id,false);lock(Resource.valueOf(existing.resource));var r=find(id,true);if(!r.status.equals("RESERVED"))throw new IllegalStateException("Pending capacity reservation required");
   em.createNativeQuery("select set_config('app.quota_reservation_id',:r,true)").setParameter("r",id.toString()).getSingleResult();return null;});
 }
 public Reservation committed(UUID id){return tx.execute(()->{authorize(false);var r=find(id,false);em.refresh(r);if(!r.status.equals("COMMITTED"))throw new IllegalStateException("Owning resource has not consumed the capacity reservation");return view(r);});}
 private QuotaReservation find(UUID id,boolean lock){if(id==null)throw new IllegalArgumentException("Reservation required");var s=TenantContext.current();var r=em.find(QuotaReservation.class,new QuotaReservation.Key(s.tenantId(),id),lock?LockModeType.PESSIMISTIC_WRITE:LockModeType.NONE);
  if(r==null||!r.companyId.equals(s.companyId())||!r.branchId.equals(s.branchId()))throw new NoSuchElementException("Reservation not found");return r;}
 private Reservation view(QuotaReservation r){return new Reservation(r.id,Resource.valueOf(r.resource),r.subjectId,r.companyId,Long.toString(r.units),r.status,r.createdAt);}
 private Reservation once(UUID operation,String kind,Object input,Supplier<Reservation> action){return tx.execute(()->{
  authorize(true);var s=TenantContext.current();String hash=sha(json.writeValueAsString(List.of(kind,s,input)));
  em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k","quota-command:"+s.tenantId()+":"+operation).getSingleResult();
  var existing=em.find(QuotaOperation.class,new QuotaOperation.Key(s.tenantId(),operation));
  if(existing!=null){if(!existing.companyId.equals(s.companyId())||!existing.branchId.equals(s.branchId())||!existing.actorId.equals(s.actorId())||!existing.requestHash.equals(hash))throw new IllegalStateException("Capacity operation reused");return json.readValue(existing.responseText,Reservation.class);}
  var result=action.get();var saved=new QuotaOperation();saved.tenantId=s.tenantId();saved.operationId=operation;saved.companyId=s.companyId();saved.branchId=s.branchId();saved.actorId=s.actorId();saved.requestHash=hash;saved.responseText=json.writeValueAsString(result);saved.createdAt=Instant.now().truncatedTo(ChronoUnit.MICROS);em.persist(saved);em.flush();return result;
 });}
 private static String sha(String value){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
