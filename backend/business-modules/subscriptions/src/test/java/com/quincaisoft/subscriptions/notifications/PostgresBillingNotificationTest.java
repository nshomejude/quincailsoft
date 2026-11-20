package com.quincaisoft.subscriptions.notifications;
import com.quincaisoft.subscriptions.api.*;
import com.quincaisoft.subscriptions.application.DefaultSubscriptionBillingService;
import com.quincaisoft.shared.ScopedTransactions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;
import java.sql.*;import java.nio.file.*;import java.time.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicInteger;import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresBillingNotificationTest {
 @Test void claimsRetriesCrashRecoveryFencingAndRedriveUseDurableEvidence()throws Exception{
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");String db="notify_"+UUID.randomUUID().toString().replace("-",""),role="notify_role_"+UUID.randomUUID().toString().replace("-","");String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID maker=UUID.randomUUID(),worker=UUID.randomUUID(),tenant=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")){
    try(var files=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));}
    sql(owner,"INSERT INTO platform_operator VALUES('"+maker+"',true),('"+worker+"',true)");sql(owner,"INSERT INTO tenant(id,name)VALUES('"+tenant+"','Synthetic notifications')");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.subscriptions.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var manager=new JpaTransactionManager(emf);var json=new JsonMapper();
    ScopedTransactions noMerchant=new ScopedTransactions(){public <T>T execute(Supplier<T> work){throw new UnsupportedOperationException("No merchant path in this worker test");}};
    var invoices=new DefaultSubscriptionBillingService(em,manager,noMerchant,json);AtomicInteger nextCode=new AtomicInteger(503);List<BillingNotificationTransport.Envelope> sent=new CopyOnWriteArrayList<>();
    BillingNotificationTransport simulated=new BillingNotificationTransport(){public String destinationKey(){return "a".repeat(64);}public Result send(Envelope e){sent.add(e);return new Result(nextCode.get());}};
    var dispatcher=new DefaultBillingNotificationDispatcher(em,manager,json,worker,simulated);
    as(maker,"platform:billing:write","platform:billing:read");var invoice=invoices.issue(UUID.randomUUID(),invoice(tenant));assertEquals(1,count(owner,"SELECT count(*) FROM subscription_notification_delivery WHERE status='PENDING'"));
    assertTrue(dispatcher.dispatchOne());assertEquals(1,count(owner,"SELECT count(*) FROM subscription_notification_delivery WHERE status='RETRY' AND attempts=1"));assertFalse(dispatcher.dispatchOne(),"Backoff prevents immediate retry");
    assertFalse(sent.getFirst().body().contains("Sensitive synthetic issuer"));assertFalse(sent.getFirst().body().contains("\"totalXaf\""));assertTrue(sent.getFirst().body().length()<4096);
    sql(owner,"UPDATE subscription_notification_delivery SET next_attempt_at=now()-interval '1 second' WHERE status='RETRY'");nextCode.set(202);assertTrue(dispatcher.dispatchOne());assertEquals(2,sent.size());assertEquals(sent.getFirst(),sent.getLast(),"Retry preserves event UUID and exact bounded body");assertFalse(dispatcher.dispatchOne());assertEquals(1,count(owner,"SELECT count(*) FROM subscription_notification_delivery WHERE status='HTTP_ACCEPTED' AND attempts=2"));
    assertThrows(SQLException.class,()->sql(owner,"UPDATE subscription_notification_delivery SET status='RETRY' WHERE status='HTTP_ACCEPTED'"));
    invoices.issue(UUID.randomUUID(),invoice(tenant));var second=new DefaultBillingNotificationDispatcher(em,manager,json,worker,simulated);
    DefaultBillingNotificationDispatcher.Claim oldClaim;
    try(var pool=Executors.newFixedThreadPool(2)){var start=new CountDownLatch(1);var a=pool.submit(()->{start.await();return dispatcher.claim();});var b=pool.submit(()->{start.await();return second.claim();});start.countDown();var x=a.get(20,TimeUnit.SECONDS);var y=b.get(20,TimeUnit.SECONDS);assertNotEquals(x==null,y==null,"Only one concurrent worker owns the event");oldClaim=x==null?y:x;}
    sql(owner,"UPDATE subscription_notification_delivery SET lease_expires_at=now()-interval '1 second' WHERE event_id='"+oldClaim.eventId()+"'");var reclaimed=second.claim();assertEquals(oldClaim.eventId(),reclaimed.eventId());assertNotEquals(oldClaim.token(),reclaimed.token());assertEquals(oldClaim.body(),reclaimed.body());dispatcher.finish(oldClaim,200);assertEquals(1,count(owner,"SELECT count(*) FROM subscription_notification_delivery WHERE event_id='"+oldClaim.eventId()+"' AND status='IN_FLIGHT'"));second.finish(reclaimed,202);assertEquals(1,count(owner,"SELECT count(*) FROM subscription_notification_attempt WHERE event_id='"+oldClaim.eventId()+"' AND stage='EXPIRED'"));
    invoices.issue(UUID.randomUUID(),invoice(tenant));nextCode.set(400);assertTrue(dispatcher.dispatchOne());var dead=dispatcher.list("DEAD").getFirst();assertEquals("PERMANENT_HTTP",dead.lastOutcome());
    assertThrows(AccessDeniedException.class,()->dispatcher.redrive(UUID.randomUUID(),dead.eventId(),"Synthetic retry review"));as(maker,"platform:billing:dispatch","platform:billing:read");UUID redrive=UUID.randomUUID();var receipt=dispatcher.redrive(redrive,dead.eventId(),"Synthetic retry review");assertEquals("RETRY",receipt.status());assertEquals(receipt,dispatcher.redrive(redrive,dead.eventId(),"Synthetic retry review"));assertThrows(IllegalStateException.class,()->dispatcher.redrive(redrive,dead.eventId(),"Changed reason"));nextCode.set(202);assertTrue(dispatcher.dispatchOne());assertEquals(receipt,dispatcher.redrive(redrive,dead.eventId(),"Synthetic retry review"));assertTrue(dispatcher.attempts(dead.eventId()).size()>=4);
    assertThrows(SQLException.class,()->sql(owner,"UPDATE subscription_notification_attempt SET outcome='HTTP_ACCEPTED'"));
    as(maker,"platform:billing:write","platform:billing:read");invoices.issue(UUID.randomUUID(),invoice(tenant));var bound=dispatcher.claim();dispatcher.finish(bound,503);sql(owner,"UPDATE subscription_notification_delivery SET next_attempt_at=now()-interval '1 second' WHERE event_id='"+bound.eventId()+"'");
    BillingNotificationTransport changed=new BillingNotificationTransport(){public String destinationKey(){return "b".repeat(64);}public Result send(Envelope e){fail("Changed destination must never receive old event");return null;}};
    var changedWorker=new DefaultBillingNotificationDispatcher(em,manager,json,worker,changed);assertFalse(changedWorker.dispatchOne());assertEquals("DESTINATION_CHANGED",changedWorker.list("DEAD").getFirst().lastOutcome());
    try(var runtime=DriverManager.getConnection(url,role,"")){assertEquals(0,count(runtime,"SELECT count(*) FROM subscription_notification_delivery"));
     String migration=Files.readString(Path.of("../../db-migrations/src/main/resources/db/migration/V35__billing_notification_delivery.sql"));String preflight=migration.substring(0,migration.indexOf("ALTER TABLE subscription_billing_event"));
     assertEquals("42501",assertThrows(SQLException.class,()->sql(runtime,preflight)).getSQLState(),"Migration cannot silently backfill through an invisible source");}
    sql(owner,"UPDATE platform_operator SET active=false WHERE actor_id='"+worker+"'");assertThrows(AccessDeniedException.class,dispatcher::dispatchOne);
    assertEquals(0,count(owner,"SELECT count(*) FROM subscription_notice"));assertEquals(0,count(owner,"SELECT count(*) FROM subscription_manual_payment"));
   }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static SubscriptionBillingService.Issue invoice(UUID tenant){var today=LocalDate.now(ZoneOffset.UTC);return new SubscriptionBillingService.Issue(UUID.randomUUID(),tenant,null,"Sensitive synthetic issuer","TEST","Sensitive customer","","Synthetic notification","TEST",today,today,today,"10000","0","0");}
 static void as(UUID actor,String... authorities){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg","RS256").subject(actor.toString()).build(),Arrays.stream(authorities).map(s->new SimpleGrantedAuthority("SCOPE_"+s)).toList()));}
 static void sql(Connection c,String query)throws SQLException{try(var s=c.createStatement()){s.execute(query);}}
 static long count(Connection c,String query)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(query)){r.next();return r.getLong(1);}}
}
