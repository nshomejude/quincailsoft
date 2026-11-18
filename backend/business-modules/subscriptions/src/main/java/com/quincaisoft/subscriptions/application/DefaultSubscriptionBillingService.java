package com.quincaisoft.subscriptions.application;

import com.quincaisoft.shared.ScopedTransactions;
import com.quincaisoft.shared.TenantContext;
import com.quincaisoft.subscriptions.api.SubscriptionBillingService;
import com.quincaisoft.subscriptions.infrastructure.SubscriptionInvoice;
import com.quincaisoft.subscriptions.infrastructure.SubscriptionBillingEvent;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

@Service
public class DefaultSubscriptionBillingService implements SubscriptionBillingService {
    private final EntityManager em;
    private final TransactionTemplate tx;
    private final ScopedTransactions merchantTx;
    private final JsonMapper json;
    public DefaultSubscriptionBillingService(EntityManager em, PlatformTransactionManager manager,
                                             ScopedTransactions merchantTx, JsonMapper json) {
        this.em=em; this.tx=new TransactionTemplate(manager); this.merchantTx=merchantTx; this.json=json;
    }
    private UUID require(String permission) {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt) || !auth.isAuthenticated()
            || auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("SCOPE_"+permission)
                || (!permission.startsWith("platform:") && a.getAuthority().equals(permission))))
            throw new AccessDeniedException("Current scoped billing authority required");
        return UUID.fromString(jwt.getToken().getSubject());
    }
    private <T> T platform(String permission, Supplier<T> body) {
        UUID actor=require(permission);
        return tx.execute(status->{
            em.createNativeQuery("select set_config('app.platform_actor',:actor,true)")
                .setParameter("actor",actor.toString()).getSingleResult();
            Number count=(Number)em.createNativeQuery("select count(*) from platform_operator where actor_id=:actor and active")
                .setParameter("actor",actor).getSingleResult();
            if(count.longValue()!=1) throw new AccessDeniedException("Active platform operator required");
            return body.get();
        });
    }
    private void lock(String key) {
        em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:key,0))")
            .setParameter("key",key).getSingleResult();
    }
    @Override public Invoice issue(UUID requestId, Issue input) {
        validate(requestId,input);
        long subtotal=money(input.subtotalXaf()),discount=money(input.discountXaf()),tax=money(input.taxXaf());
        if(subtotal<=discount) throw new IllegalArgumentException("Discount must be less than subtotal");
        long net=Math.subtractExact(subtotal,discount),total=Math.addExact(net,tax);
        String hash=hash(json.writeValueAsString(input));
        return platform("platform:billing:write",()->{
            UUID actor=require("platform:billing:write");
            if(input.originalId()!=null) require("platform:billing:credit");
            lock("subscription-invoice:"+input.operationId());
            var old=em.find(SubscriptionInvoice.class,input.operationId());
            if(old!=null) {
                if(!old.actorId.equals(actor)||!old.requestHash.equals(hash))
                    throw new IllegalStateException("Billing operation reused with different actor or payload");
                return view(old);
            }
            if(input.originalId()!=null) {
                lock("subscription-credit:"+input.originalId());
                var original=em.find(SubscriptionInvoice.class,input.originalId());
                if(original==null || !original.tenantId.equals(input.tenantId()) || !original.kind.equals("INVOICE"))
                    throw new NoSuchElementException("Original subscription invoice not found");
                if(original.actorId.equals(actor))
                    throw new AccessDeniedException("Independent billing correction operator required");
                if(!original.issuerName.equals(input.issuerName()) || !original.issuerTaxReference.equals(input.issuerTaxReference())
                    || !original.customerName.equals(input.customerName()) || !original.customerTaxReference.equals(input.customerTaxReference())
                    || !original.jurisdiction.equals(input.jurisdiction()))
                    throw new IllegalArgumentException("Credit identity and jurisdiction must match the original invoice");
                Object[] sums=(Object[])em.createNativeQuery("select coalesce(sum(subtotal_xaf::numeric-discount_xaf::numeric),0),coalesce(sum(tax_xaf::numeric),0) from subscription_invoice where original_id=:id")
                    .setParameter("id",original.id).getSingleResult();
                var creditedNet=new java.math.BigDecimal(sums[0].toString()).toBigIntegerExact();
                var creditedTax=new java.math.BigDecimal(sums[1].toString()).toBigIntegerExact();
                if(creditedNet.add(java.math.BigInteger.valueOf(net)).compareTo(java.math.BigInteger.valueOf(original.subtotalXaf-original.discountXaf))>0
                    || creditedTax.add(java.math.BigInteger.valueOf(tax)).compareTo(java.math.BigInteger.valueOf(original.taxXaf))>0)
                    throw new IllegalStateException("Credits exceed the original net or tax amount");
            }
            var invoice=new SubscriptionInvoice();
            invoice.id=input.operationId(); invoice.tenantId=input.tenantId(); invoice.actorId=actor;
            invoice.requestId=requestId; invoice.requestHash=hash; invoice.originalId=input.originalId();
            invoice.kind=input.originalId()==null?"INVOICE":"CREDIT_NOTE";
            invoice.number=(input.originalId()==null?"SUB-":"SUB-CN-")+invoice.id.toString().toUpperCase(Locale.ROOT);
            invoice.issuerName=input.issuerName(); invoice.issuerTaxReference=input.issuerTaxReference();
            invoice.customerName=input.customerName(); invoice.customerTaxReference=input.customerTaxReference();
            invoice.description=input.description(); invoice.jurisdiction=input.jurisdiction();
            invoice.periodStart=input.periodStart(); invoice.periodEnd=input.periodEnd(); invoice.dueDate=input.dueDate();
            invoice.subtotalXaf=subtotal; invoice.discountXaf=discount; invoice.taxXaf=tax; invoice.totalXaf=total;
            invoice.createdAt=Instant.now().truncatedTo(ChronoUnit.MICROS);
            em.persist(invoice); em.flush();
            var event=new SubscriptionBillingEvent(); event.id=UUID.randomUUID(); event.tenantId=invoice.tenantId;
            event.invoiceId=invoice.id; event.actorId=actor; event.requestId=requestId; event.occurredAt=invoice.createdAt;
            event.eventType=input.originalId()==null?"SUBSCRIPTION_INVOICE_ISSUED":"SUBSCRIPTION_CREDIT_ISSUED";
            em.persist(event); em.flush(); return view(invoice);
        });
    }
    @Override public List<Invoice> platformInvoices(UUID tenantId) {
        Objects.requireNonNull(tenantId,"tenantId");
        return platform("platform:billing:read",()->list(tenantId));
    }
    @Override public List<Invoice> merchantInvoices() {
        UUID actor=require("billing:read");
        return merchantTx.execute(()->{
            var scope=TenantContext.current();
            if(!actor.equals(scope.actorId()))throw new AccessDeniedException("Billing identity differs from authenticated scope");
            Number count=(Number)em.createNativeQuery("select count(*) from tenant_memberships where tenant_id=:tenant and company_id=:company and branch_id=:branch and actor_id=:actor and active and authorization_epoch=:epoch and role_code in ('owner','finance')")
                .setParameter("tenant",scope.tenantId()).setParameter("company",scope.companyId())
                .setParameter("branch",scope.branchId()).setParameter("actor",scope.actorId())
                .setParameter("epoch",TenantContext.authorizationEpoch()).getSingleResult();
            if(count.longValue()!=1)throw new AccessDeniedException("Current owner or finance billing membership required");
            return list(scope.tenantId());
        });
    }
    private List<Invoice> list(UUID tenantId) {
        return em.createQuery("select i from SubscriptionInvoice i where i.tenantId=:tenant order by i.createdAt desc,i.id",SubscriptionInvoice.class)
            .setParameter("tenant",tenantId).setMaxResults(100).getResultList().stream().map(this::view).toList();
    }
    private Invoice view(SubscriptionInvoice i) {
        return new Invoice(i.id,i.tenantId,i.kind,i.originalId,i.number,i.issuerName,i.issuerTaxReference,
            i.customerName,i.customerTaxReference,i.description,i.jurisdiction,i.periodStart,i.periodEnd,i.dueDate,
            Long.toString(i.subtotalXaf),Long.toString(i.discountXaf),Long.toString(i.taxXaf),Long.toString(i.totalXaf),i.createdAt);
    }
    private static void validate(UUID requestId,Issue i) {
        if(requestId==null||i==null||i.operationId()==null||i.tenantId()==null||i.operationId().equals(i.originalId())
            ||i.periodStart()==null||i.periodEnd()==null||i.dueDate()==null||i.periodEnd().isBefore(i.periodStart())||i.dueDate().isBefore(i.periodStart()))
            throw new IllegalArgumentException("Complete operation, tenant and billing dates required");
        for(String value:new String[]{i.issuerName(),i.issuerTaxReference(),i.customerName(),i.description(),i.jurisdiction()})
            if(value==null||value.isBlank()||value.length()>2000||value.indexOf('\0')>=0)
                throw new IllegalArgumentException("Bounded billing identity, jurisdiction and description required");
        if(i.customerTaxReference()==null||i.customerTaxReference().length()>200||i.customerTaxReference().indexOf('\0')>=0)
            throw new IllegalArgumentException("Invalid customer tax reference");
    }
    private static long money(String value) {
        if(value==null||!value.matches("0|[1-9][0-9]{0,18}")) throw new IllegalArgumentException("Canonical integer XAF required");
        return Long.parseLong(value);
    }
    private static String hash(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
