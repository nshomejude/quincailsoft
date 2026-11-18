package com.quincaisoft.subscriptions.notifications;

import com.quincaisoft.subscriptions.api.BillingNotificationDispatcher;
import com.quincaisoft.subscriptions.infrastructure.*;
import jakarta.persistence.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/** Durable at-least-once notification delivery. Receiver deduplication uses the
 * stable event UUID; HTTP acceptance is not proof of legal notice receipt. */
public final class DefaultBillingNotificationDispatcher implements BillingNotificationDispatcher {
    record Claim(UUID eventId,UUID tenantId,int attempt,UUID token,String body){}
    private final EntityManager em;private final TransactionTemplate tx;private final JsonMapper json;
    private final UUID worker;private final BillingNotificationTransport transport;
    public DefaultBillingNotificationDispatcher(EntityManager em,PlatformTransactionManager manager,JsonMapper json,UUID worker,BillingNotificationTransport transport){this.em=em;this.tx=new TransactionTemplate(manager);this.json=json;this.worker=Objects.requireNonNull(worker);this.transport=Objects.requireNonNull(transport);}
    private Instant now(){Number micros=(Number)em.createNativeQuery("select (extract(epoch from clock_timestamp())*1000000)::bigint").getSingleResult();long n=micros.longValue();return Instant.ofEpochSecond(Math.floorDiv(n,1000000),Math.floorMod(n,1000000)*1000);}
    private <T>T enrolled(UUID actor,Supplier<T> work){return tx.execute(status->{em.createNativeQuery("select set_config('app.platform_actor',:a,true)").setParameter("a",actor.toString()).getSingleResult();Number n=(Number)em.createNativeQuery("select count(*) from platform_operator where actor_id=:a and active").setParameter("a",actor).getSingleResult();if(n.longValue()!=1)throw new AccessDeniedException("Active enrolled notification operator required");return work.get();});}
    private UUID caller(String permission){var a=SecurityContextHolder.getContext().getAuthentication();if(!(a instanceof JwtAuthenticationToken jwt)||!a.isAuthenticated()||a.getAuthorities().stream().noneMatch(p->p.getAuthority().equals("SCOPE_"+permission)))throw new AccessDeniedException("Notification authority required");return UUID.fromString(jwt.getToken().getSubject());}
    private Delivery view(NotificationDelivery d){return new Delivery(d.eventId,d.tenantId,d.status,d.attempts,d.attemptBudget,d.lastOutcome,d.lastHttpStatus,d.nextAttemptAt,d.completedAt);}
    private void record(NotificationDelivery d,String stage,UUID token,String outcome,Integer code,UUID actor){
        Long exists=em.createQuery("select count(a) from NotificationAttempt a where a.eventId=:e and a.attemptNumber=:n and a.stage=:s",Long.class).setParameter("e",d.eventId).setParameter("n",d.attempts).setParameter("s",stage).getSingleResult();if(exists!=0)return;
        var a=new NotificationAttempt();a.id=UUID.randomUUID();a.tenantId=d.tenantId;a.eventId=d.eventId;a.attemptNumber=d.attempts;a.stage=stage;a.leaseToken=token;a.actorId=actor;a.outcome=outcome;a.httpStatus=code;a.recordedAt=now();em.persist(a);
    }
    Claim claim(){return enrolled(worker,()->{
        var ids=em.createNativeQuery("select d.event_id from subscription_notification_delivery d join subscription_billing_event e on e.id=d.event_id where ((d.status in ('PENDING','RETRY') and d.next_attempt_at<=clock_timestamp()) or (d.status='IN_FLIGHT' and d.lease_expires_at<=clock_timestamp())) order by e.occurred_at,d.event_id for update of d skip locked limit 1").getResultList();
        if(ids.isEmpty())return null;var d=em.find(NotificationDelivery.class,(UUID)ids.getFirst());Instant time=now();
        if(d.status.equals("IN_FLIGHT"))record(d,"EXPIRED",d.leaseToken,"LEASE_EXPIRED",null,worker);
        if(d.destinationKey!=null&&!d.destinationKey.equals(transport.destinationKey())){record(d,"BLOCKED",d.leaseToken,"DESTINATION_CHANGED",null,worker);d.status="DEAD";d.lastOutcome="DESTINATION_CHANGED";d.leaseToken=null;d.leaseExpiresAt=null;em.flush();return null;}
        if(d.attempts>=d.attemptBudget){d.status="DEAD";d.lastOutcome="ATTEMPTS_EXHAUSTED";d.leaseToken=null;d.leaseExpiresAt=null;em.flush();return null;}
        d.attempts=Math.incrementExact(d.attempts);d.destinationKey=transport.destinationKey();d.status="IN_FLIGHT";d.leaseToken=UUID.randomUUID();d.leaseExpiresAt=time.plusSeconds(60);d.lastOutcome="STARTED";
        record(d,"CLAIMED",d.leaseToken,"STARTED",null,worker);var event=em.find(SubscriptionBillingEvent.class,d.eventId);
        var payload=new LinkedHashMap<String,Object>();payload.put("schemaVersion",1);payload.put("eventId",event.id);payload.put("eventType",event.eventType+".v1");payload.put("tenantId",event.tenantId);payload.put("invoiceId",event.invoiceId);payload.put("correlationId",event.requestId);payload.put("occurredAt",event.occurredAt);
        em.flush();return new Claim(d.eventId,d.tenantId,d.attempts,d.leaseToken,json.writeValueAsString(payload));
    });}
    void finish(Claim claim,Integer statusCode){enrolled(worker,()->{
        var d=em.find(NotificationDelivery.class,claim.eventId(),LockModeType.PESSIMISTIC_WRITE);if(d==null)throw new NoSuchElementException("Delivery disappeared");
        if(!d.status.equals("IN_FLIGHT")||!Objects.equals(d.leaseToken,claim.token())||d.attempts!=claim.attempt())return null;
        Instant time=now();if(!d.leaseExpiresAt.isAfter(time)){record(d,"EXPIRED",d.leaseToken,"LEASE_EXPIRED",statusCode,worker);return null;}
        boolean success=statusCode!=null&&statusCode>=200&&statusCode<300;
        boolean retry=statusCode==null||statusCode==408||statusCode==425||statusCode==429||statusCode>=500;
        String outcome=success?"HTTP_ACCEPTED":statusCode==null?"TRANSPORT_FAILURE":retry?"RETRYABLE_HTTP":"PERMANENT_HTTP";
        record(d,"FINISHED",claim.token(),outcome,statusCode,worker);d.lastOutcome=outcome;d.lastHttpStatus=statusCode;d.leaseToken=null;d.leaseExpiresAt=null;
        if(success){d.status="HTTP_ACCEPTED";d.completedAt=time;}
        else if(retry&&d.attempts<d.attemptBudget){d.status="RETRY";d.nextAttemptAt=time.plusSeconds(Math.min(900,5L<<(Math.min(8,d.attempts-1))));}
        else d.status="DEAD";em.flush();return null;
    });}
    @Override public boolean dispatchOne(){var claim=claim();if(claim==null)return false;Integer code=null;try{var result=transport.send(new BillingNotificationTransport.Envelope(claim.eventId(),claim.body()));if(result!=null&&result.statusCode()>=100&&result.statusCode()<=599)code=result.statusCode();}catch(Exception ignored){/* No endpoint, credential, response body or sensitive exception text is persisted. */}finish(claim,code);return true;}
    @Override public List<Delivery> list(String status){if(status!=null&&!Set.of("PENDING","IN_FLIGHT","RETRY","HTTP_ACCEPTED","DEAD").contains(status))throw new IllegalArgumentException("Unknown delivery status");UUID actor=caller("platform:billing:read");return enrolled(actor,()->{var q=em.createQuery("select d from NotificationDelivery d"+(status==null?"":" where d.status=:s")+" order by d.nextAttemptAt,d.eventId",NotificationDelivery.class);if(status!=null)q.setParameter("s",status);return q.setMaxResults(100).getResultList().stream().map(this::view).toList();});}
    @Override public List<Attempt> attempts(UUID id){UUID actor=caller("platform:billing:read");return enrolled(actor,()->em.createQuery("select a from NotificationAttempt a where a.eventId=:e order by a.recordedAt,a.id",NotificationAttempt.class).setParameter("e",id).setMaxResults(300).getResultList().stream().map(a->new Attempt(a.id,a.eventId,a.attemptNumber,a.stage,a.outcome,a.httpStatus,a.recordedAt)).toList());}
    @Override public Delivery redrive(UUID operation,UUID event,String reason){if(operation==null||event==null||reason==null||reason.isBlank()||reason.length()>2000)throw new IllegalArgumentException("Operation, event and bounded redrive reason required");UUID actor=caller("platform:billing:dispatch");return enrolled(actor,()->{
        em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k","notification-redrive:"+operation).getSingleResult();String hash=hash(event+"\n"+reason);var old=em.find(NotificationRedrive.class,operation);if(old!=null){if(!old.actorId.equals(actor)||!old.eventId.equals(event)||!old.requestHash.equals(hash))throw new IllegalStateException("Redrive operation reused");return json.readValue(old.responseJson,Delivery.class);}
        var d=em.find(NotificationDelivery.class,event,LockModeType.PESSIMISTIC_WRITE);if(d==null)throw new NoSuchElementException("Delivery not found");if(!d.status.equals("DEAD")||d.attemptBudget>92||!transport.destinationKey().equals(d.destinationKey))throw new IllegalStateException("Same-destination dead letter with remaining audited budget required");
        d.attemptBudget+=8;d.status="RETRY";d.nextAttemptAt=now();d.lastOutcome="OPERATOR_REDRIVE";em.flush();var result=view(d);var receipt=new NotificationRedrive();receipt.id=operation;receipt.eventId=event;receipt.actorId=actor;receipt.reason=reason;receipt.requestHash=hash;receipt.responseJson=json.writeValueAsString(result);receipt.recordedAt=now();em.persist(receipt);em.flush();return result;
    });}
    private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
