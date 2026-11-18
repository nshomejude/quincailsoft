package com.quincaisoft.platformcontrol;

import com.quincaisoft.platformcontrol.api.*;
import com.quincaisoft.platformcontrol.application.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresOnboardingConfigurationTest {
 @Test void configurationIdentityReviewReplayIsolationAndRollback() throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String suffix=UUID.randomUUID().toString().replace("-",""),db="onboard_config_"+suffix,role="onboard_role_"+suffix;
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID maker=UUID.randomUUID(),checker=UUID.randomUUID(),ownerId=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")) {
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")) {
    try(var files=Files.list(Path.of("../db-migrations/src/main/resources/db/migration"))) {
     for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));
    }
    sql(owner,"INSERT INTO platform_operator VALUES('"+maker+"',true),('"+checker+"',true)");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setPackagesToScan("com.quincaisoft.platformcontrol.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var manager=new JpaTransactionManager(emf);
    var base=new DefaultPlatformService(em,manager);var calls=new AtomicInteger();
    // Test double exercises proof binding, not external IdP acceptance. Production uses the signature-validating adapter.
    OnboardingIdentityVerifier proof=token->{calls.incrementAndGet();if(token.equals("bad-proof"))throw new AccessDeniedException("Rejected proof");return new OnboardingIdentityVerifier.VerifiedOwner(ownerId,"owner@example.test","https://idp.example.test");};
    var service=new DefaultOnboardingConfigurationService(em,manager,proof);
    as(maker);var request=base.request(new PlatformService.Request(UUID.randomUUID(),"Hardware business","New tenant"));
    as(checker);base.approve(request.id(),new PlatformService.Review(UUID.randomUUID(),0));
    var profile=new OnboardingConfigurationService.Profile("Hardware SARL","REG-TEST","Douala",ownerId,"owner@example.test","CM","Africa/Douala","fr","HQ","EMPTY","privacy-v1","terms-v1","standard-trial");
    var configure=new OnboardingConfigurationService.Configure(UUID.randomUUID(),1,profile);
    as(maker,"platform:tenants:read");assertThrows(AccessDeniedException.class,()->service.configure(request.id(),configure));
    as(UUID.randomUUID());assertThrows(AccessDeniedException.class,()->service.configure(request.id(),configure));
    as(maker);assertThrows(IllegalStateException.class,()->service.configure(request.id(),new OnboardingConfigurationService.Configure(UUID.randomUUID(),0,profile)));
    var created=service.configure(request.id(),configure);assertEquals("AWAITING_IDENTITY",created.status());
    assertEquals(created,service.configure(request.id(),configure));
    assertThrows(IllegalStateException.class,()->service.approve(request.id(),new OnboardingConfigurationService.Approval(UUID.randomUUID(),0)));
    assertThrows(AccessDeniedException.class,()->service.verifyIdentity(request.id(),new OnboardingConfigurationService.VerifyIdentity(UUID.randomUUID(),0,"bad-proof")));
    assertEquals(1,count(owner,"select count(*) from platform_onboarding_command"));
    assertEquals(1,count(owner,"select count(*) from platform_onboarding_event"));
    var verify=new OnboardingConfigurationService.VerifyIdentity(UUID.randomUUID(),0,"sensitive-owner-token");
    var verified=service.verifyIdentity(request.id(),verify);assertEquals("IDENTITY_VERIFIED",verified.status());
    int beforeReplay=calls.get();assertEquals(verified,service.verifyIdentity(request.id(),verify));assertEquals(beforeReplay,calls.get());
    assertFalse(verify.toString().contains("sensitive-owner-token"));
    assertEquals(0,count(owner,"select count(*) from platform_onboarding_command where response_text like '%sensitive-owner-token%'"));
    assertThrows(AccessDeniedException.class,()->service.approve(request.id(),new OnboardingConfigurationService.Approval(UUID.randomUUID(),1)));
    as(checker);var approve=new OnboardingConfigurationService.Approval(UUID.randomUUID(),1);var approved=service.approve(request.id(),approve);
    assertEquals("APPROVED_CONFIGURATION",approved.status());assertEquals(approved,service.approve(request.id(),approve));
    assertEquals(3,count(owner,"select count(*) from platform_onboarding_event"));
    as(maker);assertEquals(created,service.configure(request.id(),configure),"Replay returns the original response, not mutable latest state");
    assertThrows(IllegalStateException.class,()->service.configure(request.id(),new OnboardingConfigurationService.Configure(configure.operationId(),0,profile)));
    assertThrows(IllegalStateException.class,()->service.approve(request.id(),approve));
    assertEquals(0,count(owner,"select count(*) from tenant"));
    assertEquals(0,count(owner,"select count(*) from tenant_memberships"));
    // New request; same-operation concurrent submissions converge on one original result.
    var next=base.request(new PlatformService.Request(UUID.randomUUID(),"Second business","Concurrency"));as(checker);base.approve(next.id(),new PlatformService.Review(UUID.randomUUID(),0));
    var nextConfigure=new OnboardingConfigurationService.Configure(UUID.randomUUID(),1,profile);
    try(var pool=Executors.newFixedThreadPool(4)) {
     var start=new CountDownLatch(1);var futures=new ArrayList<Future<OnboardingConfigurationService.Configuration>>();
     for(int i=0;i<4;i++)futures.add(pool.submit(()->{as(maker);try{start.await();return service.configure(next.id(),nextConfigure);}finally{SecurityContextHolder.clearContext();}}));
     start.countDown();var first=futures.getFirst().get(30,TimeUnit.SECONDS);for(var future:futures)assertEquals(first,future.get(30,TimeUnit.SECONDS));
    }
    assertEquals(2,count(owner,"select count(*) from platform_onboarding_configuration"));
    // A persistence failure rolls back state, journal and audit/outbox together.
    as(maker);var third=base.request(new PlatformService.Request(UUID.randomUUID(),"Rollback business","Rollback"));as(checker);base.approve(third.id(),new PlatformService.Review(UUID.randomUUID(),0));
    sql(owner,"CREATE FUNCTION reject_config_event_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'injected event failure'; END $$");
    sql(owner,"CREATE TRIGGER reject_config_event_test BEFORE INSERT ON platform_onboarding_event FOR EACH ROW EXECUTE FUNCTION reject_config_event_test()");
    as(maker);var thirdInput=new OnboardingConfigurationService.Configure(UUID.randomUUID(),1,profile);
    assertThrows(RuntimeException.class,()->service.configure(third.id(),thirdInput));
    assertEquals(0,count(owner,"select count(*) from platform_onboarding_configuration where request_id='"+third.id()+"'"));
    assertEquals(0,count(owner,"select count(*) from platform_onboarding_command where operation_id='"+thirdInput.operationId()+"'"));
    sql(owner,"DROP TRIGGER reject_config_event_test ON platform_onboarding_event");
    service.configure(third.id(),thirdInput);
    sql(owner,"UPDATE platform_operator SET active=false WHERE actor_id='"+maker+"'");
    assertThrows(AccessDeniedException.class,()->service.configure(request.id(),configure));
    try(var runtime=DriverManager.getConnection(url,role,"")){
     assertEquals(0,count(runtime,"select count(*) from platform_onboarding_configuration"));
     assertEquals(0,count(runtime,"select count(*) from platform_onboarding_command"));
     assertThrows(SQLException.class,()->sql(runtime,"DELETE FROM platform_onboarding_event"));
    }
   } finally {
    SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);
   }
  }
 }
 static void as(UUID actor,String... scopes) {
  String[] selected=scopes.length==0?new String[]{"platform:tenants:read","platform:tenants:write","platform:tenants:verify","platform:tenants:approve"}:scopes;
  var granted=Arrays.stream(selected).map(s->new SimpleGrantedAuthority("SCOPE_"+s)).toList();
  SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("operator-test").header("alg","RS256").subject(actor.toString()).build(),granted));
 }
 static void sql(Connection c,String q)throws SQLException{try(var s=c.createStatement()){s.execute(q);}}
 static long count(Connection c,String q)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(q)){r.next();return r.getLong(1);}}
}
