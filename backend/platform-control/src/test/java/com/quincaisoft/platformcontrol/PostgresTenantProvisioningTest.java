package com.quincaisoft.platformcontrol;
import com.quincaisoft.platformcontrol.api.*;
import com.quincaisoft.platformcontrol.application.*;
import com.quincaisoft.company.application.DefaultCompanyBootstrapService;
import com.quincaisoft.branch.application.DefaultBranchBootstrapService;
import com.quincaisoft.inventory.application.DefaultInventoryBootstrapService;
import com.quincaisoft.identity.application.DefaultIdentityBootstrapService;
import com.quincaisoft.documents.application.DefaultDocumentsBootstrapService;
import com.quincaisoft.finance.application.DefaultFinanceBootstrapService;
import com.quincaisoft.subscriptions.application.DefaultSubscriptionLifecycleService;
import com.quincaisoft.shared.*;
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
import tools.jackson.databind.json.JsonMapper;
import java.sql.*;import java.nio.file.*;import java.util.*;import java.time.*;import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresTenantProvisioningTest {
 @Test void atomicTrialBootstrapReplayConcurrencyIsolationAndFailureResume()throws Exception{
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String suffix=UUID.randomUUID().toString().replace("-",""),db="tenant_boot_"+suffix,role="tenant_boot_role_"+suffix;
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID maker=UUID.randomUUID(),checker=UUID.randomUUID(),ownerId=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS;END IF;END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")){
    try(var files=Files.list(Path.of("../db-migrations/src/main/resources/db/migration"))){for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));}
    sql(owner,"INSERT INTO platform_operator VALUES('"+maker+"',true),('"+checker+"',true)");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setPackagesToScan("com.quincaisoft");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var manager=new JpaTransactionManager(emf);
    var review=new DefaultPlatformService(em,manager);
    var configurations=new DefaultOnboardingConfigurationService(em,manager,token->new OnboardingIdentityVerifier.VerifiedOwner(ownerId,"owner@example.test","https://idp.example.test"));
    ScopedTransactions noMerchant=new ScopedTransactions(){public <T>T execute(java.util.function.Supplier<T> work){throw new AssertionError("Bootstrap must not impersonate merchant membership");}};
    var billing=new DefaultSubscriptionLifecycleService(em,manager,noMerchant,JsonMapper.builder().build());
    var provision=new DefaultTenantProvisioningService(em,manager,new DefaultCompanyBootstrapService(em),new DefaultBranchBootstrapService(em),new DefaultInventoryBootstrapService(em),new DefaultIdentityBootstrapService(em),new DefaultDocumentsBootstrapService(em),new DefaultFinanceBootstrapService(em),billing);
    UUID request=approved(review,configurations,maker,checker,ownerId,"EMPTY");
    var input=new TenantProvisioningService.Trial(UUID.randomUUID(),2,"Douala HQ","Main stock",LocalDate.now());
    as(maker,"platform:tenants:read");assertThrows(AccessDeniedException.class,()->provision.provisionTrial(request,UUID.randomUUID(),input));
    as(UUID.randomUUID());assertThrows(AccessDeniedException.class,()->provision.provisionTrial(request,UUID.randomUUID(),input));
    as(maker);assertThrows(IllegalStateException.class,()->provision.provisionTrial(request,UUID.randomUUID(),new TenantProvisioningService.Trial(UUID.randomUUID(),1,"Douala HQ","Main stock",LocalDate.now())));
    var activated=provision.provisionTrial(request,UUID.randomUUID(),input);assertEquals("ACTIVE",activated.status());assertEquals(9,activated.completedSteps().size());
    assertEquals(activated,provision.provisionTrial(request,UUID.randomUUID(),input));
    assertEquals(1,count(owner,"select count(*) from tenant"));assertEquals(1,count(owner,"select count(*) from company where branch_count=1"));
    assertEquals(1,count(owner,"select count(*) from warehouse where active"));
    assertEquals(1,count(owner,"select count(*) from tenant_memberships where active and role_code='owner' and authorization_epoch=1 and actor_id='"+ownerId+"'"));
    assertEquals(3,count(owner,"select count(*) from document_series where next_number=1"));
    assertEquals(1,count(owner,"select count(*) from document_settings where locale='fr'"));
    assertEquals(22,count(owner,"select count(*) from finance_bootstrap_mapping where review_status='PROVISIONAL'"));
    assertEquals(0,count(owner,"select count(*) from journal_entry"));assertEquals(0,count(owner,"select count(*) from stock_movement"));
    assertEquals(1,count(owner,"select count(*) from tenant_subscription where status='TRIAL'"));
    ScopedTransactions merchant=new ScopedTransactions(){public <T>T execute(java.util.function.Supplier<T> work){return new org.springframework.transaction.support.TransactionTemplate(manager).execute(st->{var scope=TenantContext.current();for(var pair:Map.of("app.tenant_id",scope.tenantId(),"app.company_id",scope.companyId(),"app.branch_id",scope.branchId(),"app.actor_id",scope.actorId()).entrySet())em.createNativeQuery("select set_config(:k,:v,true)").setParameter("k",pair.getKey()).setParameter("v",pair.getValue().toString()).getSingleResult();Number n=(Number)em.createNativeQuery("select count(*) from tenant_memberships where actor_id=:a and active and authorization_epoch=1").setParameter("a",scope.actorId()).getSingleResult();if(n.longValue()!=1)throw new AccessDeniedException("Membership required");return work.get();});}};
    var periods=new com.quincaisoft.finance.application.DefaultPeriodService(em,merchant,new com.quincaisoft.company.application.DefaultCompanyService(em,merchant),List.of(),JsonMapper.builder().build());
    TenantContext.call(new TenantScope(activated.tenantId(),activated.companyId(),activated.branchId(),ownerId),1,()->periods.create(new com.quincaisoft.finance.api.PeriodService.Create(UUID.randomUUID(),input.businessDate().plusYears(1).withDayOfYear(1),input.businessDate().plusYears(1).withMonth(12).withDayOfMonth(31),"Next fiscal year")));
    assertEquals(2,count(owner,"select count(*) from financial_period"),"Existing merchant period creation remains supported");

    assertThrows(IllegalStateException.class,()->provision.provisionTrial(request,UUID.randomUUID(),new TenantProvisioningService.Trial(input.operationId(),2,"Changed","Main stock",LocalDate.now())));
    // Merchant SQL scope cannot use bootstrap grants to create another tenant or arbitrary owner.
    try(var runtime=DriverManager.getConnection(url,role,"")){
     assertEquals(0,count(runtime,"select count(*) from tenant"));
     runtime.setAutoCommit(false);sql(runtime,"select set_config('app.tenant_id','"+activated.tenantId()+"',true)");sql(runtime,"select set_config('app.company_id','"+activated.companyId()+"',true)");sql(runtime,"select set_config('app.branch_id','"+activated.branchId()+"',true)");
     assertEquals(1,count(runtime,"select count(*) from tenant"));
     assertThrows(SQLException.class,()->sql(runtime,"insert into tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code)values('"+activated.tenantId()+"','"+activated.companyId()+"','"+activated.branchId()+"','"+UUID.randomUUID()+"','owner')"));runtime.rollback();
     sql(runtime,"select set_config('app.tenant_id','"+UUID.randomUUID()+"',true)");assertThrows(SQLException.class,()->sql(runtime,"insert into tenant(id,name)values(app_tenant(),'rogue')"));runtime.rollback();
    }
    // Force failure after all resources are staged; no membership or tenant may commit.
    UUID failedRequest=approved(review,configurations,maker,checker,ownerId,"EMPTY");as(maker);
    sql(owner,"CREATE FUNCTION inject_trial_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type='TenantTrialActivated.v1' THEN RAISE EXCEPTION 'injected bootstrap failure';END IF;RETURN NEW;END $$");
    sql(owner,"CREATE TRIGGER inject_trial_failure BEFORE INSERT ON platform_provisioning_event FOR EACH ROW EXECUTE FUNCTION inject_trial_failure()");
    var failedInput=new TenantProvisioningService.Trial(UUID.randomUUID(),2,"Buea","Buea stock",LocalDate.now());
    assertThrows(RuntimeException.class,()->provision.provisionTrial(failedRequest,UUID.randomUUID(),failedInput));
    var failed=provision.get(failedRequest);assertEquals("FAILED",failed.status());assertTrue(failed.completedSteps().isEmpty());
    assertEquals(0,count(owner,"select count(*) from tenant where id='"+failed.tenantId()+"'"));
    assertEquals(0,count(owner,"select count(*) from tenant_memberships where tenant_id='"+failed.tenantId()+"'"));
    sql(owner,"DROP TRIGGER inject_trial_failure ON platform_provisioning_event");
    try(var pool=Executors.newFixedThreadPool(4)){
     var start=new CountDownLatch(1);var futures=new ArrayList<Future<TenantProvisioningService.Result>>();
     for(int i=0;i<4;i++)futures.add(pool.submit(()->{as(maker);try{start.await();return provision.provisionTrial(failedRequest,UUID.randomUUID(),failedInput);}finally{SecurityContextHolder.clearContext();}}));
     start.countDown();var first=futures.getFirst().get(45,TimeUnit.SECONDS);assertEquals(failed.tenantId(),first.tenantId());for(var future:futures)assertEquals(first,future.get(45,TimeUnit.SECONDS));
    }
    assertEquals(2,count(owner,"select count(*) from tenant"));assertEquals(2,count(owner,"select count(*) from platform_provisioning_event where event_type='TenantTrialActivated.v1'"));
    UUID importing=approved(review,configurations,maker,checker,ownerId,"CSV");as(maker);
    assertThrows(IllegalStateException.class,()->provision.provisionTrial(importing,UUID.randomUUID(),new TenantProvisioningService.Trial(UUID.randomUUID(),2,"Import branch","Import stock",LocalDate.now())));
    assertEquals(0,count(owner,"select count(*) from platform_provisioning where request_id='"+importing+"'"));
    sql(owner,"update platform_operator set active=false where actor_id='"+maker+"'");assertThrows(AccessDeniedException.class,()->provision.provisionTrial(request,UUID.randomUUID(),input));
   }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static UUID approved(DefaultPlatformService review,DefaultOnboardingConfigurationService configs,UUID maker,UUID checker,UUID owner,String mode){
  as(maker);var req=review.request(new PlatformService.Request(UUID.randomUUID(),"Tenant trial","Owner approved onboarding"));as(checker);review.approve(req.id(),new PlatformService.Review(UUID.randomUUID(),0));
  as(maker);configs.configure(req.id(),new OnboardingConfigurationService.Configure(UUID.randomUUID(),1,new OnboardingConfigurationService.Profile("Hardware SARL","REG-TEST","Douala",owner,"owner@example.test","CM","Africa/Douala","fr","HQ",mode,"privacy-v1","terms-v1","standard-trial")));
  configs.verifyIdentity(req.id(),new OnboardingConfigurationService.VerifyIdentity(UUID.randomUUID(),0,"test-proof"));as(checker);configs.approve(req.id(),new OnboardingConfigurationService.Approval(UUID.randomUUID(),1));return req.id();
 }
 static void as(UUID actor,String... scopes){String[] selected=scopes.length==0?new String[]{"platform:tenants:read","platform:tenants:write","platform:tenants:verify","platform:tenants:approve","platform:tenants:provision"}:scopes;SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg","RS256").subject(actor.toString()).build(),Arrays.stream(selected).map(s->new SimpleGrantedAuthority("SCOPE_"+s)).toList()));}
 static void sql(Connection c,String q)throws SQLException{try(var s=c.createStatement()){s.execute(q);}}
 static long count(Connection c,String q)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(q)){r.next();return r.getLong(1);}}
}
