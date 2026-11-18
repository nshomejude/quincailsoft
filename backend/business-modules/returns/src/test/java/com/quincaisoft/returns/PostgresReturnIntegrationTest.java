package com.quincaisoft.returns;
import com.quincaisoft.returns.api.ReturnService;
import com.quincaisoft.returns.application.DefaultReturnService;
import com.quincaisoft.quotations.api.QuotationService;
import com.quincaisoft.quotations.application.DefaultQuotationService;
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
class PostgresReturnIntegrationTest {
 @Test void stockJournalsOutboxAndReplayCommitOrRollbackTogether() throws Exception {
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
    var ds=new DriverManagerDataSource(url,role,"");factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.returns.infrastructure","com.quincaisoft.quotations.infrastructure","com.quincaisoft.documents.infrastructure","com.quincaisoft.sales.infrastructure","com.quincaisoft.finance.infrastructure","com.quincaisoft.inventory.infrastructure","com.quincaisoft.catalog.infrastructure","com.quincaisoft.customeraccounts.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var catalog=new DefaultCatalogService(em,tx);var stock=new DefaultStockService(em,tx,catalog);var finance=new DefaultFinanceService(em,tx);var customers=new DefaultCustomerService(em,tx);var documents=new DefaultDocumentService(em,tx,JsonMapper.builder().build());var sales=new DefaultSalesService(em,tx,stock,catalog,finance,customers,JsonMapper.builder().build(),documents);
    var returns=new DefaultReturnService(em,tx,sales,finance,stock,catalog,documents,JsonMapper.builder().build());var quotes=new DefaultQuotationService(em,tx,sales,catalog,customers,stock,documents,JsonMapper.builder().build());var scope=new TenantScope(t,c,b,a);
    TenantContext.call(scope,1,()->{
     documents.configure(new DocumentService.Settings("Synthetic merchant","Synthetic address","TEST-REG","TEST","en",0));
     var draftRequest=new SalesService.Draft(UUID.randomUUID(),w,customer,LocalDate.of(2026,11,18),List.of(new SalesService.RequestedLine(p,"2000")));
     var draft=sales.draft(draftRequest);assertEquals("DRAFT",draft.status());assertEquals("2000",draft.totalXaf());
     var postRequest=new SalesService.Post(UUID.randomUUID(),draft.version(),"Synthetic acceptance posting");var posted=sales.post(draft.id(),postRequest);assertEquals("POSTED",posted.status());assertEquals(posted,sales.post(draft.id(),postRequest));assertEquals(1,documents.list(draft.id()).size());assertTrue(documents.render(documents.list(draft.id()).getFirst().id(),true).contains("COPY"));assertEquals(draft,sales.draft(draftRequest));
     assertEquals(8000,count(owner,"select quantity_milli from stock_balance"));assertEquals(4000,count(owner,"select value_xaf from stock_balance"));assertEquals(1,count(owner,"select count(*) from journal_entry"));assertEquals(4,count(owner,"select count(*) from journal_line"));assertEquals(1,count(owner,"select count(*) from outbox_event"));assertEquals(0,count(owner,"select sum(debit_xaf-credit_xaf) from journal_line"));
     assertThrows(IllegalStateException.class,()->sales.post(draft.id(),new SalesService.Post(postRequest.operationId(),draft.version(),"Different request must conflict")));
     var quote=quotes.draft(new QuotationService.Draft(UUID.randomUUID(),customer,w,LocalDate.now(),LocalDate.now().plusDays(10),List.of(new SalesService.RequestedLine(p,"1000"))));
     quote=quotes.issue(quote.id(),new QuotationService.Command(UUID.randomUUID(),quote.version(),"Issue synthetic quote"));assertEquals("ISSUED",quote.status());
     var convert=new QuotationService.Command(UUID.randomUUID(),quote.version(),"Accept synthetic quote");var converted=quotes.convert(quote.id(),convert);assertEquals("CONVERTED",converted.status());assertEquals(converted,quotes.convert(quote.id(),convert));assertNotNull(converted.convertedSaleId());
     var returnDraft=new ReturnService.Draft(UUID.randomUUID(),posted.id(),LocalDate.of(2026,11,18),"SELLABLE","inspection-1",List.of(new ReturnService.RequestedLine(p,"1000")));
     var ret=returns.draft(returnDraft);assertEquals(ret,returns.draft(returnDraft));
     var reviewerScope=new TenantScope(t,c,b,UUID.randomUUID());
     TenantContext.call(reviewerScope,1,()->{assertThrows(IllegalStateException.class,()->returns.draft(returnDraft));return null;});
     TenantContext.call(new TenantScope(t,c,UUID.randomUUID(),a),1,()->{assertEquals("Sale not found",assertThrows(NoSuchElementException.class,()->returns.draft(returnDraft)).getMessage());return null;});
     TenantContext.call(new TenantScope(t,UUID.randomUUID(),b,a),1,()->{assertEquals("Sale not found",assertThrows(NoSuchElementException.class,()->returns.draft(returnDraft)).getMessage());return null;});
     assertThrows(IllegalStateException.class,()->returns.draft(new ReturnService.Draft(returnDraft.operationId(),posted.id(),returnDraft.businessDate(),"SELLABLE","Changed evidence",returnDraft.lines())));
     assertEquals(1,count(owner,"select count(*) from customer_return"));
     var approval=new ReturnService.Command(UUID.randomUUID(),ret.version(),"Independent inspection approval");assertThrows(IllegalStateException.class,()->returns.approve(ret.id(),approval));
     var approved=TenantContext.call(reviewerScope,1,()->{var result=returns.approve(ret.id(),approval);assertEquals(result,returns.approve(ret.id(),approval));return result;});
     assertThrows(IllegalStateException.class,()->returns.approve(ret.id(),approval));
     var postReturn=new ReturnService.Command(UUID.randomUUID(),approved.version(),"Accept returned goods and credit");var credited=returns.post(approved.id(),postReturn);assertEquals("POSTED",credited.status());assertEquals("1000",credited.creditXaf());assertEquals(credited,returns.post(approved.id(),postReturn));
     TenantContext.call(reviewerScope,1,()->{assertThrows(IllegalStateException.class,()->returns.post(approved.id(),postReturn));return null;});
     TenantContext.call(new TenantScope(t,c,UUID.randomUUID(),a),1,()->{assertEquals("Return not found",assertThrows(NoSuchElementException.class,()->returns.post(approved.id(),postReturn)).getMessage());return null;});
     assertEquals(9000,count(owner,"select quantity_milli from stock_balance"));assertEquals(4500,count(owner,"select value_xaf from stock_balance"));assertEquals(2,count(owner,"select count(*) from journal_entry"));assertEquals(3,count(owner,"select count(*) from document_snapshot"));assertEquals(1000,finance.outstandingSale(posted.id()));
     var excess=returns.draft(new ReturnService.Draft(UUID.randomUUID(),posted.id(),LocalDate.of(2026,11,18),"SELLABLE","inspection-2",List.of(new ReturnService.RequestedLine(p,"2000"))));
     var excessApproved=TenantContext.call(new TenantScope(t,c,b,UUID.randomUUID()),1,()->returns.approve(excess.id(),new ReturnService.Command(UUID.randomUUID(),excess.version(),"Independent approval")));
     assertThrows(IllegalStateException.class,()->returns.post(excessApproved.id(),new ReturnService.Command(UUID.randomUUID(),excessApproved.version(),"Must reject excess")));assertEquals(9000,count(owner,"select quantity_milli from stock_balance"));
     return null;
    });
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static void sql(Connection c,String query)throws SQLException {try(var s=c.createStatement()){s.execute(query);}}
 static long count(Connection c,String query)throws SQLException {try(var s=c.createStatement();var rs=s.executeQuery(query)){rs.next();return rs.getLong(1);}}
}
