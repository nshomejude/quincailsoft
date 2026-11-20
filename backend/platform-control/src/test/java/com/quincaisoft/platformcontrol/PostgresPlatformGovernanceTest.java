package com.quincaisoft.platformcontrol;
import com.quincaisoft.platformcontrol.api.PlatformGovernanceService.*;
import com.quincaisoft.platformcontrol.application.*;import com.quincaisoft.shared.*;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;import org.springframework.orm.jpa.*;import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;import org.springframework.security.core.authority.SimpleGrantedAuthority;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.support.TransactionTemplate;import tools.jackson.databind.json.JsonMapper;
import java.sql.*;import java.nio.file.*;import java.util.*;import java.time.*;import java.util.concurrent.*;import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresPlatformGovernanceTest {
 @Test void consentReviewRedactionRevocationSuspensionFencingReplayAndLegalHolds()throws Exception{
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent"),suffix=UUID.randomUUID().toString().replace("-",""),db="governance_"+suffix,role="governance_role_"+suffix,url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID tenant=UUID.randomUUID(),company=UUID.randomUUID(),branch=UUID.randomUUID(),ownerId=UUID.randomUUID(),maker=UUID.randomUUID(),checker=UUID.randomUUID();var scope=new TenantScope(tenant,company,branch,ownerId);
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS;END IF;END $$");sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var dbOwner=DriverManager.getConnection(url,adminUser,"")){
    try(var files=Files.list(Path.of("../db-migrations/src/main/resources/db/migration"))){for(var f:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(dbOwner,Files.readString(f));}
    sql(dbOwner,"INSERT INTO tenant(id,name)VALUES('"+tenant+"','Governance test');INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+company+"','Company');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+branch+"','Branch');INSERT INTO tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code)VALUES('"+tenant+"','"+company+"','"+branch+"','"+ownerId+"','owner');INSERT INTO platform_operator VALUES('"+maker+"',true),('"+checker+"',true)");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var manager=new JpaTransactionManager(emf);
    ScopedTransactions merchant=new ScopedTransactions(){public <T>T execute(Supplier<T> work){return new TransactionTemplate(manager).execute(st->{var s=TenantContext.current();for(var p:Map.of("app.tenant_id",s.tenantId(),"app.company_id",s.companyId(),"app.branch_id",s.branchId(),"app.actor_id",s.actorId()).entrySet())em.createNativeQuery("select set_config(:k,:v,true)").setParameter("k",p.getKey()).setParameter("v",p.getValue().toString()).getSingleResult();return work.get();});}};
    var service=new DefaultPlatformGovernanceService(em,manager,merchant,JsonMapper.builder().build());var policy=new DefaultTenantLifecyclePolicy(em,merchant);
    auth(ownerId,null);var consent=new Consent(UUID.randomUUID(),maker,"TICKET-1","Investigate stock count",Instant.now().plusSeconds(3600));var grant=scoped(scope,()->service.consent(consent));assertEquals(grant,scoped(scope,()->service.consent(consent)));
    assertThrows(IllegalArgumentException.class,()->scoped(scope,()->service.consent(new Consent(UUID.randomUUID(),maker,"TICKET-2","Expired consent",Instant.now().minusSeconds(1)))));
    auth(maker,null);assertThrows(AccessDeniedException.class,()->service.approveGrant(grant.id(),command(0)));auth(checker,null);var approved=service.approveGrant(grant.id(),command(0));assertThrows(AccessDeniedException.class,()->service.diagnostics(grant.id(),UUID.randomUUID()));
    auth(maker,null);var diagnostic=service.diagnostics(grant.id(),UUID.randomUUID());assertEquals("0",diagnostic.productCount());assertEquals("0",diagnostic.postedSaleCount());assertEquals("0",diagnostic.stockPositionCount());
    assertEquals(1,count(dbOwner,"select count(*) from platform_governance_event where event_type='SupportDiagnosticsRead' and actor_id='"+maker+"'"));
    auth(ownerId,null);scoped(scope,()->service.revoke(grant.id(),command(Long.parseLong(approved.version()))));auth(maker,null);assertThrows(AccessDeniedException.class,()->service.diagnostics(grant.id(),UUID.randomUUID()));
    var holdRequest=service.request(tenant,request(Kind.PLACE_LEGAL_HOLD,0,null));auth(checker,null);service.approve(holdRequest.id(),command(0));assertEquals("ACTIVE",service.holds(tenant).getFirst().status());
    auth(maker,null);var suspension=service.request(tenant,request(Kind.SUSPEND,0,null));assertThrows(AccessDeniedException.class,()->service.approve(suspension.id(),command(0)));
    // An admitted mutation holds the fence until transaction commit; review cannot overtake it.
    try(var pool=Executors.newFixedThreadPool(2)){
     var held=new CountDownLatch(1);var release=new CountDownLatch(1);var entered=new CountDownLatch(1);
     var mutation=pool.submit(()->{auth(ownerId,null);try{return scoped(scope,()->merchant.execute(()->{policy.requireNewMutation();held.countDown();try{if(!release.await(15,TimeUnit.SECONDS))throw new AssertionError("Fence timeout");}catch(InterruptedException e){throw new RuntimeException(e);}return true;}));}finally{SecurityContextHolder.clearContext();}});
     assertTrue(held.await(10,TimeUnit.SECONDS));var decision=command(0);var review=pool.submit(()->{auth(checker,null);entered.countDown();try{return service.approve(suspension.id(),decision);}finally{SecurityContextHolder.clearContext();}});
     assertTrue(entered.await(10,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->review.get(200,TimeUnit.MILLISECONDS));release.countDown();assertTrue(mutation.get(10,TimeUnit.SECONDS));var applied=review.get(10,TimeUnit.SECONDS);auth(checker,null);assertEquals(applied,service.approve(suspension.id(),decision));
    }
    auth(ownerId,null);assertThrows(AccessDeniedException.class,()->scoped(scope,()->{policy.requireNewMutation();return null;}));assertEquals("SUSPENDED",scoped(scope,policy::current).status());assertEquals(1,scoped(scope,()->policy.requireSuspended(suspension.id(),1)).epoch());assertThrows(IllegalStateException.class,()->scoped(scope,()->policy.requireSuspended(UUID.randomUUID(),1)));
    auth(maker,null);var resume=service.request(tenant,request(Kind.RESUME,1,null));auth(checker,null);
    sql(dbOwner,"CREATE FUNCTION inject_governance_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type='TenantResumed' THEN RAISE EXCEPTION 'test rollback';END IF;RETURN NEW;END $$;CREATE TRIGGER inject_governance_failure BEFORE INSERT ON platform_governance_event FOR EACH ROW EXECUTE FUNCTION inject_governance_failure()");
    var resumeCommand=command(0);assertThrows(RuntimeException.class,()->service.approve(resume.id(),resumeCommand));assertEquals("SUSPENDED",service.tenantState(tenant).status());sql(dbOwner,"DROP TRIGGER inject_governance_failure ON platform_governance_event");service.approve(resume.id(),resumeCommand);assertEquals(2,service.tenantState(tenant).epoch());
    auth(ownerId,null);assertThrows(AccessDeniedException.class,()->scoped(scope,()->{policy.requireNewMutation();return null;}));auth(ownerId,1L);assertThrows(AccessDeniedException.class,()->scoped(scope,()->{policy.requireNewMutation();return null;}));auth(ownerId,2L);scoped(scope,()->{policy.requireNewMutation();return null;});assertThrows(IllegalStateException.class,()->scoped(scope,()->policy.requireSuspended(suspension.id(),1)));
    auth(maker,null);var releaseHold=service.request(tenant,request(Kind.RELEASE_LEGAL_HOLD,0,holdRequest.id()));auth(checker,null);service.approve(releaseHold.id(),command(0));assertEquals("RELEASED",service.holds(tenant).getFirst().status());
    try(var runtime=DriverManager.getConnection(url,role,"")){assertEquals(0,count(runtime,"select count(*) from platform_legal_hold"));assertThrows(SQLException.class,()->sql(runtime,"delete from platform_legal_hold"));}
    sql(dbOwner,"update platform_operator set active=false where actor_id='"+checker+"'");assertThrows(AccessDeniedException.class,()->service.tenantState(tenant));
   }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static Command command(long version){return new Command(UUID.randomUUID(),version,"Reviewed evidence independently");}
 static Request request(Kind kind,long version,UUID hold){return new Request(UUID.randomUUID(),kind,version,hold,"CASE-1","Documented operational security review");}
 static <T>T scoped(TenantScope scope,Supplier<T> work){try{return TenantContext.call(scope,1,work::get);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
 static void auth(UUID actor,Long epoch){var jwt=Jwt.withTokenValue("test").header("alg","RS256").subject(actor.toString());if(epoch!=null)jwt.claim("tenant_epoch",epoch);SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.build(),List.of("support:read","support:write","platform:support:read","platform:support:approve","platform:governance:read","platform:governance:write","platform:governance:approve").stream().map(x->new SimpleGrantedAuthority("SCOPE_"+x)).toList()));}
 static void sql(Connection c,String q)throws SQLException{try(var s=c.createStatement()){s.execute(q);}}
 static long count(Connection c,String q)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(q)){r.next();return r.getLong(1);}}
}
