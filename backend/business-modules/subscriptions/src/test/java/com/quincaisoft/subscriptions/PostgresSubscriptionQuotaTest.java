package com.quincaisoft.subscriptions;
import com.quincaisoft.subscriptions.api.SubscriptionQuotaService;
import com.quincaisoft.subscriptions.application.DefaultSubscriptionQuotaService;
import com.quincaisoft.shared.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;
import java.sql.*;import java.nio.file.*;import java.util.*;import java.time.*;import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresSubscriptionQuotaTest {
 @Test void authoritativeDistinctSeatsReservationRaceConsumptionReleaseAndLegacy()throws Exception{
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String suffix=UUID.randomUUID().toString().replace("-",""),db="quota_test_"+suffix,role="quota_role_"+suffix;String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID tenant=UUID.randomUUID(),company=UUID.randomUUID(),branch=UUID.randomUUID(),branch2=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(),d=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS;END IF;END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")){
    try(var files=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));}
    seed(owner,tenant,company,branch,a,b,c,d);
    sql(owner,"insert into branch(tenant_id,company_id,id,name)values('"+tenant+"','"+company+"','"+branch2+"','Other branch')");
    sql(owner,"insert into tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code)values('"+tenant+"','"+company+"','"+branch2+"','"+a+"','owner')");
    sql(owner,"insert into subscription_plan values('quota-test','Quota test',0,3,2,1,false,'2024-01-01T00:00:00Z')");
    sql(owner,"insert into tenant_subscription(tenant_id,plan_code,status,trial_ends_at,version,created_at,updated_at)values('"+tenant+"','quota-test','TRIAL',now()+interval '14 days',0,now(),now())");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.subscriptions.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var manager=new JpaTransactionManager(emf);var template=new TransactionTemplate(manager);
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(java.util.function.Supplier<T> work){return template.execute(st->{var s=TenantContext.current();for(var pair:Map.of("app.tenant_id",s.tenantId(),"app.company_id",s.companyId(),"app.branch_id",s.branchId(),"app.actor_id",s.actorId()).entrySet())em.createNativeQuery("select set_config(:k,:v,true)").setParameter("k",pair.getKey()).setParameter("v",pair.getValue().toString()).getSingleResult();Number n=(Number)em.createNativeQuery("select count(*) from tenant_memberships where actor_id=:a and active and authorization_epoch=:e").setParameter("a",s.actorId()).setParameter("e",TenantContext.authorizationEpoch()).getSingleResult();if(n.longValue()!=1)throw new AccessDeniedException("Current membership required");return work.get();});}};
    var service=new DefaultSubscriptionQuotaService(em,tx,JsonMapper.builder().build());var scopeA=new TenantScope(tenant,company,branch,a);var scopeB=new TenantScope(tenant,company,branch,b);
    var usage=call(scopeA,()->service.usage());assertEquals("2",usage.getFirst().used(),"One login across two branches still consumes one seat");
    assertEquals("2",usage.stream().filter(x->x.resource()==SubscriptionQuotaService.Resource.BRANCHES).findFirst().orElseThrow().used(),"Counts include hidden other branch");
    var reserve=new SubscriptionQuotaService.Reserve(UUID.randomUUID(),SubscriptionQuotaService.Resource.STAFF_SEATS,c);
    var held=call(scopeA,()->service.reserve(reserve));assertEquals("RESERVED",held.status());assertEquals(held,call(scopeA,()->service.reserve(reserve)));
    assertThrows(IllegalStateException.class,()->call(scopeA,()->service.reserve(new SubscriptionQuotaService.Reserve(reserve.operationId(),reserve.resource(),d))));
    assertThrows(RuntimeException.class,()->call(scopeA,()->service.reserve(new SubscriptionQuotaService.Reserve(UUID.randomUUID(),reserve.resource(),d))));
    assertThrows(IllegalStateException.class,()->call(scopeA,()->service.committed(held.id())));
    // Even a runtime SQL caller cannot fake consumption using the transaction setting.
    assertThrows(RuntimeException.class,()->call(scopeA,()->tx.execute(()->{em.createNativeQuery("select set_config('app.quota_consuming',:id,true)").setParameter("id",held.id().toString()).getSingleResult();em.createNativeQuery("update subscription_quota_reservation set status='COMMITTED' where tenant_id=:t and id=:i").setParameter("t",tenant).setParameter("i",held.id()).executeUpdate();return null;})));
    UUID release=UUID.randomUUID();var released=call(scopeA,()->service.release(release,held.id()));assertEquals("RELEASED",released.status());assertEquals(released,call(scopeA,()->service.release(release,held.id())));
    assertEquals(held,call(scopeA,()->service.reserve(reserve)),"Original reserve response remains stable after release");
    SubscriptionQuotaService.Reservation winner;
    try(var pool=Executors.newFixedThreadPool(2)){
     var start=new CountDownLatch(1);var tasks=new ArrayList<Future<SubscriptionQuotaService.Reservation>>();
     for(UUID target:List.of(c,d))tasks.add(pool.submit(()->{start.await();try{return call(scopeA,()->service.reserve(new SubscriptionQuotaService.Reserve(UUID.randomUUID(),SubscriptionQuotaService.Resource.STAFF_SEATS,target)));}catch(RuntimeException full){return null;}}));
     start.countDown();var results=new ArrayList<SubscriptionQuotaService.Reservation>();for(var task:tasks){var r=task.get(30,TimeUnit.SECONDS);if(r!=null)results.add(r);}assertEquals(1,results.size(),"Exactly one last-seat reservation wins");winner=results.getFirst();
    }
    UUID change=UUID.randomUUID(),approval=UUID.randomUUID();
    call(scopeA,()->tx.execute(()->{em.createNativeQuery("insert into identity_change(tenant_id,company_id,branch_id,id,operation_id,target_actor,requested_by,desired_role,desired_active,expected_epoch,reason)values(:t,:c,:b,:i,:o,:target,:a,'cashier',true,1,'Activate staff login')").setParameter("t",tenant).setParameter("c",company).setParameter("b",branch).setParameter("i",change).setParameter("o",UUID.randomUUID()).setParameter("target",winner.subjectId()).setParameter("a",a).executeUpdate();return null;}));
    call(scopeB,()->tx.execute(()->{
     service.bind(winner.id());em.createNativeQuery("update identity_change set approved_by=:a,approval_operation=:o where tenant_id=:t and company_id=:c and id=:i").setParameter("a",b).setParameter("o",approval).setParameter("t",tenant).setParameter("c",company).setParameter("i",change).executeUpdate();
     em.createNativeQuery("select set_config('app.identity_change_id',:id,true)").setParameter("id",change.toString()).getSingleResult();
     em.createNativeQuery("update tenant_memberships set active=true,authorization_epoch=2 where tenant_id=:t and company_id=:c and branch_id=:b and actor_id=:a").setParameter("t",tenant).setParameter("c",company).setParameter("b",branch).setParameter("a",winner.subjectId()).executeUpdate();
     em.createNativeQuery("update identity_change set status='APPLIED' where tenant_id=:t and company_id=:c and id=:i").setParameter("t",tenant).setParameter("c",company).setParameter("i",change).executeUpdate();
     assertEquals("COMMITTED",service.committed(winner.id()).status());return null;
    }));
    assertEquals("3",call(scopeA,()->service.usage()).getFirst().used());assertEquals("0",call(scopeA,()->service.usage()).getFirst().reserved());
    assertThrows(IllegalStateException.class,()->call(scopeA,()->service.release(UUID.randomUUID(),winner.id())));
    assertThrows(IllegalStateException.class,()->call(scopeA,()->{service.requireCapacity(SubscriptionQuotaService.Resource.STAFF_SEATS,1);return null;}));
    // Payroll employee creation must not be counted as an application login seat (no source trigger exists).
    assertEquals(0,count(owner,"select count(*) from pg_trigger where not tgisinternal and tgname like 'quota_%' and tgrelid='workforce_employee'::regclass"));
    // Legacy/perpetual tenant without SaaS contract is explicit and source behavior remains authorized/unmetered.
    UUID lt=UUID.randomUUID(),lc=UUID.randomUUID(),lb=UUID.randomUUID(),la=UUID.randomUUID(),lother=UUID.randomUUID();seed(owner,lt,lc,lb,la,lother,UUID.randomUUID(),UUID.randomUUID());
    var legacyScope=new TenantScope(lt,lc,lb,la);assertEquals("NO_SAAS_CONTRACT",call(legacyScope,()->service.usage()).getFirst().licenseMode());
    call(legacyScope,()->{service.requireCapacity(SubscriptionQuotaService.Resource.STAFF_SEATS,100);return null;});
    assertThrows(IllegalStateException.class,()->call(legacyScope,()->service.reserve(new SubscriptionQuotaService.Reserve(UUID.randomUUID(),SubscriptionQuotaService.Resource.STAFF_SEATS,UUID.randomUUID()))));
    assertThrows(NoSuchElementException.class,()->call(legacyScope,()->service.get(winner.id())));
    assertThrows(NoSuchElementException.class,()->call(new TenantScope(tenant,company,branch2,a),()->service.get(winner.id())));
    try(var runtime=DriverManager.getConnection(url,role,"")){assertEquals(0,count(runtime,"select count(*) from subscription_quota_state"));assertThrows(SQLException.class,()->sql(runtime,"update subscription_quota_state set used=0"));}
   }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static <T>T call(TenantScope scope,java.util.concurrent.Callable<T> work)throws Exception{as(scope.actorId());try{return TenantContext.call(scope,1,work);}finally{SecurityContextHolder.clearContext();}}
 static void as(UUID actor){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg","RS256").subject(actor.toString()).build(),List.of(new SimpleGrantedAuthority("SCOPE_billing:read"),new SimpleGrantedAuthority("SCOPE_billing:write"))));}
 static void seed(Connection c,UUID t,UUID company,UUID branch,UUID a,UUID b,UUID inactive1,UUID inactive2)throws SQLException{
  sql(c,"insert into tenant(id,name)values('"+t+"','Quota fixture');insert into company(tenant_id,id,name)values('"+t+"','"+company+"','Company');insert into branch(tenant_id,company_id,id,name)values('"+t+"','"+company+"','"+branch+"','Branch')");
  sql(c,"insert into tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code,active)values('"+t+"','"+company+"','"+branch+"','"+a+"','owner',true),('"+t+"','"+company+"','"+branch+"','"+b+"','owner',true),('"+t+"','"+company+"','"+branch+"','"+inactive1+"','cashier',false),('"+t+"','"+company+"','"+branch+"','"+inactive2+"','cashier',false)");
 }
 static void sql(Connection c,String q)throws SQLException{try(var s=c.createStatement()){s.execute(q);}}
 static long count(Connection c,String q)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(q)){r.next();return r.getLong(1);}}
}
