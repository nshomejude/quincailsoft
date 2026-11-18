package com.quincaisoft.sales;
import com.quincaisoft.sales.api.SalesService;
import com.quincaisoft.sales.api.FulfillmentService;
import com.quincaisoft.sales.application.DefaultFulfillmentService;
import com.quincaisoft.inventory.api.InventoryCustody;
import com.quincaisoft.inventory.application.DefaultInventoryCustody;
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
class PostgresSaleIntegrationTest {
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
    var ds=new DriverManagerDataSource(url,role,"");factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.documents.infrastructure","com.quincaisoft.sales.infrastructure","com.quincaisoft.finance.infrastructure","com.quincaisoft.inventory.infrastructure","com.quincaisoft.catalog.infrastructure","com.quincaisoft.customeraccounts.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var catalog=new DefaultCatalogService(em,tx);var stock=new DefaultStockService(em,tx,catalog);var finance=new DefaultFinanceService(em,tx);var customers=new DefaultCustomerService(em,tx);var documents=new DefaultDocumentService(em,tx,JsonMapper.builder().build());var sales=new DefaultSalesService(em,tx,stock,catalog,finance,customers,JsonMapper.builder().build(),documents);
    var scope=new TenantScope(t,c,b,a);
    TenantContext.call(scope,1,()->{
     var draftRequest=new SalesService.Draft(UUID.randomUUID(),w,customer,LocalDate.of(2026,11,18),List.of(new SalesService.RequestedLine(p,"2000")));
     var draft=sales.draft(draftRequest);assertEquals("DRAFT",draft.status());assertEquals("2000",draft.totalXaf());
     var postRequest=new SalesService.Post(UUID.randomUUID(),draft.version(),"Synthetic acceptance posting");assertThrows(IllegalStateException.class,()->sales.post(draft.id(),postRequest));assertEquals(10000,count(owner,"select quantity_milli from stock_balance"));assertEquals(0,count(owner,"select count(*) from journal_entry"));assertEquals(0,count(owner,"select count(*) from document_series"));documents.configure(new DocumentService.Settings("Synthetic merchant","Synthetic address","TEST-REG","TEST","en",0));var posted=sales.post(draft.id(),postRequest);assertEquals("POSTED",posted.status());assertEquals(posted,sales.post(draft.id(),postRequest));assertEquals(1,documents.list(draft.id()).size());assertTrue(documents.render(documents.list(draft.id()).getFirst().id(),true).contains("COPY"));var original=documents.list(draft.id()).getFirst();documents.configure(new DocumentService.Settings("Changed issuer","Changed address","REG-NEW","NEW","fr",1));assertEquals(original,documents.get(original.id()));assertTrue(documents.render(original.id(),false).contains("Synthetic merchant"));assertThrows(SQLException.class,()->sql(owner,"update document_snapshot set content='tampered'"));assertThrows(NoSuchElementException.class,()->TenantContext.call(new TenantScope(UUID.randomUUID(),c,b,a),1,()->documents.get(original.id())));assertEquals(draft,sales.draft(draftRequest));
     assertEquals(8000,count(owner,"select quantity_milli from stock_balance"));assertEquals(4000,count(owner,"select value_xaf from stock_balance"));assertEquals(1,count(owner,"select count(*) from journal_entry"));assertEquals(4,count(owner,"select count(*) from journal_line"));assertEquals(1,count(owner,"select count(*) from outbox_event"));assertEquals(0,count(owner,"select sum(debit_xaf-credit_xaf) from journal_line"));
     assertThrows(IllegalStateException.class,()->sales.post(draft.id(),new SalesService.Post(postRequest.operationId(),draft.version(),"Different request must conflict")));
     var excessive=sales.draft(new SalesService.Draft(UUID.randomUUID(),w,customer,LocalDate.of(2026,11,18),List.of(new SalesService.RequestedLine(p,"100000"))));
     assertThrows(IllegalArgumentException.class,()->sales.post(excessive.id(),new SalesService.Post(UUID.randomUUID(),excessive.version(),"Oversell must rollback")));
     assertEquals("DRAFT",sales.get(excessive.id()).status());assertEquals(8000,count(owner,"select quantity_milli from stock_balance"));
     var day = LocalDate.of(2026, 11, 18);
     assertEquals("2000", sales.postedSalesSummary(day, day).totalXaf());
     assertEquals("0", sales.postedSalesSummary(day.minusDays(1), day.minusDays(1)).totalXaf());
     assertThrows(IllegalArgumentException.class, () -> sales.postedSalesSummary(day, day.plusDays(366)));
     assertEquals("0", TenantContext.call(new TenantScope(UUID.randomUUID(), c, b, a), 1,
         () -> sales.postedSalesSummary(day, day).totalXaf()));
     assertEquals("0", TenantContext.call(new TenantScope(t, c, UUID.randomUUID(), a), 1,
         () -> sales.postedSalesSummary(day, day).totalXaf()));
     // Closed period fails after stock issue in memory; whole transaction rolls it back.
     var next=sales.draft(new SalesService.Draft(UUID.randomUUID(),w,customer,LocalDate.of(2026,11,18),List.of(new SalesService.RequestedLine(p,"1000"))));
     sql(owner,"update financial_period set status='Closed'");
     assertThrows(IllegalStateException.class,()->sales.post(next.id(),new SalesService.Post(UUID.randomUUID(),next.version(),"Closed period must rollback")));
     assertEquals("DRAFT",sales.get(next.id()).status());assertEquals(8000,count(owner,"select quantity_milli from stock_balance"));assertEquals(1,count(owner,"select count(*) from stock_movement"));assertEquals(1,count(owner,"select count(*) from journal_entry"));assertEquals(1,count(owner,"select count(*) from outbox_event"));assertEquals(1,count(owner,"select count(*) from document_snapshot"));
     sql(owner,"update financial_period set status='Open'");var custody=new DefaultInventoryCustody(em,tx);var fulfillment=new DefaultFulfillmentService(em,tx,sales,stock,finance);
     var heldDraft=sales.draft(new SalesService.Draft(UUID.randomUUID(),w,customer,LocalDate.of(2026,11,18),List.of(new SalesService.RequestedLine(p,"2000"))));
     var heldPost=new SalesService.Post(UUID.randomUUID(),heldDraft.version(),"Recognize title while customer goods stay here","SALE_WITH_CUSTOMER_CUSTODY");
     assertThrows(IllegalStateException.class,()->sales.post(heldDraft.id(),heldPost));assertEquals(8000,count(owner,"select quantity_milli from stock_balance"));assertEquals(0,count(owner,"select count(*) from customer_custody"));
     var policy=custody.requestPolicy(new InventoryCustody.PolicyRequest(UUID.randomUUID(),"SALE_WITH_CUSTOMER_CUSTODY",LocalDate.of(2026,11,18),"FINANCE-CONTRACT-ACCEPTANCE","Enable approved issuing contract"));var policyApproval=new InventoryCustody.Approve(UUID.randomUUID(),policy.version(),"Finance independently reviewed contract");assertThrows(IllegalStateException.class,()->custody.approvePolicy(policy.id(),policyApproval));
     var approvedPolicy=TenantContext.call(new TenantScope(t,c,b,UUID.randomUUID()),1,()->custody.approvePolicy(policy.id(),policyApproval));assertEquals("APPROVED",approvedPolicy.status());
     var heldSale=sales.post(heldDraft.id(),heldPost);assertEquals("SALE_WITH_CUSTOMER_CUSTODY",heldSale.recognitionPolicy());assertEquals(heldSale,sales.post(heldDraft.id(),heldPost));assertEquals(6000,count(owner,"select quantity_milli from stock_balance"));assertEquals(3000,count(owner,"select value_xaf from stock_balance"));assertEquals(2000,count(owner,"select sum(quantity_milli-collected_milli) from customer_custody"));assertEquals(8000,count(owner,"select (select sum(quantity_milli) from stock_balance)+(select sum(quantity_milli-collected_milli) from customer_custody)"));
     long journalCount=count(owner,"select count(*) from journal_entry"),movementCount=count(owner,"select count(*) from stock_movement");
     var position=custody.positions().getFirst();assertEquals("1000",position.frozenCostXaf());assertThrows(IllegalStateException.class,()->stock.requireCustodyCollected(heldSale.id()));
     assertEquals(0,fulfillment.pendingPreparationCount());
     var date=LocalDate.of(2026,11,18);
     var delivery=new FulfillmentService.DeliveryDetails("DELIVERY",date.plusDays(1),
         "Synthetic address","Synthetic recipient","+237000000000","Synthetic instructions");
     var request=new FulfillmentService.Draft(UUID.randomUUID(),heldSale.id(),customer,date,"Prepare customer collection",delivery);
     var task=fulfillment.draft(request);
     assertEquals(delivery,fulfillment.get(task.id()).delivery());
     assertEquals(task,fulfillment.draft(request));
    assertThrows(IllegalStateException.class,()->fulfillment.dispatch(task.id(),new FulfillmentService.Action(UUID.randomUUID(),task.version(),null,"Must collect custody first")));
     assertThrows(IllegalArgumentException.class,()->custody.collect(position.id(),new InventoryCustody.Collect(UUID.randomUUID(),position.version(),"1000"," ","Missing proof must reject")));
     assertEquals(1,fulfillment.pendingPreparationCount());
     var pickList=fulfillment.pickList(task.id());
     assertEquals(task.id(),pickList.fulfillmentId());
     assertEquals(heldSale.id(),pickList.saleId());
     assertEquals(w,pickList.warehouseId());
     assertEquals("PENDING",pickList.status());
     assertEquals(List.of(new FulfillmentService.PickLine(p,"2000")),pickList.lines());
     assertThrows(NoSuchElementException.class,()->TenantContext.call(
         new TenantScope(t,c,UUID.randomUUID(),a),1,()->fulfillment.pickList(task.id())));
     assertEquals(0,TenantContext.call(new TenantScope(t,c,UUID.randomUUID(),a),1,fulfillment::pendingPreparationCount));
     var collection=new InventoryCustody.Collect(UUID.randomUUID(),position.version(),"1000","CUSTOMER-SIGNED-PICKUP-1","Partial collection");var partial=custody.collect(position.id(),collection);assertEquals("PARTIALLY_COLLECTED",partial.status());assertEquals(partial,custody.collect(position.id(),collection));
     assertThrows(IllegalArgumentException.class,()->custody.collect(position.id(),new InventoryCustody.Collect(UUID.randomUUID(),partial.version(),"2000","EXCESS","Excess collection")));
     assertThrows(RevisionConflict.class,()->custody.collect(position.id(),new InventoryCustody.Collect(UUID.randomUUID(),position.version(),"1000","STALE","Stale collection")));
     assertEquals("COLLECTED",custody.collect(position.id(),new InventoryCustody.Collect(UUID.randomUUID(),partial.version(),"1000","CUSTOMER-SIGNED-PICKUP-2","Final collection")).status());assertEquals(2,custody.collections(position.id()).size());
     assertEquals(6000,count(owner,"select quantity_milli from stock_balance"));assertEquals(3000,count(owner,"select value_xaf from stock_balance"));assertEquals(journalCount,count(owner,"select count(*) from journal_entry"));assertEquals(movementCount,count(owner,"select count(*) from stock_movement"));assertEquals(0,count(owner,"select sum(quantity_milli-collected_milli) from customer_custody"));
     var firstDispatch=new FulfillmentService.DispatchLines(UUID.randomUUID(),task.version(),"First dispatch",
         List.of(new FulfillmentService.PickLine(p,"500")));
     var first=fulfillment.dispatchLines(task.id(),firstDispatch);
     assertEquals("PARTIAL",first.status());
     assertEquals(first,fulfillment.dispatchLines(task.id(),firstDispatch));
     assertThrows(RevisionConflict.class,()->fulfillment.dispatchLines(task.id(),
         new FulfillmentService.DispatchLines(UUID.randomUUID(),task.version(),"Stale dispatch",firstDispatch.lines())));
     assertThrows(IllegalArgumentException.class,()->fulfillment.dispatchLines(task.id(),
         new FulfillmentService.DispatchLines(UUID.randomUUID(),first.version(),"Excess dispatch",
             List.of(new FulfillmentService.PickLine(p,"1501")))));
     var second=fulfillment.dispatchLines(task.id(),new FulfillmentService.DispatchLines(
         UUID.randomUUID(),first.version(),"Second dispatch",firstDispatch.lines()));
     assertEquals("PARTIAL",second.status());
     assertNotEquals(first.version(),second.version());
     var dispatched=fulfillment.dispatchLines(task.id(),new FulfillmentService.DispatchLines(
         UUID.randomUUID(),second.version(),"Final dispatch",List.of(new FulfillmentService.PickLine(p,"1000"))));
     assertEquals("DISPATCHED",dispatched.status());
     assertEquals(3,count(owner,"select count(*) from fulfillment_dispatch_line"));
     assertEquals(2000,count(owner,"select sum(quantity_milli) from fulfillment_dispatch_line"));
     assertEquals(journalCount,count(owner,"select count(*) from journal_entry"));
     assertEquals(movementCount,count(owner,"select count(*) from stock_movement"));
     assertThrows(IllegalArgumentException.class,()->fulfillment.complete(task.id(),new FulfillmentService.Action(UUID.randomUUID(),dispatched.version()," ","Missing proof")));var completion=new FulfillmentService.Action(UUID.randomUUID(),dispatched.version(),"SIGNED-DELIVERY","Complete delivery");var completed=fulfillment.complete(task.id(),completion);assertEquals("COMPLETED",completed.status());assertEquals(completed,fulfillment.complete(task.id(),completion));
     assertEquals(0,count(owner,"""
         select count(*) from outbox_event
         where event_type='FulfillmentChanged.v1' and
           (payload ? 'command' or payload ? 'fulfillment' or payload::text like '%SIGNED-DELIVERY%' or payload::text like '%Synthetic recipient%')
         """));
     assertEquals(0,fulfillment.pendingPreparationCount());
     assertTrue(TenantContext.call(new TenantScope(t,c,UUID.randomUUID(),a),1,custody::positions).isEmpty());assertTrue(TenantContext.call(new TenantScope(UUID.randomUUID(),c,b,a),1,fulfillment::list).isEmpty());assertThrows(SQLException.class,()->sql(owner,"update customer_custody set frozen_cost_xaf=0"));assertThrows(SQLException.class,()->sql(owner,"update custody_collection set quantity_milli=1"));
     return null;
    });
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static void sql(Connection c,String query)throws SQLException {try(var s=c.createStatement()){s.execute(query);}}
 static long count(Connection c,String query)throws SQLException {try(var s=c.createStatement();var rs=s.executeQuery(query)){rs.next();return rs.getLong(1);}}
}
