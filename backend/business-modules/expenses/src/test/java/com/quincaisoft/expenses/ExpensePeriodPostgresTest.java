package com.quincaisoft.expenses;

import com.quincaisoft.expenses.api.ExpenseService;
import com.quincaisoft.expenses.application.*;
import com.quincaisoft.finance.api.*;
import com.quincaisoft.finance.application.*;
import com.quincaisoft.company.application.DefaultCompanyService;
import com.quincaisoft.tenders.api.TenderService;
import com.quincaisoft.tenders.application.*;
import com.quincaisoft.shared.*;
import com.quincaisoft.returns.application.ReturnsPeriodGuard;
import com.quincaisoft.integrations.application.ProviderPeriodGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.time.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Real typed JPA writes, runtime non-owner RLS, independent makers/checkers and cash conservation. */
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class ExpensePeriodPostgresTest {
 @Test void expensesAndPeriodChangesAreApprovedAtomicAndReplayable() throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"), adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String db="expense_test_"+UUID.randomUUID().toString().replace("-",""), role="expense_role_"+UUID.randomUUID().toString().replace("-","");
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID t=UUID.randomUUID(),c=UUID.randomUUID(),b=UUID.randomUUID(),maker=UUID.randomUUID(),checker=UUID.randomUUID();
  var makerScope=new TenantScope(t,c,b,maker);var checkerScope=new TenantScope(t,c,b,checker);var date=LocalDate.of(2026,11,18);
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")){
    try(var files=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));}
    sql(owner,"INSERT INTO tenant(id,name)VALUES('"+t+"','Synthetic');INSERT INTO company(tenant_id,id,name)VALUES('"+t+"','"+c+"','Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+t+"','"+c+"','"+b+"','Synthetic')");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.expenses.infrastructure","com.quincaisoft.tenders.infrastructure","com.quincaisoft.finance.infrastructure","com.quincaisoft.company.infrastructure","com.quincaisoft.returns.infrastructure","com.quincaisoft.integrations.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var s=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",s.tenantId().toString()).setParameter("c",s.companyId().toString()).setParameter("b",s.branchId().toString()).setParameter("a",s.actorId().toString()).getSingleResult();return action.get();});}};
    var json=JsonMapper.builder().build();var finance=new DefaultFinanceService(em,tx);var tenders=new DefaultTenderService(em,tx,finance,null,null,json);var expenses=new DefaultExpenseService(em,tx,tenders,finance,json);
    // Actual module guard implementations query typed, RLS-scoped tables.
    PeriodCloseGuard providers=new ProviderPeriodGuard(em),returns=new ReturnsPeriodGuard(em);
    var periods=new DefaultPeriodService(em,tx,new DefaultCompanyService(em,tx),List.of(new CashPeriodGuard(em),new ExpensePeriodGuard(em),providers,returns),json);
    var create=new PeriodService.Create(UUID.randomUUID(),date.withDayOfYear(1),LocalDate.of(2026,12,31),"Annual operating period");
    var period=TenantContext.call(makerScope,1,()->periods.create(create));assertEquals(period,TenantContext.call(makerScope,1,()->periods.create(create)));
    var draft=TenantContext.call(makerScope,1,()->expenses.draft(new ExpenseService.Draft(UUID.randomUUID(),date,"DELIVERY","Synthetic carrier","300","Delivery expense evidence")));
    var reviewed=TenantContext.call(makerScope,1,()->expenses.command(draft.id(),"submit",expense(draft.version(),null)));
    assertThrows(IllegalArgumentException.class,()->TenantContext.call(makerScope,1,()->expenses.command(draft.id(),"approve",expense(reviewed.version(),null))));
    var approved=TenantContext.call(checkerScope,1,()->expenses.command(draft.id(),"approve",expense(reviewed.version(),null)));
    var request=TenantContext.call(makerScope,1,()->periods.request(period.id(),"CLOSE",command(period.version())));
    assertThrows(IllegalArgumentException.class,()->TenantContext.call(makerScope,1,()->periods.decide(request.id(),true,command(request.version()))));
    assertThrows(IllegalStateException.class,()->TenantContext.call(checkerScope,1,()->periods.decide(request.id(),true,command(request.version())))); // Approved unpaid expense.
    var shift=TenantContext.call(makerScope,1,()->tenders.open(new TenderService.Open(UUID.randomUUID(),"TILL",date,"1000","Counted opening cash")));
    var pay=new ExpenseService.Command(UUID.randomUUID(),approved.version(),"Paid verified carrier",shift.id());
    var paid=TenantContext.call(makerScope,1,()->expenses.command(draft.id(),"pay",pay));assertEquals("POSTED",paid.status());assertEquals(paid,TenantContext.call(makerScope,1,()->expenses.command(draft.id(),"pay",pay)));
    assertEquals(1,count(owner,"select count(*) from tender_fact where kind='EXPENSE'"));assertEquals(1,count(owner,"select count(*) from journal_entry"));assertEquals(0,count(owner,"select sum(debit_xaf-credit_xaf) from journal_line"));
    var currentShift=TenantContext.call(makerScope,1,()->tenders.sessions().getFirst());assertEquals("700",currentShift.expectedXaf());
    assertThrows(IllegalStateException.class,()->TenantContext.call(checkerScope,1,()->periods.decide(request.id(),true,command(request.version())))); // Till still open.
    TenantContext.call(makerScope,1,()->tenders.close(shift.id(),new TenderService.Close(UUID.randomUUID(),currentShift.version(),date,"700","Till count reconciled")));
    UUID warehouse=UUID.randomUUID(),sale=UUID.randomUUID(),intent=UUID.randomUUID(),ret=UUID.randomUUID();
    // Synthetic historical source must satisfy the same deferred invoice invariant as every posted sale.
    UUID saleOperation=UUID.randomUUID();String content="{\"fixture\":\"period-close-guard\"}";
    String digest=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    owner.setAutoCommit(false);
    try {
     sql(owner,"INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name)VALUES('"+t+"','"+c+"','"+b+"','"+warehouse+"','Guard fixture');INSERT INTO sale(tenant_id,company_id,branch_id,id,operation_id,warehouse_id,actor_id,business_date,status,total_xaf)VALUES('"+t+"','"+c+"','"+b+"','"+sale+"','"+saleOperation+"','"+warehouse+"','"+maker+"','2026-11-18','POSTED',100)");
     sql(owner,"INSERT INTO document_snapshot(tenant_id,company_id,branch_id,id,source_id,operation_id,number,kind,business_date,total_xaf,sha256,content,issued_at)VALUES('"+t+"','"+c+"','"+b+"',gen_random_uuid(),'"+sale+"','"+saleOperation+"','GUARD-INVOICE-000001','INVOICE','2026-11-18',100,'"+digest+"','"+content+"',now())");
     owner.commit();
    } catch(Exception failure){owner.rollback();throw failure;} finally{owner.setAutoCommit(true);}
    sql(owner,"INSERT INTO provider_intent(tenant_id,company_id,branch_id,id,operation_id,sale_id,actor_id,business_date,amount_xaf,currency,environment,phone,status,reason)VALUES('"+t+"','"+c+"','"+b+"','"+intent+"',gen_random_uuid(),'"+sale+"','"+maker+"','2026-11-18',100,'XAF','mtncameroon','237600000000','PENDING','Synthetic provider guard fixture')");
    assertThrows(IllegalStateException.class,()->TenantContext.call(checkerScope,1,()->periods.decide(request.id(),true,command(request.version()))));sql(owner,"update provider_intent set status='FAILED' where id='"+intent+"'");
    sql(owner,"INSERT INTO customer_return(tenant_id,company_id,branch_id,id,sale_id,warehouse_id,customer_id,created_by,approved_by,operation_id,status,condition_code,evidence_reference,business_date,created_at)VALUES('"+t+"','"+c+"','"+b+"','"+ret+"','"+sale+"','"+warehouse+"',gen_random_uuid(),'"+maker+"','"+checker+"',gen_random_uuid(),'APPROVED','SELLABLE','Synthetic return guard fixture','2026-11-18',now())");
    assertThrows(IllegalStateException.class,()->TenantContext.call(checkerScope,1,()->periods.decide(request.id(),true,command(request.version()))));sql(owner,"update customer_return set status='POSTED' where id='"+ret+"'");
    var close=command(request.version());var decision=TenantContext.call(checkerScope,1,()->periods.decide(request.id(),true,close));assertEquals("APPROVED",decision.status());assertEquals(decision,TenantContext.call(checkerScope,1,()->periods.decide(request.id(),true,close)));
    assertEquals("Closed",TenantContext.call(makerScope,1,()->periods.list().getFirst().status()));
    assertThrows(IllegalStateException.class,()->TenantContext.call(makerScope,1,()->expenses.draft(new ExpenseService.Draft(UUID.randomUUID(),date,"DELIVERY","Carrier","1","Closed period denied"))));
    var closed=TenantContext.call(makerScope,1,()->periods.list().getFirst());var reopen=TenantContext.call(makerScope,1,()->periods.request(period.id(),"REOPEN",command(closed.version())));
    TenantContext.call(checkerScope,1,()->periods.decide(reopen.id(),true,command(reopen.version())));assertEquals("Open",TenantContext.call(makerScope,1,()->periods.list().getFirst().status()));
    // Raw status mutation is still denied even though the application has column permission.
    assertThrows(RuntimeException.class,()->TenantContext.call(makerScope,1,()->tx.execute(()->em.createNativeQuery("update financial_period set status='Closed'").executeUpdate())));
    sql(owner,"INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+t+"','"+c+"',gen_random_uuid(),'Second branch')");
    assertThrows(IllegalStateException.class,()->TenantContext.call(makerScope,1,()->periods.create(new PeriodService.Create(UUID.randomUUID(),LocalDate.of(2027,1,1),LocalDate.of(2027,12,31),"Multi-branch blocked"))));
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static ExpenseService.Command expense(long version,UUID cash){return new ExpenseService.Command(UUID.randomUUID(),version,"Reviewed expense evidence",cash);}
 static PeriodService.Command command(long version){return new PeriodService.Command(UUID.randomUUID(),version,"Reviewed close evidence");}
 static void sql(Connection c,String query)throws SQLException{try(var s=c.createStatement()){s.execute(query);}}
 static long count(Connection c,String query)throws SQLException{try(var s=c.createStatement();var rs=s.executeQuery(query)){rs.next();return rs.getLong(1);}}
}
