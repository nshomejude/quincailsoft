package com.quincaisoft.subscriptions;

import com.quincaisoft.subscriptions.api.SubscriptionBillingService;
import com.quincaisoft.subscriptions.application.DefaultSubscriptionBillingService;
import com.quincaisoft.shared.ScopedTransactions;
import com.quincaisoft.shared.TenantContext;
import com.quincaisoft.shared.TenantScope;
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
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresSubscriptionBillingTest {
    @Test void billingDocumentsAreSeparateImmutableScopedReplaySafeAndAtomic() throws Exception {
        String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
        String db="billing_test_"+UUID.randomUUID().toString().replace("-",""),role="billing_role_"+UUID.randomUUID().toString().replace("-","");
        String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
        UUID operator=UUID.randomUUID(),otherOperator=UUID.randomUUID(),tenant=UUID.randomUUID(),otherTenant=UUID.randomUUID();
        try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")) {
            sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
            sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");
            sql(admin,"GRANT quincai_app TO "+role); sql(admin,"CREATE DATABASE "+db);
            LocalContainerEntityManagerFactoryBean factory=null;
            try(var owner=DriverManager.getConnection(url,adminUser,"")) {
                try(var files=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))) {
                    for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));
                }
                sql(owner,"INSERT INTO platform_operator VALUES('"+operator+"',true),('"+otherOperator+"',true)");
                sql(owner,"INSERT INTO tenant(id,name) VALUES('"+tenant+"','Synthetic tenant'),('"+otherTenant+"','Other synthetic tenant')");
                factory=new LocalContainerEntityManagerFactoryBean();
                factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
                factory.setPackagesToScan("com.quincaisoft.subscriptions.infrastructure");
                factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
                var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);
                var manager=new JpaTransactionManager(emf);
                ScopedTransactions merchantScope=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return new TransactionTemplate(manager).execute(status->{
                    var scope=TenantContext.current();
                    em.createNativeQuery("select set_config('app.tenant_id',:tenant,true),set_config('app.company_id',:company,true),set_config('app.branch_id',:branch,true),set_config('app.actor_id',:actor,true)")
                        .setParameter("tenant",scope.tenantId().toString()).setParameter("company",scope.companyId().toString()).setParameter("branch",scope.branchId().toString()).setParameter("actor",scope.actorId().toString()).getSingleResult();
                    return action.get();
                });}};
                var service=new DefaultSubscriptionBillingService(em,manager,merchantScope,new JsonMapper());
                var input=issue(tenant,null,"10000","1000","1500");
                as(operator,"platform:billing:read"); assertThrows(AccessDeniedException.class,()->service.issue(UUID.randomUUID(),input));
                as(UUID.randomUUID(),"platform:billing:write");assertThrows(AccessDeniedException.class,()->service.issue(UUID.randomUUID(),input));
                as(operator,"platform:billing:write","platform:billing:read");
                var invoice=service.issue(UUID.randomUUID(),input);assertEquals("10500",invoice.totalXaf());
                assertEquals(invoice,service.issue(UUID.randomUUID(),input));
                assertEquals(1,count(owner,"SELECT count(*) FROM subscription_billing_event"));
                assertEquals(1,service.platformInvoices(tenant).size());assertEquals(0,service.platformInvoices(otherTenant).size());
                as(otherOperator,"platform:billing:write");assertThrows(IllegalStateException.class,()->service.issue(UUID.randomUUID(),input));
                as(operator,"platform:billing:write","platform:billing:read");
                var changed=new SubscriptionBillingService.Issue(input.operationId(),tenant,null,input.issuerName(),input.issuerTaxReference(),input.customerName(),input.customerTaxReference(),"Changed",input.jurisdiction(),input.periodStart(),input.periodEnd(),input.dueDate(),"10000","1000","1500");
                assertThrows(IllegalStateException.class,()->service.issue(UUID.randomUUID(),changed));
                as(operator,"platform:billing:write","platform:billing:credit");
                assertThrows(AccessDeniedException.class,()->service.issue(UUID.randomUUID(),issue(tenant,invoice.id(),"1000","0","0")));
                as(otherOperator,"platform:billing:write","platform:billing:credit");
                assertThrows(NoSuchElementException.class,()->service.issue(UUID.randomUUID(),issue(otherTenant,invoice.id(),"1000","0","0")));
                var credit=service.issue(UUID.randomUUID(),issue(tenant,invoice.id(),"1000","0","100"));
                assertEquals("CREDIT_NOTE",credit.kind());
                assertThrows(IllegalStateException.class,()->service.issue(UUID.randomUUID(),issue(tenant,invoice.id(),"9000","0","0")));
                assertThrows(IllegalStateException.class,()->service.issue(UUID.randomUUID(),issue(tenant,invoice.id(),"1000","0","1500")));
                as(operator,"platform:billing:write","platform:billing:read");
                assertEquals(invoice,service.issue(UUID.randomUUID(),input));
                assertThrows(IllegalArgumentException.class,()->service.issue(UUID.randomUUID(),issue(tenant,null,"1.5","0","0")));
                assertThrows(ArithmeticException.class,()->service.issue(UUID.randomUUID(),issue(tenant,null,Long.toString(Long.MAX_VALUE),"0","1")));
                try(var runtime=DriverManager.getConnection(url,role,"")) {
                    assertEquals(0,count(runtime,"SELECT count(*) FROM subscription_invoice"));
                    sql(runtime,"select set_config('app.tenant_id','"+tenant+"',false)");
                    assertEquals(2,count(runtime,"SELECT count(*) FROM subscription_invoice"));
                    sql(runtime,"select set_config('app.tenant_id','"+otherTenant+"',false)");
                    assertEquals(0,count(runtime,"SELECT count(*) FROM subscription_invoice"));
                    assertThrows(SQLException.class,()->sql(runtime,"DELETE FROM subscription_invoice"));
                    sql(runtime,"select set_config('app.platform_actor','"+otherOperator+"',false)");
                    // Runtime SQL cannot forge another operator's attribution or bypass credit conservation.
                    assertThrows(SQLException.class,()->sql(runtime,"INSERT INTO subscription_invoice SELECT (jsonb_populate_record(NULL::subscription_invoice,to_jsonb(i)||'{\"id\":\""+UUID.randomUUID()+"\",\"number\":\"FORGED-ACTOR\"}'::jsonb)).* FROM subscription_invoice i WHERE id='"+invoice.id()+"'"));
                    assertThrows(SQLException.class,()->sql(runtime,"INSERT INTO subscription_invoice SELECT (jsonb_populate_record(NULL::subscription_invoice,to_jsonb(i)||'{\"id\":\""+UUID.randomUUID()+"\",\"number\":\"EXCESS-CREDIT\",\"subtotal_xaf\":100000,\"total_xaf\":100100}'::jsonb)).* FROM subscription_invoice i WHERE id='"+credit.id()+"'"));
                }
                assertThrows(SQLException.class,()->sql(owner,"UPDATE subscription_invoice SET total_xaf=1 WHERE id='"+invoice.id()+"'"));
                // Durable invoice and billing event are one transaction: forced event failure leaves neither.
                sql(owner,"CREATE FUNCTION billing_test_fail() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'injected event failure'; END $$");
                sql(owner,"CREATE TRIGGER billing_fail BEFORE INSERT ON subscription_billing_event FOR EACH ROW EXECUTE FUNCTION billing_test_fail()");
                var rollback=issue(tenant,null,"5000","0","0");
                assertThrows(RuntimeException.class,()->service.issue(UUID.randomUUID(),rollback));
                assertEquals(0,count(owner,"SELECT count(*) FROM subscription_invoice WHERE id='"+rollback.operationId()+"'"));
                sql(owner,"DROP TRIGGER billing_fail ON subscription_billing_event");
                // Concurrent retries converge; competing credits cannot overrun net/tax.
                var concurrentInput=issue(tenant,null,"5000","0","500");
                try(var pool=Executors.newFixedThreadPool(6)) {
                    List<Future<SubscriptionBillingService.Invoice>> results=new ArrayList<>();
                    for(int i=0;i<6;i++)results.add(pool.submit(()->{as(operator,"platform:billing:write");try{return service.issue(UUID.randomUUID(),concurrentInput);}finally{SecurityContextHolder.clearContext();}}));
                    var first=results.getFirst().get(30,TimeUnit.SECONDS);
                    for(var result:results)assertEquals(first,result.get(30,TimeUnit.SECONDS));
                    var start=new CountDownLatch(1);List<Future<Boolean>> credits=new ArrayList<>();
                    for(int i=0;i<2;i++)credits.add(pool.submit(()->{as(otherOperator,"platform:billing:write","platform:billing:credit");start.await();try{service.issue(UUID.randomUUID(),issue(tenant,first.id(),"5000","0","500"));return true;}catch(IllegalStateException expected){return false;}finally{SecurityContextHolder.clearContext();}}));
                    start.countDown();int winners=0;for(var result:credits)if(result.get(30,TimeUnit.SECONDS))winners++;assertEquals(1,winners);
                }
                assertEquals(0,count(owner,"SELECT count(*) FROM sale"));
                assertEquals(0,count(owner,"SELECT count(*) FROM tender_fact"));
                assertEquals(4,count(owner,"SELECT count(*) FROM subscription_billing_event"));
                UUID company=UUID.randomUUID(),branch=UUID.randomUUID(),merchant=UUID.randomUUID(),cashier=UUID.randomUUID(),financier=UUID.randomUUID();
                sql(owner,"INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+company+"','Synthetic company')");
                sql(owner,"INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+branch+"','Synthetic branch')");
                sql(owner,"INSERT INTO tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code,authorization_epoch)VALUES('"+tenant+"','"+company+"','"+branch+"','"+merchant+"','owner',2),('"+tenant+"','"+company+"','"+branch+"','"+cashier+"','cashier',1),('"+tenant+"','"+company+"','"+branch+"','"+financier+"','finance',1)");
                as(merchant,"billing:read");
                assertEquals(4,TenantContext.call(new TenantScope(tenant,company,branch,merchant),2,service::merchantInvoices).size());
                assertThrows(AccessDeniedException.class,()->TenantContext.call(new TenantScope(tenant,company,branch,merchant),1,service::merchantInvoices));
                assertThrows(AccessDeniedException.class,()->TenantContext.call(new TenantScope(otherTenant,company,branch,merchant),2,service::merchantInvoices));
                as(cashier,"billing:read");assertThrows(AccessDeniedException.class,()->TenantContext.call(new TenantScope(tenant,company,branch,cashier),1,service::merchantInvoices));
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg","RS256").subject(financier.toString()).build(),List.of(new SimpleGrantedAuthority("billing:read"))));
                assertEquals(4,TenantContext.call(new TenantScope(tenant,company,branch,financier),1,service::merchantInvoices).size());
                sql(owner,"UPDATE platform_operator SET active=false WHERE actor_id='"+operator+"'");
                as(operator,"platform:billing:write");assertThrows(AccessDeniedException.class,()->service.issue(UUID.randomUUID(),input));
            } finally {
                SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();
                sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);
            }
        }
    }
    static SubscriptionBillingService.Issue issue(UUID tenant,UUID original,String subtotal,String discount,String tax) {
        return new SubscriptionBillingService.Issue(UUID.randomUUID(),tenant,original,"Synthetic SaaS issuer","TEST-TAX","Synthetic customer","","Subscription fee","TEST-CM",LocalDate.of(2026,11,1),LocalDate.of(2026,11,30),LocalDate.of(2026,11,30),subtotal,discount,tax);
    }
    static void as(UUID actor,String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("synthetic-test").header("alg","RS256").subject(actor.toString()).build(),Arrays.stream(authorities).map(a->new SimpleGrantedAuthority("SCOPE_"+a)).toList()));
    }
    static void sql(Connection connection,String query)throws SQLException{try(var statement=connection.createStatement()){statement.execute(query);}}
    static long count(Connection connection,String query)throws SQLException{try(var statement=connection.createStatement();var rows=statement.executeQuery(query)){rows.next();return rows.getLong(1);}}
}
