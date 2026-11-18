package com.quincaisoft.subscriptions.application;

import com.quincaisoft.shared.*;
import com.quincaisoft.subscriptions.api.SubscriptionLifecycleService;
import com.quincaisoft.subscriptions.api.SubscriptionQuotaService;
import com.quincaisoft.subscriptions.domain.SubscriptionRestrictionPolicy;
import com.quincaisoft.subscriptions.infrastructure.*;
import jakarta.persistence.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

@Service
public class DefaultSubscriptionLifecycleService implements SubscriptionLifecycleService {
    private final EntityManager em; private final TransactionTemplate tx; private final ScopedTransactions merchantTx; private final JsonMapper json;private final SubscriptionQuotaService quota;
    public DefaultSubscriptionLifecycleService(EntityManager em,PlatformTransactionManager manager,ScopedTransactions merchantTx,JsonMapper json){this(em,manager,merchantTx,json,new DefaultSubscriptionQuotaService(em,merchantTx,json));}
    @Autowired public DefaultSubscriptionLifecycleService(EntityManager em,PlatformTransactionManager manager,ScopedTransactions merchantTx,JsonMapper json,SubscriptionQuotaService quota){this.em=em;this.tx=new TransactionTemplate(manager);this.merchantTx=merchantTx;this.json=json;this.quota=quota;}
    private Instant now(){return Instant.now().truncatedTo(ChronoUnit.MICROS);}
    private UUID actor(String... authorities){
        var a=SecurityContextHolder.getContext().getAuthentication();
        if(!(a instanceof JwtAuthenticationToken j)||!a.isAuthenticated()||a.getAuthorities().stream().noneMatch(x->Arrays.stream(authorities).anyMatch(p->x.getAuthority().equals("SCOPE_"+p))))throw new AccessDeniedException("Platform subscription authority required");
        return UUID.fromString(j.getToken().getSubject());
    }
    private <T>T platform(Supplier<T> work,String... permissions){UUID actor=actor(permissions);return tx.execute(status->{
        em.createNativeQuery("select set_config('app.platform_actor',:a,true)").setParameter("a",actor.toString()).getSingleResult();
        Number n=(Number)em.createNativeQuery("select count(*) from platform_operator where actor_id=:a and active").setParameter("a",actor).getSingleResult();
        if(n.longValue()!=1)throw new AccessDeniedException("Active enrolled platform operator required");return work.get();});}
    private void lock(String key){em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k",key).getSingleResult();}
    private <T>T once(UUID operation,UUID tenant,String kind,Object request,Class<T> type,Supplier<T> work){
        if(operation==null)throw new IllegalArgumentException("Operation required");
        lock("subscription-lifecycle:"+operation);
        UUID actor=UUID.fromString(((JwtAuthenticationToken)SecurityContextHolder.getContext().getAuthentication()).getToken().getSubject());
        String hash=hash(kind+"\n"+json.writeValueAsString(request));var previous=em.find(LifecycleOperation.class,operation);
        if(previous!=null){if(!previous.actorId.equals(actor)||!Objects.equals(previous.tenantId,tenant)||!previous.kind.equals(kind)||!previous.requestHash.equals(hash))throw new IllegalStateException("Subscription operation reused");return json.readValue(previous.responseJson,type);}
        T result=work.get();em.flush();var receipt=new LifecycleOperation();receipt.id=operation;receipt.tenantId=tenant;receipt.actorId=actor;receipt.kind=kind;receipt.requestHash=hash;receipt.requestJson=json.writeValueAsString(request);receipt.responseJson=json.writeValueAsString(result);receipt.occurredAt=now();em.persist(receipt);em.flush();return result;
    }
    private LifecyclePlan plan(String code){var p=em.find(LifecyclePlan.class,code);if(p==null||p.effectiveAt.isAfter(now()))throw new IllegalArgumentException("Effective subscription plan required");return p;}
    private TenantSubscription subscription(UUID tenant,LockModeType mode){var s=em.find(TenantSubscription.class,tenant,mode);if(s==null)throw new NoSuchElementException("Subscription not found");return s;}
    private String effectiveStatus(TenantSubscription s){return (s.status.equals("TRIAL")&&!s.trialEndsAt.isAfter(now()))||(s.status.equals("ACTIVE")&&!s.termEndsAt.isAfter(now()))?"EXPIRED":s.status;}
    private Snapshot view(TenantSubscription s){var p=plan(s.planCode);return new Snapshot(s.tenantId,s.planCode,effectiveStatus(s),s.trialEndsAt,s.version,true,p.commerceAccess,p.seatLimit,p.branchLimit,p.warehouseLimit,s.termEndsAt);}
    private Plan view(LifecyclePlan p){return new Plan(p.code,p.name,Long.toString(p.monthlyPriceXaf),p.seatLimit,p.branchLimit,p.warehouseLimit,p.commerceAccess,p.effectiveAt);}
    private NoticeView view(SubscriptionNotice n){return new NoticeView(n.id,n.tenantId,n.subscriptionVersion,n.status,n.recordedBy,n.reviewedBy,n.deliveredAt,n.graceEndsAt,n.reviewedAt,n.deliveryReference,n.contractReference,n.reason);}
    @Override public List<NoticeView> notices(UUID tenant){return platform(()->em.createQuery("select n from SubscriptionNotice n where n.tenantId=:t order by n.deliveredAt desc,n.id",SubscriptionNotice.class).setParameter("t",tenant).setMaxResults(100).getResultList().stream().map(this::view).toList(),"platform:billing:read");}
    @Override public Snapshot bootstrapTrial(UUID operation,UUID tenant,String code,Instant ends){
        if(tenant==null||code==null||ends==null)throw new IllegalArgumentException("Tenant, plan and trial end required");
        return platform(()->once(operation,tenant,"TRIAL_BOOTSTRAP",List.of(tenant,code,ends),Snapshot.class,()->{
            lock("subscription-tenant:"+tenant);if(em.find(TenantSubscription.class,tenant)!=null)throw new IllegalStateException("Tenant already has a subscription");
            var chosen=plan(code);Instant start=now();if(!ends.isAfter(start)||ends.isAfter(start.plus(90,ChronoUnit.DAYS)))throw new IllegalArgumentException("Trial must end within 90 days");
            var s=new TenantSubscription();s.tenantId=tenant;s.planCode=code;s.status="TRIAL";s.trialEndsAt=ends.truncatedTo(ChronoUnit.MICROS);s.createdAt=start;s.updatedAt=start;s.version=0;em.persist(s);
            if(chosen.commerceAccess){var e=new TrialEntitlement();e.tenantId=tenant;e.feature="COMMERCE";e.id=UUID.randomUUID();e.enabled=true;e.expiresAt=s.trialEndsAt;em.persist(e);}em.flush();return view(s);
        }),"platform:tenants:provision","platform:billing:write");
    }
    @Override public Snapshot verifyTrial(UUID tenant){return platform(()->view(subscription(tenant,LockModeType.PESSIMISTIC_READ)),"platform:tenants:provision","platform:billing:read");}
    @Override public List<Plan> plans(){return platform(()->em.createQuery("select p from LifecyclePlan p order by p.code",LifecyclePlan.class).getResultList().stream().map(this::view).toList(),"platform:billing:read","platform:tenants:provision");}
    @Override public Plan createPlan(UUID operation,Plan input){
        if(input==null||input.code()==null||!input.code().matches("[a-z0-9][a-z0-9-]{0,63}")||input.name()==null||input.name().isBlank()||input.name().length()>120||input.monthlyPriceXaf()==null||!input.monthlyPriceXaf().matches("0|[1-9][0-9]{0,18}")||input.seatLimit()<1||input.branchLimit()<1||input.warehouseLimit()<1||input.effectiveAt()==null)throw new IllegalArgumentException("Complete typed plan limits and integral XAF price required");
        long price=Long.parseLong(input.monthlyPriceXaf());
        return platform(()->once(operation,null,"CREATE_PLAN",input,Plan.class,()->{var p=new LifecyclePlan();p.code=input.code();p.name=input.name();p.monthlyPriceXaf=price;p.seatLimit=input.seatLimit();p.branchLimit=input.branchLimit();p.warehouseLimit=input.warehouseLimit();p.commerceAccess=input.commerceAccess();p.effectiveAt=input.effectiveAt().truncatedTo(ChronoUnit.MICROS);em.persist(p);em.flush();return view(p);}),"platform:billing:write");
    }
    @Override public Snapshot current(){return merchantTx.execute(()->view(subscription(TenantContext.current().tenantId(),LockModeType.PESSIMISTIC_READ)));}
    private void disableCommerce(UUID tenant){var e=em.find(TrialEntitlement.class,new TrialEntitlement.Key(tenant,"COMMERCE"),LockModeType.PESSIMISTIC_WRITE);if(e!=null)e.enabled=false;}
    @Override public Snapshot expireTrial(UUID operation,UUID tenant,long version){return platform(()->once(operation,tenant,"EXPIRE_TERM",List.of(tenant,version),Snapshot.class,()->{var s=subscription(tenant,LockModeType.PESSIMISTIC_WRITE);if(s.version!=version||!Set.of("TRIAL","ACTIVE").contains(s.status)||!effectiveStatus(s).equals("EXPIRED"))throw new IllegalStateException("Current elapsed trial or paid term required");s.status="EXPIRED";s.version=Math.incrementExact(s.version);s.updatedAt=now();disableCommerce(tenant);em.flush();return view(s);}),"platform:billing:write");}
    @Override public Snapshot cancel(UUID operation,UUID tenant,long version,String reason){bounded(reason,"Cancellation reason");return platform(()->once(operation,tenant,"CANCEL",List.of(tenant,version,reason),Snapshot.class,()->{var s=subscription(tenant,LockModeType.PESSIMISTIC_WRITE);if(s.version!=version||s.status.equals("CANCELLED"))throw new IllegalStateException("Subscription version changed");s.status="CANCELLED";s.version=Math.incrementExact(s.version);s.updatedAt=now();disableCommerce(tenant);em.flush();return view(s);}),"platform:billing:write");}
    @Override public NoticeView recordNotice(Notice input){
        if(input==null||input.tenantId()==null||input.subscriptionVersion()<0||input.deliveredAt()==null||input.graceEndsAt()==null||!input.graceEndsAt().isAfter(input.deliveredAt()))throw new IllegalArgumentException("Positive documented grace period required");bounded(input.deliveryReference(),"Delivery evidence");bounded(input.contractReference(),"Contract evidence");bounded(input.reason(),"Notice reason");
        return platform(()->once(input.operationId(),input.tenantId(),"RECORD_NOTICE",input,NoticeView.class,()->{
            var s=subscription(input.tenantId(),LockModeType.PESSIMISTIC_WRITE);if(s.version!=input.subscriptionVersion()||input.deliveredAt().isAfter(now()))throw new IllegalStateException("Current subscription and past delivery evidence required");
            var n=new SubscriptionNotice();n.id=input.operationId();n.tenantId=input.tenantId();n.subscriptionVersion=s.version;n.deliveryReference=input.deliveryReference();n.deliveredAt=input.deliveredAt().truncatedTo(ChronoUnit.MICROS);n.graceEndsAt=input.graceEndsAt().truncatedTo(ChronoUnit.MICROS);n.contractReference=input.contractReference();n.reason=input.reason();n.status="RECORDED";n.recordedBy=actor("platform:billing:write");em.persist(n);em.flush();return view(n);
        }),"platform:billing:write");
    }
    @Override public NoticeView approveNotice(UUID operation,UUID id){return platform(()->once(operation,null,"APPROVE_NOTICE",id,NoticeView.class,()->{
        var n=em.find(SubscriptionNotice.class,id);if(n==null)throw new NoSuchElementException("Notice not found");
        var s=subscription(n.tenantId,LockModeType.PESSIMISTIC_WRITE);em.refresh(n,LockModeType.PESSIMISTIC_WRITE);
        UUID reviewer=actor("platform:billing:approve");if(n.recordedBy.equals(reviewer))throw new AccessDeniedException("Independent contract and delivery reviewer required");
        if(s.version!=n.subscriptionVersion||!n.status.equals("RECORDED"))throw new IllegalStateException("Current unreviewed notice required");n.reviewedBy=reviewer;n.reviewedAt=now();n.status="APPROVED";em.flush();return view(n);
    }),"platform:billing:approve");}
    private PaymentView view(ManualSubscriptionPayment p){return new PaymentView(p.id,p.tenantId,p.invoiceId,p.planCode,Long.toString(p.amountXaf),p.bankReference,p.evidenceSha256,p.reason,p.status,p.recordedBy,p.reviewedBy,p.reviewedAt);}
    @Override public List<PaymentView> payments(UUID tenant){return platform(()->em.createQuery("select p from ManualSubscriptionPayment p where p.tenantId=:t order by p.id",ManualSubscriptionPayment.class).setParameter("t",tenant).setMaxResults(100).getResultList().stream().map(this::view).toList(),"platform:billing:read");}
    @Override public String printReceipt(UUID id,boolean platform,String locale){Supplier<String> render=()->{var p=em.find(ManualSubscriptionPayment.class,id);if(p==null)throw new NoSuchElementException("Subscription receipt not found");var invoice=em.find(SubscriptionInvoice.class,p.invoiceId);if(invoice==null)throw new NoSuchElementException("Subscription invoice not found");return SubscriptionHtml.receipt(p,invoice,locale);};
        if(platform)return platform(render,"platform:billing:read");
        var a=SecurityContextHolder.getContext().getAuthentication();if(!(a instanceof JwtAuthenticationToken jwt)||a.getAuthorities().stream().noneMatch(x->x.getAuthority().equals("billing:read")||x.getAuthority().equals("SCOPE_billing:read")))throw new AccessDeniedException("Billing read authority required");
        return merchantTx.execute(()->{var s=TenantContext.current();if(!s.actorId().toString().equals(jwt.getToken().getSubject()))throw new AccessDeniedException("Billing actor mismatch");Number n=(Number)em.createNativeQuery("select count(*) from tenant_memberships where tenant_id=:t and company_id=:c and branch_id=:b and actor_id=:a and active and authorization_epoch=:e and role_code in ('owner','finance')").setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("a",s.actorId()).setParameter("e",TenantContext.authorizationEpoch()).getSingleResult();if(n.longValue()!=1)throw new AccessDeniedException("Current owner or finance membership required");return render.get();});
    }
    @Override public PaymentView recordPayment(Payment input){
        if(input==null||input.tenantId()==null||input.invoiceId()==null||input.planCode()==null||input.amountXaf()==null||!input.amountXaf().matches("[1-9][0-9]{0,18}")||input.evidenceSha256()==null||!input.evidenceSha256().matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invoice, selected plan, positive integral amount and SHA256 evidence required");
        bounded(input.bankReference(),"Bank account and transaction reference");bounded(input.reason(),"Reconciliation reason");long amount=Long.parseLong(input.amountXaf());
        return platform(()->once(input.operationId(),input.tenantId(),"RECORD_PAYMENT",input,PaymentView.class,()->{
            subscription(input.tenantId(),LockModeType.PESSIMISTIC_WRITE);plan(input.planCode());lock("subscription-credit:"+input.invoiceId());
            var invoice=em.find(SubscriptionInvoice.class,input.invoiceId());if(invoice==null||!invoice.tenantId.equals(input.tenantId())||!invoice.kind.equals("INVOICE"))throw new NoSuchElementException("Subscription invoice not found");
            var p=new ManualSubscriptionPayment();p.id=input.operationId();p.tenantId=input.tenantId();p.invoiceId=input.invoiceId();p.planCode=input.planCode();p.amountXaf=amount;p.bankReference=input.bankReference().trim();p.evidenceSha256=input.evidenceSha256();p.reason=input.reason();p.status="RECORDED";p.recordedBy=actor("platform:billing:write");em.persist(p);em.flush();return view(p);
        }),"platform:billing:write");
    }
    @Override public PaymentView approvePayment(UUID operation,UUID id){return platform(()->once(operation,null,"APPROVE_PAYMENT",id,PaymentView.class,()->{
        var p=em.find(ManualSubscriptionPayment.class,id);if(p==null)throw new NoSuchElementException("Payment evidence not found");var s=subscription(p.tenantId,LockModeType.PESSIMISTIC_WRITE);em.refresh(p,LockModeType.PESSIMISTIC_WRITE);lock("subscription-credit:"+p.invoiceId);
        UUID reviewer=actor("platform:billing:approve");if(p.recordedBy.equals(reviewer))throw new AccessDeniedException("Independent bank reconciliation reviewer required");if(!p.status.equals("RECORDED"))throw new IllegalStateException("Unreviewed payment required");
        p.reviewedBy=reviewer;p.reviewedAt=now();p.status="MANUALLY_RECONCILED";em.flush();activateIfCovered(s,p,false);return view(p);
    }),"platform:billing:approve");}
    @Override public Snapshot activatePayment(UUID operation,UUID id){return platform(()->once(operation,null,"ACTIVATE_PAYMENT",id,Snapshot.class,()->{
        var p=em.find(ManualSubscriptionPayment.class,id);if(p==null)throw new NoSuchElementException("Payment not found");var s=subscription(p.tenantId,LockModeType.PESSIMISTIC_WRITE);lock("subscription-credit:"+p.invoiceId);activateIfCovered(s,p,true);return view(s);
    }),"platform:billing:approve");}
    private void activateIfCovered(TenantSubscription s,ManualSubscriptionPayment payment,boolean required){
        if(!payment.status.equals("MANUALLY_RECONCILED"))throw new IllegalStateException("Reviewed manual receipt required");
        var invoice=em.find(SubscriptionInvoice.class,payment.invoiceId);Instant start=invoice.periodStart.atStartOfDay(ZoneOffset.UTC).toInstant(),end=invoice.periodEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var paid=new java.math.BigDecimal(em.createNativeQuery("select coalesce(sum(amount_xaf::numeric),0) from subscription_manual_payment where invoice_id=:i and status='MANUALLY_RECONCILED'").setParameter("i",invoice.id).getSingleResult().toString()).toBigIntegerExact();
        var credits=new java.math.BigDecimal(em.createNativeQuery("select coalesce(sum(total_xaf::numeric),0) from subscription_invoice where original_id=:i").setParameter("i",invoice.id).getSingleResult().toString()).toBigIntegerExact();
        if(!paid.equals(java.math.BigInteger.valueOf(invoice.totalXaf).subtract(credits))||start.isAfter(now())||!end.isAfter(now())){if(required)throw new IllegalStateException("Full invoice reconciliation and current service period required");return;}
        if(em.find(SubscriptionActivation.class,invoice.id)!=null)throw new IllegalStateException("Invoice already consumed for subscription activation; a new term requires a new invoice");
        if(s.termEndsAt!=null&&end.isBefore(s.termEndsAt))throw new IllegalStateException("A historical receipt cannot shorten the paid term");
        var chosen=plan(payment.planCode);
        Boolean commercial=(Boolean)em.createNativeQuery("select subscription_commercial_matches(i,:plan) from subscription_invoice i where i.id=:id").setParameter("plan",chosen.code).setParameter("id",invoice.id).getSingleResult();
        if(!Boolean.TRUE.equals(commercial)||credits.signum()!=0)throw new IllegalStateException("Paid activation requires matching plan price or independently approved commercial terms, with no credited invoice");
        var activation=new SubscriptionActivation();activation.invoiceId=invoice.id;activation.tenantId=s.tenantId;activation.paymentId=payment.id;activation.activatedAt=now();em.persist(activation);em.flush();
        s.planCode=chosen.code;s.status="ACTIVE";s.termEndsAt=end;s.activationPaymentId=payment.id;s.version=Math.incrementExact(s.version);s.updatedAt=now();
        var entitlement=em.find(TrialEntitlement.class,new TrialEntitlement.Key(s.tenantId,"COMMERCE"),LockModeType.PESSIMISTIC_WRITE);
        if(entitlement==null&&chosen.commerceAccess){entitlement=new TrialEntitlement();entitlement.tenantId=s.tenantId;entitlement.feature="COMMERCE";entitlement.id=UUID.randomUUID();entitlement.enabled=true;entitlement.expiresAt=end;em.persist(entitlement);}
        if(entitlement!=null){entitlement.enabled=chosen.commerceAccess;entitlement.expiresAt=end;}em.flush();
    }
    @Override public void requireAllowed(Operation operation){
        Objects.requireNonNull(operation);if(operation!=Operation.NEW_BUSINESS_POSTING)return;
        merchantTx.execute(()->{
            UUID tenant=TenantContext.current().tenantId();var s=em.find(TenantSubscription.class,tenant,LockModeType.PESSIMISTIC_READ);
            // No contract is inferred for legacy/perpetual tenants. This grants no business permission.
            if(s==null)return null;
            var status=effectiveStatus(s);
            var notices=em.createQuery("select n from SubscriptionNotice n where n.tenantId=:tenant and n.subscriptionVersion=:version and n.status='APPROVED' order by n.reviewedAt desc,n.id",SubscriptionNotice.class).setParameter("tenant",tenant).setParameter("version",s.version).setMaxResults(1).setLockMode(LockModeType.PESSIMISTIC_READ).getResultList();
            SubscriptionRestrictionPolicy.RestrictionEvidence evidence=null;
            if(!notices.isEmpty()){var n=notices.getFirst();evidence=new SubscriptionRestrictionPolicy.RestrictionEvidence(tenant,tenant,s.version,n.id,n.deliveredAt,n.graceEndsAt,n.id,n.reviewedAt);}
            var decision=SubscriptionRestrictionPolicy.evaluate(new SubscriptionRestrictionPolicy.Subscription(tenant,tenant,s.version,SubscriptionRestrictionPolicy.Status.valueOf(status)),SubscriptionRestrictionPolicy.Operation.NEW_BUSINESS_POSTING,evidence,now());
            if(decision==SubscriptionRestrictionPolicy.Decision.RESTRICT_NEW_POSTING)throw new AccessDeniedException("New posting restricted after reviewed notice and grace; historical access remains available");return null;
        });
    }
    @Override public void requireAllocation(Resource resource,long existing,long requested){
        if(resource==null||existing<0||requested<=0)throw new IllegalArgumentException("Trusted nonnegative usage and positive allocation required");
        // Retained source compatibility only: caller-provided usage is never authoritative.
        quota.requireCapacity(SubscriptionQuotaService.Resource.valueOf(resource.name()),requested);
    }
    private static void bounded(String s,String name){if(s==null||s.isBlank()||s.length()>2000||s.indexOf('\0')>=0)throw new IllegalArgumentException(name+" required and bounded");}
    private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
