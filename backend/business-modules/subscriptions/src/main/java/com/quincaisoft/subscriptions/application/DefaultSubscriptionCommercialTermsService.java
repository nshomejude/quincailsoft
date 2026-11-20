package com.quincaisoft.subscriptions.application;
import com.quincaisoft.subscriptions.api.SubscriptionCommercialTermsService;
import com.quincaisoft.subscriptions.infrastructure.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class DefaultSubscriptionCommercialTermsService implements SubscriptionCommercialTermsService {
 private final EntityManager em;private final TransactionTemplate tx;private final JsonMapper json;
 public DefaultSubscriptionCommercialTermsService(EntityManager em,PlatformTransactionManager manager,JsonMapper json){this.em=em;this.tx=new TransactionTemplate(manager);this.json=json;}
 private UUID actor(String permission){var a=SecurityContextHolder.getContext().getAuthentication();if(!(a instanceof JwtAuthenticationToken jwt)||!a.isAuthenticated()||a.getAuthorities().stream().noneMatch(p->p.getAuthority().equals("SCOPE_"+permission)))throw new AccessDeniedException("Scoped commercial authority required");return UUID.fromString(jwt.getToken().getSubject());}
 private <T>T platform(String permission,Supplier<T> work){UUID actor=actor(permission);return tx.execute(status->{em.createNativeQuery("select set_config('app.platform_actor',:a,true)").setParameter("a",actor.toString()).getSingleResult();Number n=(Number)em.createNativeQuery("select count(*) from platform_operator where actor_id=:a and active").setParameter("a",actor).getSingleResult();if(n.longValue()!=1)throw new AccessDeniedException("Active platform operator required");return work.get();});}
 private void lock(String key){em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k",key).getSingleResult();}
 private TermView once(UUID operation,UUID tenant,String kind,Object input,Supplier<TermView> work){if(operation==null)throw new IllegalArgumentException("Operation required");lock("subscription-lifecycle:"+operation);String hash=hash(kind+"\n"+json.writeValueAsString(input));UUID actor=UUID.fromString(((JwtAuthenticationToken)SecurityContextHolder.getContext().getAuthentication()).getToken().getSubject());var old=em.find(LifecycleOperation.class,operation);if(old!=null){if(!old.actorId.equals(actor)||!Objects.equals(old.tenantId,tenant)||!old.kind.equals(kind)||!old.requestHash.equals(hash))throw new IllegalStateException("Commercial operation reused");return json.readValue(old.responseJson,TermView.class);}var result=work.get();var receipt=new LifecycleOperation();receipt.id=operation;receipt.actorId=actor;receipt.tenantId=tenant;receipt.kind=kind;receipt.requestHash=hash;receipt.requestJson=json.writeValueAsString(input);receipt.responseJson=json.writeValueAsString(result);receipt.occurredAt=Instant.now().truncatedTo(ChronoUnit.MICROS);em.persist(receipt);em.flush();return result;}
 @Override public TermView request(Request input){
  if(input==null||input.tenantId()==null||input.invoiceId()==null||input.planCode()==null||input.basis()==null||!Set.of("PRORATED","NEGOTIATED").contains(input.basis())||input.evidenceSha256()==null||!input.evidenceSha256().matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Typed commercial terms and SHA256 contract evidence required");bounded(input.evidenceReference());bounded(input.reason());
  return platform("platform:billing:write",()->once(input.operationId(),input.tenantId(),"COMMERCIAL_TERMS_REQUEST",input,()->{
   lock("subscription-credit:"+input.invoiceId());var i=em.find(SubscriptionInvoice.class,input.invoiceId());if(i==null||!i.tenantId.equals(input.tenantId())||!i.kind.equals("INVOICE"))throw new NoSuchElementException("Subscription invoice not found");
   var plan=em.find(LifecyclePlan.class,input.planCode());if(plan==null||plan.effectiveAt.isAfter(Instant.now()))throw new IllegalArgumentException("Effective immutable plan required");
   if(em.find(SubscriptionActivation.class,i.id)!=null)throw new IllegalStateException("Consumed invoice cannot receive revised commercial terms");
   if(ChronoUnit.DAYS.between(i.periodStart,i.periodEnd)>=366)throw new IllegalArgumentException("Commercial term must not exceed 366 days");
   if(input.basis().equals("PRORATED")){long days=ChronoUnit.DAYS.between(i.periodStart,i.periodEnd)+1;int monthDays=i.periodStart.lengthOfMonth();BigInteger numerator=BigInteger.valueOf(plan.monthlyPriceXaf).multiply(BigInteger.valueOf(days)).multiply(BigInteger.TWO).add(BigInteger.valueOf(monthDays));long expected=numerator.divide(BigInteger.valueOf(2L*monthDays)).longValueExact();if(!YearMonth.from(i.periodStart).equals(YearMonth.from(i.periodEnd))||i.discountXaf!=0||i.subtotalXaf!=expected)throw new IllegalArgumentException("Proration requires exact inclusive calendar days with half-up XAF rounding");}
   var t=new SubscriptionCommercialTerm();t.id=input.operationId();t.tenantId=input.tenantId();t.invoiceId=i.id;t.planCode=plan.code;t.basis=input.basis();t.status="REQUESTED";t.monthlyPriceXaf=plan.monthlyPriceXaf;t.subtotalXaf=i.subtotalXaf;t.discountXaf=i.discountXaf;t.taxXaf=i.taxXaf;t.totalXaf=i.totalXaf;t.periodStart=i.periodStart;t.periodEnd=i.periodEnd;t.evidenceReference=input.evidenceReference();t.evidenceSha256=input.evidenceSha256();t.reason=input.reason();t.recordedBy=actor("platform:billing:write");em.persist(t);em.flush();return view(t);
  }));
 }
 @Override public TermView approve(UUID operation,UUID id){return platform("platform:billing:approve",()->once(operation,null,"COMMERCIAL_TERMS_APPROVE",id,()->{
  var t=em.find(SubscriptionCommercialTerm.class,id);if(t==null)throw new NoSuchElementException("Commercial terms not found");lock("subscription-credit:"+t.invoiceId);em.refresh(t,LockModeType.PESSIMISTIC_WRITE);var i=em.find(SubscriptionInvoice.class,t.invoiceId);UUID reviewer=actor("platform:billing:approve");if(reviewer.equals(t.recordedBy)||reviewer.equals(i.actorId))throw new AccessDeniedException("Independent invoice and contract reviewer required");if(!t.status.equals("REQUESTED")||em.find(SubscriptionActivation.class,t.invoiceId)!=null)throw new IllegalStateException("Unconsumed requested commercial terms required");t.status="APPROVED";t.reviewedBy=reviewer;t.reviewedAt=Instant.now().truncatedTo(ChronoUnit.MICROS);em.flush();return view(t);
 }));}
 @Override public List<TermView> list(UUID tenant){Objects.requireNonNull(tenant);return platform("platform:billing:read",()->em.createQuery("select t from SubscriptionCommercialTerm t where t.tenantId=:t order by t.periodStart desc,t.id",SubscriptionCommercialTerm.class).setParameter("t",tenant).setMaxResults(100).getResultList().stream().map(this::view).toList());}
 private TermView view(SubscriptionCommercialTerm t){return new TermView(t.id,t.tenantId,t.invoiceId,t.planCode,t.basis,t.status,Long.toString(t.monthlyPriceXaf),Long.toString(t.subtotalXaf),Long.toString(t.discountXaf),Long.toString(t.taxXaf),Long.toString(t.totalXaf),t.periodStart,t.periodEnd,t.evidenceReference,t.evidenceSha256,t.reason,t.recordedBy,t.reviewedBy,t.reviewedAt);}
 private static void bounded(String value){if(value==null||value.isBlank()||value.length()>2000||value.indexOf('\0')>=0)throw new IllegalArgumentException("Bounded reason and evidence reference required");}
 private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
