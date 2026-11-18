package com.quincaisoft.subscriptions;

import com.quincaisoft.subscriptions.api.*;
import com.quincaisoft.subscriptions.application.*;
import com.quincaisoft.shared.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.sql.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static com.quincaisoft.subscriptions.PostgresSubscriptionBillingTest.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresSubscriptionLifecycleTest {
 @Test void trialReviewGraceAndManualPaymentRemainAuditedAndSeparate()throws Exception{
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String db="lifecycle_"+UUID.randomUUID().toString().replace("-",""),role="lifecycle_role_"+UUID.randomUUID().toString().replace("-","");
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID maker=UUID.randomUUID(),checker=UUID.randomUUID(),tenant=UUID.randomUUID(),otherTenant=UUID.randomUUID(),company=UUID.randomUUID(),branch=UUID.randomUUID(),merchant=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")){
    try(var files=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));}
    sql(owner,"INSERT INTO platform_operator VALUES('"+maker+"',true),('"+checker+"',true)");
    sql(owner,"INSERT INTO tenant(id,name)VALUES('"+tenant+"','Synthetic lifecycle'),('"+otherTenant+"','Legacy synthetic')");
    sql(owner,"INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+company+"','Synthetic')");
    sql(owner,"INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+branch+"','Synthetic')");
    sql(owner,"INSERT INTO tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code)VALUES('"+tenant+"','"+company+"','"+branch+"','"+merchant+"','owner')");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.subscriptions.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var manager=new JpaTransactionManager(emf);
    ScopedTransactions scoped=new ScopedTransactions(){public <T>T execute(Supplier<T> work){return new TransactionTemplate(manager).execute(status->{var s=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",s.tenantId().toString()).setParameter("c",s.companyId().toString()).setParameter("b",s.branchId().toString()).setParameter("a",s.actorId().toString()).getSingleResult();return work.get();});}};
    var lifecycle=new DefaultSubscriptionLifecycleService(em,manager,scoped,new JsonMapper());var invoices=new DefaultSubscriptionBillingService(em,manager,scoped,new JsonMapper());
    Instant ends=Instant.now().plusSeconds(86400*14).truncatedTo(java.time.temporal.ChronoUnit.MICROS);UUID bootstrap=UUID.randomUUID();
    as(maker,"platform:tenants:provision");var trial=lifecycle.bootstrapTrial(bootstrap,tenant,"standard-trial",ends);assertEquals("TRIAL",trial.status());assertTrue(trial.mobileAccess());assertEquals(trial,lifecycle.bootstrapTrial(bootstrap,tenant,"standard-trial",ends));
    assertThrows(IllegalStateException.class,()->lifecycle.bootstrapTrial(bootstrap,tenant,"standard-trial",ends.plusSeconds(1)));
    var scope=new TenantScope(tenant,company,branch,merchant);
    as(merchant,"billing:write");TenantContext.call(scope,1,()->{lifecycle.requireAllowed(SubscriptionLifecycleService.Operation.NEW_BUSINESS_POSTING);lifecycle.requireAllocation(SubscriptionLifecycleService.Resource.STAFF_SEATS,999,4);assertThrows(IllegalStateException.class,()->lifecycle.requireAllocation(SubscriptionLifecycleService.Resource.STAFF_SEATS,0,5));return null;});
    as(maker,"platform:billing:write","platform:billing:read");UUID cancellation=UUID.randomUUID();var cancelled=lifecycle.cancel(cancellation,tenant,0,"Synthetic merchant cancellation");assertEquals(1,cancelled.version());assertEquals(cancelled,lifecycle.cancel(cancellation,tenant,0,"Synthetic merchant cancellation"));
    assertThrows(IllegalStateException.class,()->lifecycle.cancel(UUID.randomUUID(),tenant,0,"Stale cancellation"));
    Instant delivered=Instant.now().minusSeconds(86400*7),grace=Instant.now().minusSeconds(3600);
    var noticeInput=new SubscriptionLifecycleService.Notice(UUID.randomUUID(),tenant,1,"synthetic-delivery-evidence",delivered,grace,"synthetic-reviewed-contract","Synthetic test only");
    var notice=lifecycle.recordNotice(noticeInput);assertEquals(notice,lifecycle.recordNotice(noticeInput));
    TenantContext.call(scope,1,()->{lifecycle.requireAllowed(SubscriptionLifecycleService.Operation.NEW_BUSINESS_POSTING);return null;});
    as(maker,"platform:billing:approve");assertThrows(AccessDeniedException.class,()->lifecycle.approveNotice(UUID.randomUUID(),notice.id()));
    as(checker,"platform:billing:approve");UUID approval=UUID.randomUUID();assertEquals("APPROVED",lifecycle.approveNotice(approval,notice.id()).status());assertEquals("APPROVED",lifecycle.approveNotice(approval,notice.id()).status());
    TenantContext.call(scope,1,()->{assertThrows(AccessDeniedException.class,()->lifecycle.requireAllowed(SubscriptionLifecycleService.Operation.NEW_BUSINESS_POSTING));for(var operation:SubscriptionLifecycleService.Operation.values())if(operation!=SubscriptionLifecycleService.Operation.NEW_BUSINESS_POSTING)lifecycle.requireAllowed(operation);return null;});
    // An uncontracted legacy tenant is never silently billed or denied by this policy.
    TenantContext.call(new TenantScope(otherTenant,company,branch,merchant),1,()->{lifecycle.requireAllowed(SubscriptionLifecycleService.Operation.NEW_BUSINESS_POSTING);return null;});
    as(maker,"platform:billing:write","platform:billing:read");var plan=new SubscriptionLifecycleService.Plan("synthetic-paid","Synthetic growth","10000",25,5,10,true,Instant.now().minusSeconds(1));UUID planOp=UUID.randomUUID();assertEquals(lifecycle.createPlan(planOp,plan),lifecycle.createPlan(planOp,plan));
    LocalDate today=LocalDate.now(ZoneOffset.UTC);var issue=new SubscriptionBillingService.Issue(UUID.randomUUID(),tenant,null,"Synthetic Opesware","TEST-TAX","Synthetic company","","Synthetic subscription month","TEST",today.withDayOfMonth(1),today.withDayOfMonth(today.lengthOfMonth()),today,"10000","0","0");var invoice=invoices.issue(UUID.randomUUID(),issue);
    var first=new SubscriptionLifecycleService.Payment(UUID.randomUUID(),tenant,invoice.id(),"synthetic-paid","4000","Synthetic bank / reference A","a".repeat(64),"Synthetic evidence reviewed manually");var p=lifecycle.recordPayment(first);assertEquals(p,lifecycle.recordPayment(first));
    assertThrows(IllegalStateException.class,()->lifecycle.printReceipt(p.id(),true,"en"));
    var cheapIssue=new SubscriptionBillingService.Issue(UUID.randomUUID(),tenant,null,"Synthetic Opesware","TEST-TAX","Synthetic company","","Underpriced synthetic invoice","TEST",today.withDayOfMonth(1),today.withDayOfMonth(today.lengthOfMonth()),today,"1","0","0");var cheap=invoices.issue(UUID.randomUUID(),cheapIssue);
    assertThrows(RuntimeException.class,()->lifecycle.recordPayment(new SubscriptionLifecycleService.Payment(UUID.randomUUID(),tenant,cheap.id(),"synthetic-paid","1","Synthetic underpriced","d".repeat(64),"Must not buy upgraded plan")));
    assertThrows(RuntimeException.class,()->lifecycle.recordPayment(new SubscriptionLifecycleService.Payment(UUID.randomUUID(),tenant,invoice.id(),"synthetic-paid","7000","Synthetic excess","b".repeat(64),"Synthetic excess")));
    assertThrows(RuntimeException.class,()->lifecycle.recordPayment(new SubscriptionLifecycleService.Payment(UUID.randomUUID(),tenant,invoice.id(),"synthetic-paid","1000",first.bankReference(),"b".repeat(64),"Duplicate bank reference")));
    as(maker,"platform:billing:approve");assertThrows(AccessDeniedException.class,()->lifecycle.approvePayment(UUID.randomUUID(),p.id()));
    as(checker,"platform:billing:approve","platform:billing:read");assertEquals("MANUALLY_RECONCILED",lifecycle.approvePayment(UUID.randomUUID(),p.id()).status());assertEquals("CANCELLED",lifecycle.verifyTrial(tenant).status());
    as(maker,"platform:billing:write");var finalPayment=lifecycle.recordPayment(new SubscriptionLifecycleService.Payment(UUID.randomUUID(),tenant,invoice.id(),"synthetic-paid","6000","Synthetic bank / reference B","c".repeat(64),"Synthetic remainder"));
    as(checker,"platform:billing:approve","platform:billing:read");UUID finalApproval=UUID.randomUUID();assertEquals("MANUALLY_RECONCILED",lifecycle.approvePayment(finalApproval,finalPayment.id()).status());assertEquals("MANUALLY_RECONCILED",lifecycle.approvePayment(finalApproval,finalPayment.id()).status());
    var active=lifecycle.verifyTrial(tenant);assertEquals("ACTIVE",active.status());assertEquals("synthetic-paid",active.planCode());assertEquals(2,active.version());assertTrue(active.commerceAccess());assertEquals(1,count(owner,"SELECT count(*) FROM subscription_entitlement WHERE enabled"));
    assertThrows(IllegalStateException.class,()->lifecycle.activatePayment(UUID.randomUUID(),p.id()));
    assertTrue(lifecycle.printReceipt(p.id(),true,"fr").contains("MANUALLY_RECONCILED"));assertTrue(invoices.printInvoice(invoice.id(),true,"fr").contains("Facture d’abonnement"));
    TenantContext.call(scope,1,()->{lifecycle.requireAllowed(SubscriptionLifecycleService.Operation.NEW_BUSINESS_POSTING);return null;});
    as(merchant,"billing:read");TenantContext.call(scope,1,()->{assertTrue(lifecycle.printReceipt(p.id(),false,"en").contains("Synthetic bank"));assertTrue(invoices.printInvoice(invoice.id(),false,"en").contains(invoice.number()));return null;});
    as(maker,"platform:billing:write");lifecycle.cancel(UUID.randomUUID(),tenant,2,"Synthetic cancellation after payment");
    as(checker,"platform:billing:approve","platform:billing:read");assertThrows(IllegalStateException.class,()->lifecycle.activatePayment(UUID.randomUUID(),finalPayment.id()));
    assertEquals(0,count(owner,"SELECT count(*) FROM tender_fact"));assertEquals(0,count(owner,"SELECT count(*) FROM sale"));
    assertThrows(SQLException.class,()->sql(owner,"UPDATE subscription_manual_payment SET amount_xaf=1 WHERE id='"+p.id()+"'"));
    // A DBA-imported historical paid snapshot is elapsed; displaying it must not
    // imply a current paid term, and expiry persists without weakening immutability.
    UUID expiredTenant=UUID.randomUUID();sql(owner,"INSERT INTO tenant(id,name)VALUES('"+expiredTenant+"','Synthetic historical paid snapshot')");
    sql(owner,"INSERT INTO tenant_subscription(tenant_id,plan_code,status,trial_ends_at,version,created_at,updated_at,term_ends_at)VALUES('"+expiredTenant+"','synthetic-paid','ACTIVE',now()-interval '46 days',0,now()-interval '60 days',now()-interval '30 days',now()-interval '1 day')");
    as(maker,"platform:billing:read","platform:billing:write");assertEquals("EXPIRED",lifecycle.verifyTrial(expiredTenant).status());assertEquals(0,lifecycle.verifyTrial(expiredTenant).version());
    UUID expireOperation=UUID.randomUUID();var expired=lifecycle.expireTrial(expireOperation,expiredTenant,0);assertEquals("EXPIRED",expired.status());assertEquals(1,expired.version());assertEquals(expired,lifecycle.expireTrial(expireOperation,expiredTenant,0));
    // Historical immutable invoice fixtures with identical timestamps exercise the
    // UUID tie-breaker beyond the old 100-record limit, not fake ingestion evidence.
    sql(owner,"INSERT INTO subscription_invoice SELECT (jsonb_populate_record(NULL::subscription_invoice,to_jsonb(i)||jsonb_build_object('id',gen_random_uuid(),'number','HISTORY-'||n,'description','=1+1','customer_name','  +SUM(1,1)'))).* FROM subscription_invoice i CROSS JOIN generate_series(1,205) n WHERE i.id='"+invoice.id()+"'");
    long historicalCount=count(owner,"SELECT count(*) FROM subscription_invoice WHERE tenant_id='"+tenant+"'");
    var firstPage=invoices.platformPage(tenant,null,37);assertEquals(37,firstPage.items().size());assertNotNull(firstPage.nextCursor());assertNotNull(firstPage.currentCursor());assertEquals(firstPage,invoices.platformPage(tenant,firstPage.currentCursor(),37));
    var csv=invoices.platformExport(tenant,firstPage.currentCursor(),37);assertEquals(firstPage.cutoff(),csv.cutoff());assertEquals(firstPage.nextCursor(),csv.nextCursor());assertTrue(csv.content().contains("\"'=1+1\""));assertTrue(csv.content().contains("\"'  +SUM(1,1)\""));
    assertThrows(IllegalArgumentException.class,()->invoices.platformPage(otherTenant,firstPage.nextCursor(),37));
    String tampered=(firstPage.nextCursor().startsWith("A")?"B":"A")+firstPage.nextCursor().substring(1);assertThrows(IllegalArgumentException.class,()->invoices.platformPage(tenant,tampered,37));
    assertThrows(IllegalArgumentException.class,()->invoices.platformPage(tenant,null,201));
    var concurrentInvoice=invoices.issue(UUID.randomUUID(),new SubscriptionBillingService.Issue(UUID.randomUUID(),tenant,null,"Synthetic","TEST","Synthetic","","After export cutoff","TEST",today,today,today,"10","0","0"));
    Set<UUID> seen=new HashSet<>();var page=firstPage;while(true){for(var row:page.items())assertTrue(seen.add(row.id()),"No repeated invoice across keyset pages");if(page.nextCursor()==null)break;page=invoices.platformPage(tenant,page.nextCursor(),37);}assertEquals(historicalCount,seen.size());assertFalse(seen.contains(concurrentInvoice.id()));
    as(merchant,"billing:read");TenantContext.call(scope,1,()->{assertEquals(firstPage.items(),invoices.merchantPage(firstPage.currentCursor(),37).items());assertTrue(invoices.merchantExport(firstPage.currentCursor(),37).content().contains("invoice_id"));return null;});
    // Exact signed commercial contracts permit proration; no approval means no payment activation.
    var terms=new DefaultSubscriptionCommercialTermsService(em,manager,new JsonMapper());UUID commercialTenant=UUID.randomUUID();sql(owner,"INSERT INTO tenant(id,name)VALUES('"+commercialTenant+"','Synthetic prorated tenant')");
    as(maker,"platform:billing:write","platform:billing:read");lifecycle.createPlan(UUID.randomUUID(),new SubscriptionLifecycleService.Plan("prorated-plan","Synthetic proration","3000",5,1,1,false,Instant.now().minusSeconds(1)));lifecycle.bootstrapTrial(UUID.randomUUID(),commercialTenant,"standard-trial",ends);
    long days=today.lengthOfMonth()-today.getDayOfMonth()+1;long prorated=(2*3000*days+today.lengthOfMonth())/(2*today.lengthOfMonth());
    var proratedInvoice=invoices.issue(UUID.randomUUID(),new SubscriptionBillingService.Issue(UUID.randomUUID(),commercialTenant,null,"Synthetic","TEST","Synthetic","","Prorated current term","TEST",today,today.withDayOfMonth(today.lengthOfMonth()),today,Long.toString(prorated),"0","0"));
    var prorationPayment=new SubscriptionLifecycleService.Payment(UUID.randomUUID(),commercialTenant,proratedInvoice.id(),"prorated-plan",Long.toString(prorated),"Synthetic proration bank ref","e".repeat(64),"Synthetic contractual proration");
    if(today.getDayOfMonth()!=1)assertThrows(RuntimeException.class,()->lifecycle.recordPayment(prorationPayment));
    var termsInput=new SubscriptionCommercialTermsService.Request(UUID.randomUUID(),commercialTenant,proratedInvoice.id(),"prorated-plan","PRORATED","Synthetic signed commercial contract","f".repeat(64),"Exact calendar-day proration");var requested=terms.request(termsInput);assertEquals(requested,terms.request(termsInput));assertEquals("REQUESTED",requested.status());assertEquals(Long.toString(prorated),requested.subtotalXaf());
    as(maker,"platform:billing:approve");assertThrows(AccessDeniedException.class,()->terms.approve(UUID.randomUUID(),requested.id()));
    as(checker,"platform:billing:approve","platform:billing:read");UUID termsApproval=UUID.randomUUID();var approved=terms.approve(termsApproval,requested.id());assertEquals("APPROVED",approved.status());assertEquals(approved,terms.approve(termsApproval,requested.id()));
    as(maker,"platform:billing:write");var recorded=lifecycle.recordPayment(prorationPayment);
    as(checker,"platform:billing:approve","platform:billing:read");lifecycle.approvePayment(UUID.randomUUID(),recorded.id());assertEquals("ACTIVE",lifecycle.verifyTrial(commercialTenant).status());assertEquals("prorated-plan",lifecycle.verifyTrial(commercialTenant).planCode());
    assertThrows(SQLException.class,()->sql(owner,"UPDATE subscription_commercial_term SET total_xaf=1 WHERE id='"+requested.id()+"'"));
    try(var runtime=DriverManager.getConnection(url,role,"")){assertEquals(0,count(runtime,"SELECT count(*) FROM tenant_subscription"));try(var statement=runtime.createStatement()){assertEquals(0,statement.executeUpdate("UPDATE tenant_subscription SET status='ACTIVE'"));}
     sql(runtime,"select set_config('app.platform_actor','"+maker+"',false)");assertThrows(SQLException.class,()->sql(runtime,"INSERT INTO tenant_subscription(tenant_id,plan_code,status,trial_ends_at,version,created_at,updated_at,term_ends_at)VALUES('"+otherTenant+"','synthetic-paid','ACTIVE',now()+interval '14 days',0,now(),now(),now()+interval '30 days')"));}
    as(checker,"platform:billing:approve");
    sql(owner,"UPDATE platform_operator SET active=false WHERE actor_id='"+checker+"'");assertThrows(AccessDeniedException.class,()->lifecycle.approvePayment(finalApproval,finalPayment.id()));
   }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
}
