package com.quincaisoft.tenders;
import com.quincaisoft.tenders.api.TenderService;import com.quincaisoft.tenders.application.DefaultTenderService;
import com.quincaisoft.sales.api.SalesService;
import com.quincaisoft.documents.application.DefaultDocumentService;
import com.quincaisoft.documents.api.DocumentService;
import com.quincaisoft.sales.application.DefaultSalesService;
import com.quincaisoft.finance.application.DefaultFinanceService;
import com.quincaisoft.customeraccounts.application.DefaultCustomerService;
import com.quincaisoft.shared.*;
import com.quincaisoft.catalog.application.DefaultCatalogService;
import com.quincaisoft.inventory.application.DefaultStockService;
import com.quincaisoft.finance.api.FinanceService;
import com.quincaisoft.customeraccounts.api.CustomerService;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.sql.*;import java.nio.file.*;import java.util.*;import java.time.*;import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
/** Executes production JPA modules against real PG18 as a non-owner/NOBYPASSRLS role. */
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class TenderPostgresTest {
 @Test void cashDepositsAllocationsRefundsConserveAndReplay() throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String db="sale_test_"+UUID.randomUUID().toString().replace("-",""),role="sale_role_"+UUID.randomUUID().toString().replace("-","");
  UUID t=UUID.randomUUID(),c=UUID.randomUUID(),b=UUID.randomUUID(),a=UUID.randomUUID(),w=UUID.randomUUID(),p=UUID.randomUUID(),customer=UUID.randomUUID();
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")) {
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")) {
    Path migrations=Path.of("../../db-migrations/src/main/resources/db/migration");
    for(var file:Files.list(migrations).filter(x->x.toString().endsWith(".sql")).sorted(java.util.Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));
    sql(owner,"INSERT INTO tenant(id,name) VALUES('"+t+"','Synthetic');INSERT INTO company(tenant_id,id,name)VALUES('"+t+"','"+c+"','Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+t+"','"+c+"','"+b+"','Synthetic');INSERT INTO tenant_memberships(tenant_id,actor_id,company_id,branch_id,role_code)VALUES('"+t+"','"+a+"','"+c+"','"+b+"','owner');INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name)VALUES('"+t+"','"+c+"','"+b+"','"+w+"','Synthetic');INSERT INTO customer(tenant_id,company_id,id,name)VALUES('"+t+"','"+c+"','"+customer+"','Synthetic');INSERT INTO catalog_product(tenant_id,company_id,id,sku,name,base_unit,selling_price_xaf)VALUES('"+t+"','"+c+"','"+p+"','SKU','Synthetic','piece',1000);INSERT INTO stock_balance(tenant_id,company_id,branch_id,warehouse_id,product_id,quantity_milli,value_xaf)VALUES('"+t+"','"+c+"','"+b+"','"+w+"','"+p+"',10000,5000);INSERT INTO financial_period(tenant_id,company_id,id,start_date,end_date,status)VALUES('"+t+"','"+c+"',gen_random_uuid(),'2026-01-01','2026-12-31','Open')");
    var ds=new DriverManagerDataSource(url,role,"");factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.tenders.infrastructure","com.quincaisoft.documents.infrastructure","com.quincaisoft.sales.infrastructure","com.quincaisoft.finance.infrastructure","com.quincaisoft.inventory.infrastructure","com.quincaisoft.catalog.infrastructure","com.quincaisoft.customeraccounts.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var catalog=new DefaultCatalogService(em,tx);var stock=new DefaultStockService(em,tx,catalog);var finance=new DefaultFinanceService(em,tx);var customers=new DefaultCustomerService(em,tx);var documents=new DefaultDocumentService(em,tx,JsonMapper.builder().build());var sales=new DefaultSalesService(em,tx,stock,catalog,finance,customers,JsonMapper.builder().build(),documents);
    var tenders=new DefaultTenderService(em,tx,finance,sales,customers,JsonMapper.builder().build());
    var scope=new TenantScope(t,c,b,a);
    var differentActor=new TenantScope(t,c,b,UUID.randomUUID());
    var otherBranch=UUID.randomUUID();
    sql(owner,"INSERT INTO branch(tenant_id,company_id,id,name) VALUES('"+t+"','"+c+"','"+otherBranch+"','Other branch')");
    TenantContext.call(scope,1,()->{
     assertEquals(0,finance.pendingCollectionCount());
     documents.configure(new DocumentService.Settings("Synthetic merchant","Synthetic address","TEST-REG","TEST","en",0));
     var draftRequest=new SalesService.Draft(UUID.randomUUID(),w,customer,LocalDate.of(2026,11,18),List.of(new SalesService.RequestedLine(p,"2000")));
     var draft=sales.draft(draftRequest);assertEquals("DRAFT",draft.status());assertEquals("2000",draft.totalXaf());
     var postRequest=new SalesService.Post(UUID.randomUUID(),draft.version(),"Synthetic acceptance posting");var posted=sales.post(draft.id(),postRequest);assertEquals("POSTED",posted.status());assertEquals(posted,sales.post(draft.id(),postRequest));assertEquals(1,documents.list(draft.id()).size());assertTrue(documents.render(documents.list(draft.id()).getFirst().id(),true).contains("COPY"));assertEquals(draft,sales.draft(draftRequest));
     assertEquals(1,finance.pendingCollectionCount());
     assertEquals(8000,count(owner,"select quantity_milli from stock_balance"));assertEquals(4000,count(owner,"select value_xaf from stock_balance"));assertEquals(1,count(owner,"select count(*) from journal_entry"));assertEquals(4,count(owner,"select count(*) from journal_line"));assertEquals(1,count(owner,"select count(*) from outbox_event"));assertEquals(0,count(owner,"select sum(debit_xaf-credit_xaf) from journal_line"));
     assertThrows(IllegalStateException.class,()->sales.post(draft.id(),new SalesService.Post(postRequest.operationId(),draft.version(),"Different request must conflict")));
     var excessive=sales.draft(new SalesService.Draft(UUID.randomUUID(),w,customer,LocalDate.of(2026,11,18),List.of(new SalesService.RequestedLine(p,"100000"))));
     assertThrows(IllegalArgumentException.class,()->sales.post(excessive.id(),new SalesService.Post(UUID.randomUUID(),excessive.version(),"Oversell must rollback")));
     assertEquals("DRAFT",sales.get(excessive.id()).status());assertEquals(8000,count(owner,"select quantity_milli from stock_balance"));
     // Closed period fails after stock issue in memory; whole transaction rolls it back.
     var next=sales.draft(new SalesService.Draft(UUID.randomUUID(),w,customer,LocalDate.of(2026,11,18),List.of(new SalesService.RequestedLine(p,"1000"))));
     sql(owner,"update financial_period set status='Closed'");
     assertThrows(IllegalStateException.class,()->sales.post(next.id(),new SalesService.Post(UUID.randomUUID(),next.version(),"Closed period must rollback")));
     assertEquals("DRAFT",sales.get(next.id()).status());assertEquals(8000,count(owner,"select quantity_milli from stock_balance"));assertEquals(1,count(owner,"select count(*) from stock_movement"));assertEquals(1,count(owner,"select count(*) from journal_entry"));assertEquals(1,count(owner,"select count(*) from outbox_event"));assertEquals(1,count(owner,"select count(*) from document_snapshot"));
     sql(owner,"update financial_period set status='Open'");
     var date=LocalDate.of(2026,11,18);var opening=new TenderService.Open(UUID.randomUUID(),"TILL-1",date,"100","Opening float declaration");var shift=tenders.open(opening);
     assertEquals(shift,tenders.open(opening));
     assertThrows(IllegalStateException.class,()->TenantContext.call(differentActor,1,()->tenders.open(opening)));
     assertEquals(1,tenders.sessions().size());
     var collect=new TenderService.Receipt(UUID.randomUUID(),draft.id(),customer,shift.id(),date,"500","Confirmed physical cash");var payment=tenders.receipt(collect);assertEquals(payment,tenders.receipt(collect));
     long journalCount=count(owner,"select count(*) from journal_entry");long factCount=count(owner,"select count(*) from tender_fact");
     assertThrows(IllegalStateException.class,()->TenantContext.call(differentActor,1,()->tenders.receipt(collect)));
     // Forced RLS conceals both the other branch receipt and the source sale.
     var concealed=assertThrows(NoSuchElementException.class,()->TenantContext.call(new TenantScope(t,c,otherBranch,a),1,()->tenders.receipt(collect)));
     assertEquals("Sale not found",concealed.getMessage());
     assertEquals(journalCount,count(owner,"select count(*) from journal_entry"));assertEquals(factCount,count(owner,"select count(*) from tender_fact"));
     assertEquals(1500,finance.outstandingSale(draft.id()));
     assertEquals(1,finance.pendingCollectionCount());
     assertEquals(0,TenantContext.call(new TenantScope(t,c,otherBranch,a),1,finance::pendingCollectionCount));
     var deposit=tenders.deposit(new TenderService.Deposit(UUID.randomUUID(),customer,shift.id(),date,"2500","Customer cash advance"));
     tenders.allocate(deposit.depositId(),new TenderService.Allocation(UUID.randomUUID(),draft.id(),null,date,"1500","Allocate advance to debt"));assertEquals(0,finance.outstandingSale(draft.id()));
     assertEquals(0,finance.pendingCollectionCount());
     tenders.refund(deposit.depositId(),new TenderService.Allocation(UUID.randomUUID(),null,shift.id(),date,"500","Approved cash refund"));
     assertEquals("500",tenders.deposits().getFirst().remainingXaf());assertEquals("2600",tenders.sessions().getFirst().expectedXaf());
     assertThrows(IllegalArgumentException.class,()->tenders.refund(deposit.depositId(),new TenderService.Allocation(UUID.randomUUID(),null,shift.id(),date,"501","Excess refund rejected")));
     assertEquals(4,tenders.facts().size());assertEquals(5,count(owner,"select count(*) from journal_entry"));assertEquals(0,count(owner,"select sum(debit_xaf-credit_xaf) from journal_line"));
     assertThrows(RevisionConflict.class,()->tenders.close(shift.id(),new TenderService.Close(UUID.randomUUID(),shift.version(),date,"2600","Stale session review rejected")));
     var closed=tenders.close(shift.id(),new TenderService.Close(UUID.randomUUID(),tenders.sessions().getFirst().version(),date,"2600","Exact till reconciliation"));assertEquals("CLOSED",closed.status());
     assertThrows(IllegalStateException.class,()->tenders.deposit(new TenderService.Deposit(UUID.randomUUID(),customer,shift.id(),date,"100","Cannot receive into closed till")));assertEquals(4,tenders.facts().size());
     return null;
    });
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static void sql(Connection c,String query)throws SQLException {try(var s=c.createStatement()){s.execute(query);}}
 static long count(Connection c,String query)throws SQLException {try(var s=c.createStatement();var rs=s.executeQuery(query)){rs.next();return rs.getLong(1);}}
}
