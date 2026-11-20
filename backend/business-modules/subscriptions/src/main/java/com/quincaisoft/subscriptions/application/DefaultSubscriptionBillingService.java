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
        return merchant(()->list(TenantContext.current().tenantId()));
    }
    private <T>T merchant(Supplier<T> work){
        UUID actor=require("billing:read");
        return merchantTx.execute(()->{
            var scope=TenantContext.current();
            if(!actor.equals(scope.actorId()))throw new AccessDeniedException("Billing identity differs from authenticated scope");
            Number count=(Number)em.createNativeQuery("select count(*) from tenant_memberships where tenant_id=:tenant and company_id=:company and branch_id=:branch and actor_id=:actor and active and authorization_epoch=:epoch and role_code in ('owner','finance')")
                .setParameter("tenant",scope.tenantId()).setParameter("company",scope.companyId())
                .setParameter("branch",scope.branchId()).setParameter("actor",scope.actorId())
                .setParameter("epoch",TenantContext.authorizationEpoch()).getSingleResult();
            if(count.longValue()!=1)throw new AccessDeniedException("Current owner or finance billing membership required");
            return work.get();
        });
    }
    @Override public String printInvoice(UUID id,boolean platform,String locale){
        Supplier<String> render=()->{var invoice=em.find(SubscriptionInvoice.class,id);if(invoice==null)throw new NoSuchElementException("Subscription invoice not found");return SubscriptionHtml.invoice(invoice,locale);};
        return platform?platform("platform:billing:read",render):merchant(render);
    }
    private List<Invoice> list(UUID tenantId) {
        return em.createQuery("select i from SubscriptionInvoice i where i.tenantId=:tenant order by i.createdAt desc,i.id",SubscriptionInvoice.class)
            .setParameter("tenant",tenantId).setMaxResults(100).getResultList().stream().map(this::view).toList();
    }
    private record Cursor(UUID tenantId,Instant cutoff,Instant createdAt,UUID id,Instant expiresAt,String criteria){}
    private static final String CURSOR_CRITERIA="SUBSCRIPTION_INVOICES_CREATED_DESC_ID_DESC_V1";
    @Override public InvoicePage platformPage(UUID tenant,String cursor,int limit){Objects.requireNonNull(tenant);return platform("platform:billing:read",()->page(tenant,cursor,limit));}
    @Override public InvoicePage merchantPage(String cursor,int limit){return merchant(()->page(TenantContext.current().tenantId(),cursor,limit));}
    @Override public CsvPage platformExport(UUID tenant,String cursor,int limit){return csv(platformPage(tenant,cursor,limit));}
    @Override public CsvPage merchantExport(String cursor,int limit){return csv(merchantPage(cursor,limit));}
    private InvoicePage page(UUID tenant,String token,int limit){
        if(limit<1||limit>200)throw new IllegalArgumentException("Page size must be between 1 and 200");
        Instant now=Instant.now().truncatedTo(ChronoUnit.MICROS);Cursor cursor=new Cursor(tenant,now,null,null,now.plusSeconds(900),CURSOR_CRITERIA);Instant cutoff=now;
        byte[] key=(byte[])em.createNativeQuery("select secret from subscription_cursor_key where id=1").getSingleResult();
        if(token!=null&&!token.isBlank()){
            try{if(token.length()>1024)throw new IllegalArgumentException();String[] parts=token.split("\\.",-1);if(parts.length!=2||!MessageDigest.isEqual(mac(parts[0],key),Base64.getUrlDecoder().decode(parts[1])))throw new IllegalArgumentException();cursor=json.readValue(new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8),Cursor.class);}
            catch(RuntimeException invalid){throw new IllegalArgumentException("Invalid invoice cursor");}
            if(cursor.tenantId()==null||!tenant.equals(cursor.tenantId())||cursor.cutoff()==null||cursor.expiresAt()==null||!cursor.expiresAt().isAfter(now)||!CURSOR_CRITERIA.equals(cursor.criteria())||(cursor.createdAt()==null)!=(cursor.id()==null)||cursor.cutoff().isAfter(cutoff)||(cursor.createdAt()!=null&&cursor.createdAt().isAfter(cursor.cutoff())))throw new IllegalArgumentException("Cursor belongs to another scope, sort or expired time boundary");
            if(cursor.id()!=null){var anchor=em.find(SubscriptionInvoice.class,cursor.id());if(anchor==null||!anchor.tenantId.equals(tenant)||!anchor.createdAt.equals(cursor.createdAt()))throw new IllegalArgumentException("Invoice cursor anchor is unavailable");}cutoff=cursor.cutoff();
        }
        String current=encode(cursor,key);
        var query=em.createQuery("select i from SubscriptionInvoice i where i.tenantId=:tenant and i.createdAt<=:cutoff"+(cursor.id()==null?"":" and (i.createdAt<:afterTime or (i.createdAt=:afterTime and i.id<:afterId))")+" order by i.createdAt desc,i.id desc",SubscriptionInvoice.class).setParameter("tenant",tenant).setParameter("cutoff",cutoff);
        if(cursor.id()!=null)query.setParameter("afterTime",cursor.createdAt()).setParameter("afterId",cursor.id());
        var rows=query.setMaxResults(limit+1).getResultList();boolean more=rows.size()>limit;var items=rows.subList(0,Math.min(rows.size(),limit)).stream().map(this::view).toList();String next=null;
        if(more){var last=items.getLast();next=encode(new Cursor(tenant,cutoff,last.createdAt(),last.id(),cursor.expiresAt(),CURSOR_CRITERIA),key);}
        return new InvoicePage(items,next,cutoff,current);
    }
    private String encode(Cursor cursor,byte[] key){String payload=Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsString(cursor).getBytes(StandardCharsets.UTF_8));return payload+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(mac(payload,key));}
    private static byte[] mac(String payload,byte[] key){try{var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));}catch(java.security.GeneralSecurityException error){throw new IllegalStateException("Cursor signing unavailable",error);}}
    private static CsvPage csv(InvoicePage page){
        StringBuilder out=new StringBuilder("invoice_id,tenant_id,kind,number,original_id,issuer,issuer_tax_reference,customer,customer_tax_reference,description,jurisdiction,period_start,period_end,due_date,subtotal_xaf,discount_xaf,tax_xaf,total_xaf,created_at\r\n");
        for(var i:page.items()){String[] fields={i.id().toString(),i.tenantId().toString(),i.kind(),i.number(),i.originalId()==null?"":i.originalId().toString(),i.issuerName(),i.issuerTaxReference(),i.customerName(),i.customerTaxReference(),i.description(),i.jurisdiction(),i.periodStart().toString(),i.periodEnd().toString(),i.dueDate().toString(),i.subtotalXaf(),i.discountXaf(),i.taxXaf(),i.totalXaf(),i.createdAt().toString()};for(int n=0;n<fields.length;n++){if(n>0)out.append(',');out.append(csvCell(fields[n]));}out.append("\r\n");}
        return new CsvPage(out.toString(),page.nextCursor(),page.cutoff(),page.currentCursor());
    }
    private static String csvCell(String value){String safe=value==null?"":value;String leading=safe.stripLeading();if((!leading.isEmpty()&&"=+-@".indexOf(leading.charAt(0))>=0)||(!safe.isEmpty()&&"\t\r\n".indexOf(safe.charAt(0))>=0))safe="'"+safe;return "\""+safe.replace("\"","\"\"")+"\"";}
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
