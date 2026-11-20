package com.quincaisoft.platformcontrol.application;

import com.quincaisoft.platformcontrol.api.*;
import com.quincaisoft.platformcontrol.infrastructure.*;
import com.quincaisoft.shared.BootstrapScope;
import com.quincaisoft.company.api.CompanyBootstrapService;
import com.quincaisoft.branch.api.BranchBootstrapService;
import com.quincaisoft.inventory.api.InventoryBootstrapService;
import com.quincaisoft.identity.api.IdentityBootstrapService;
import com.quincaisoft.documents.api.DocumentsBootstrapService;
import com.quincaisoft.finance.api.FinanceBootstrapService;
import com.quincaisoft.subscriptions.api.SubscriptionLifecycleService;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;import java.time.*;import java.time.temporal.ChronoUnit;import java.util.function.Supplier;

@Service
public class DefaultTenantProvisioningService implements TenantProvisioningService {
 private final EntityManager em;private final TransactionTemplate tx;
 private final CompanyBootstrapService company;private final BranchBootstrapService branch;private final InventoryBootstrapService inventory;
 private final IdentityBootstrapService identity;private final DocumentsBootstrapService documents;private final FinanceBootstrapService finance;
 private final SubscriptionLifecycleService subscriptions;private final JsonMapper json=JsonMapper.builder().build();
 public DefaultTenantProvisioningService(EntityManager em,PlatformTransactionManager manager,CompanyBootstrapService company,
   BranchBootstrapService branch,InventoryBootstrapService inventory,IdentityBootstrapService identity,DocumentsBootstrapService documents,
   FinanceBootstrapService finance,SubscriptionLifecycleService subscriptions){
  this.em=em;this.tx=new TransactionTemplate(manager);this.company=company;this.branch=branch;this.inventory=inventory;this.identity=identity;this.documents=documents;this.finance=finance;this.subscriptions=subscriptions;
 }
 private UUID actor(String capability){var auth=SecurityContextHolder.getContext().getAuthentication();
  if(!(auth instanceof JwtAuthenticationToken jwt)||!jwt.isAuthenticated()||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("SCOPE_"+capability)))throw new AccessDeniedException("Platform capability required");
  try{return UUID.fromString(jwt.getToken().getSubject());}catch(RuntimeException e){throw new AccessDeniedException("Platform identity required");}
 }
 private <T>T scoped(String capability,Supplier<T> action){UUID actor=actor(capability);return tx.execute(status->{
  set("app.platform_actor",actor.toString());Number count=(Number)em.createNativeQuery("select count(*) from platform_operator where actor_id=:a and active").setParameter("a",actor).getSingleResult();
  if(count.longValue()!=1)throw new AccessDeniedException("Active enrolled platform operator required");return action.get();});}
 public Result get(UUID requestId){return scoped("platform:tenants:read",()->view(requireRequest(requestId,LockModeType.NONE)));}
 public Result provisionTrial(UUID requestId,UUID correlationId,Trial input){
  if(requestId==null||correlationId==null||input==null||input.operationId()==null||input.configurationVersion()<0||input.businessDate()==null)throw new IllegalArgumentException("Operation, correlation, version and business date required");
  text(input.branchName());text(input.warehouseName());UUID actor=actor("platform:tenants:provision");String hash=sha(json.writeValueAsString(List.of(requestId,actor,input)));
  UUID planId=scoped("platform:tenants:provision",()->{
   em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k","tenant-bootstrap:"+input.operationId()).getSingleResult();
   var configuration=em.find(OnboardingConfiguration.class,requestId,LockModeType.PESSIMISTIC_WRITE);
   if(configuration==null)throw new NoSuchElementException("Configuration not found");
   var prior=em.createQuery("select p from ProvisioningPlan p where p.requestId=:r or p.operationId=:o",ProvisioningPlan.class).setParameter("r",requestId).setParameter("o",input.operationId()).getResultList();
   if(!prior.isEmpty()){var p=prior.getFirst();if(!p.requestId.equals(requestId)||!p.operationId.equals(input.operationId())||!p.actorId.equals(actor)||!p.requestHash.equals(hash))throw new IllegalStateException("Provisioning operation reused");return p.id;}
   if(!"APPROVED_CONFIGURATION".equals(configuration.status)||configuration.version!=input.configurationVersion())throw new IllegalStateException("Current independently approved configuration required");
   if(!"standard-trial".equals(configuration.planCode)||!"EMPTY".equals(configuration.openingMode))throw new IllegalStateException("This command provisions an empty standard trial only; paid and opening-import workflows require their own acceptance");
   if(configuration.identityVerifiedAt==null||configuration.approvedBy==null)throw new IllegalStateException("Verified owner identity and separate approval required");
   var p=new ProvisioningPlan();p.id=UUID.randomUUID();p.requestId=requestId;p.operationId=input.operationId();p.correlationId=correlationId;p.actorId=actor;p.requestHash=hash;
   p.tenantId=UUID.randomUUID();p.companyId=UUID.randomUUID();p.branchId=UUID.randomUUID();p.warehouseId=UUID.randomUUID();p.periodId=UUID.randomUUID();p.ownerSubject=configuration.ownerSubject;
   p.branchName=input.branchName();p.warehouseName=input.warehouseName();p.businessDate=input.businessDate();p.createdAt=now();p.trialEndsAt=p.createdAt.plus(14,ChronoUnit.DAYS);
   p.storagePrefix="tenants/"+p.tenantId+"/";p.status="PENDING";em.persist(p);em.flush();event(p,"TrialProvisioningRequested.v1",correlationId);em.flush();return p.id;
  });
  try{return scoped("platform:tenants:provision",()->execute(planId,correlationId));}
  catch(RuntimeException failure){
   // Business resources are rolled back together. The independently committed intent survives for exact retry.
   try{scoped("platform:tenants:provision",()->{var p=em.find(ProvisioningPlan.class,planId,LockModeType.PESSIMISTIC_WRITE);if(p!=null&&!"ACTIVE".equals(p.status)){p.status="FAILED";p.failureCode="BOOTSTRAP_TRANSACTION_FAILED";em.flush();event(p,"TrialProvisioningFailed.v1",correlationId);}return null;});}
   catch(RuntimeException recordingFailure){failure.addSuppressed(recordingFailure);}throw failure;
  }
 }
 private Result execute(UUID planId,UUID correlationId){
  var p=em.find(ProvisioningPlan.class,planId,LockModeType.PESSIMISTIC_WRITE);if(p==null)throw new NoSuchElementException("Provisioning intent not found");
  if(!p.actorId.equals(actor("platform:tenants:provision")))throw new AccessDeniedException("Provisioning retry belongs to another actor");
  if("ACTIVE".equals(p.status))return json.readValue(p.responseText,Result.class);
  if(!p.trialEndsAt.isAfter(now()))throw new IllegalStateException("Reserved trial window expired; independent new approval required");
  var c=em.find(OnboardingConfiguration.class,p.requestId);if(c==null||!"APPROVED_CONFIGURATION".equals(c.status))throw new IllegalStateException("Approved configuration required");
  p.status="PROVISIONING";p.failureCode=null;em.flush();set("app.provisioning_id",p.id.toString());set("app.tenant_id",p.tenantId.toString());set("app.company_id",p.companyId.toString());set("app.branch_id",p.branchId.toString());set("app.actor_id",p.ownerSubject.toString());
  var s=new BootstrapScope(p.id,p.tenantId,p.companyId,p.branchId,p.ownerSubject);
  var tenant=new ProvisionedTenant();tenant.id=p.tenantId;tenant.name=c.legalName;tenant.createdAt=p.createdAt;em.persist(tenant);em.flush();step(p,"TENANT");
  company.initialize(s,c.legalName,c.locale);step(p,"COMPANY");
  branch.initialize(s,p.branchName,c.timezone);step(p,"BRANCH");
  inventory.initialize(s,p.warehouseId,p.warehouseName);step(p,"STOCK_LOCATION");
  documents.initialize(s,c.legalName,c.address,c.registrationNumber,c.documentPrefix,c.locale,p.businessDate.getYear());step(p,"DOCUMENTS");
  finance.initialize(s,p.periodId,p.businessDate);step(p,"FINANCE");
  var subscription=subscriptions.bootstrapTrial(p.operationId,p.tenantId,c.planCode,p.trialEndsAt);
  if(!subscription.mobileAccess()||!p.trialEndsAt.equals(subscription.trialEndsAt()))throw new IllegalStateException("Trial entitlement verification failed");step(p,"TRIAL_ENTITLEMENTS");
  identity.initialize(s);step(p,"OWNER_MEMBERSHIP");
  verify(p,c);step(p,"VALIDATED");
  p.status="ACTIVE";p.activatedAt=now();em.flush();event(p,"TenantTrialActivated.v1",correlationId);
  Result result=view(p);p.responseText=json.writeValueAsString(result);em.flush();return result;
 }
 private void verify(ProvisioningPlan p,OnboardingConfiguration c){
  requireCount("select count(*) from company where tenant_id=:t and id=:c and currency='XAF' and branch_count=1",p,1);
  requireCount("select count(*) from branch where tenant_id=:t and company_id=:c",p,1);
  requireCount("select count(*) from warehouse where tenant_id=:t and company_id=:c and active",p,1);
  requireCount("select count(*) from tenant_memberships where tenant_id=:t and company_id=:c and active and role_code='owner' and authorization_epoch=1",p,1);
  requireCount("select count(*) from document_settings where tenant_id=:t and company_id=:c",p,1);
  requireCount("select count(*) from document_series where tenant_id=:t and company_id=:c and next_number=1",p,3);
  requireCount("select count(*) from financial_period where tenant_id=:t and company_id=:c and status='Open'",p,1);
  requireCount("select count(*) from finance_bootstrap_mapping where tenant_id=:t and company_id=:c and review_status='PROVISIONAL'",p,FinanceBootstrapService.OPERATIONAL_ACCOUNTS.size());
  requireCount("select count(*) from company_business_defaults where tenant_id=:t and company_id=:c and currency='XAF'",p,1);
 }
 private void requireCount(String sql,ProvisioningPlan p,long expected){Number count=(Number)em.createNativeQuery(sql).setParameter("t",p.tenantId).setParameter("c",p.companyId).getSingleResult();if(count.longValue()!=expected)throw new IllegalStateException("Provisioning invariant failed");}
 private ProvisioningPlan requireRequest(UUID requestId,LockModeType lock){var rows=em.createQuery("select p from ProvisioningPlan p where p.requestId=:r",ProvisioningPlan.class).setParameter("r",requestId).setLockMode(lock).getResultList();if(rows.isEmpty())throw new NoSuchElementException("Provisioning intent not found");return rows.getFirst();}
 private Result view(ProvisioningPlan p){var steps=em.createQuery("select s.stepCode from ProvisioningStep s where s.provisioningId=:p order by s.stepCode",String.class).setParameter("p",p.id).getResultList();return new Result(p.requestId,p.tenantId,p.companyId,p.branchId,p.warehouseId,p.ownerSubject,p.status,Long.toString(p.version),p.trialEndsAt,steps,"1");}
 private void step(ProvisioningPlan p,String code){var s=new ProvisioningStep();s.provisioningId=p.id;s.stepCode=code;s.completedAt=now();em.persist(s);em.flush();}
 private void event(ProvisioningPlan p,String type,UUID correlation){var e=new ProvisioningEvent();e.id=UUID.randomUUID();e.provisioningId=p.id;e.actorId=actor("platform:tenants:provision");e.correlationId=correlation;e.eventType=type;e.createdAt=now();em.persist(e);}
 private void set(String key,String value){em.createNativeQuery("select set_config(:k,:v,true)").setParameter("k",key).setParameter("v",value).getSingleResult();}
 private static void text(String value){if(value==null||value.isBlank()||value.length()>120||!value.equals(value.trim())||value.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Nonblank bounded branch/location names required");}
 private static Instant now(){return Instant.now().truncatedTo(ChronoUnit.MICROS);}
 private static String sha(String value){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
