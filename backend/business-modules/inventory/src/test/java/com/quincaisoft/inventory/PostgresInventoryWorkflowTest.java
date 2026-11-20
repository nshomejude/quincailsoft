package com.quincaisoft.inventory;
import com.quincaisoft.finance.application.DefaultFinanceService;
import com.quincaisoft.shared.*;
import com.quincaisoft.catalog.api.CatalogService;
import com.quincaisoft.inventory.api.InventoryWorkflows;
import com.quincaisoft.inventory.api.InventoryQuality;
import com.quincaisoft.inventory.application.DefaultInventoryQuality;
import com.quincaisoft.inventory.api.InventoryJobs;
import com.quincaisoft.inventory.application.DefaultInventoryJobs;
import com.quincaisoft.inventory.application.DefaultInventoryWorkflows;
import com.quincaisoft.catalog.application.DefaultCatalogService;
import com.quincaisoft.inventory.application.DefaultStockService;
import com.quincaisoft.finance.api.FinanceService;
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
class PostgresInventoryWorkflowTest {
 @Test void catalogAndApprovedPartialTransfersPreserveScopeVersionAndValue() throws Exception {
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
    var ds=new DriverManagerDataSource(url,role,"");factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.finance.infrastructure","com.quincaisoft.inventory.infrastructure","com.quincaisoft.catalog.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var catalog=new DefaultCatalogService(em,tx);var stock=new DefaultStockService(em,tx,catalog);var finance=new DefaultFinanceService(em,tx);var inventory=new DefaultInventoryWorkflows(em,tx,stock,catalog,finance);var jobs=new DefaultInventoryJobs(em,tx,stock,catalog,finance);
    UUID w2=UUID.randomUUID(),approver=UUID.randomUUID();sql(owner,"INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name)VALUES('"+t+"','"+c+"','"+b+"','"+w2+"','Destination')");
    var scope=new TenantScope(t,c,b,a);var reviewer=new TenantScope(t,c,b,approver);
    TenantContext.call(scope,1,()->{
     var create=new CatalogService.Save(UUID.randomUUID(),"NEW-SKU","New product","piece","250",0,null,true,"Create product");
     var product=catalog.create(create);assertEquals(product,catalog.create(create));assertEquals(2,catalog.list().size());
     var edit=new CatalogService.Save(UUID.randomUUID(),"NEW-SKU","Updated product","piece","300",0,Long.toString(product.version()),true,"Correct price");var updated=catalog.update(product.id(),edit);assertEquals(300,updated.sellingPriceXaf());assertEquals(updated,catalog.update(product.id(),edit));assertEquals(product,catalog.create(create));
     assertThrows(RevisionConflict.class,()->catalog.update(product.id(),new CatalogService.Save(UUID.randomUUID(),"NEW-SKU","Stale","piece","300",0,Long.toString(product.version()),true,"Reject stale edit")));
     assertThrows(IllegalArgumentException.class,()->catalog.update(product.id(),new CatalogService.Save(UUID.randomUUID(),"NEW-SKU","Wrong unit","kg","300",0,Long.toString(updated.version()),true,"Reject unit change")));
     assertEquals(2,inventory.warehouses().size());assertEquals(10000,inventory.balances(w).getFirst().quantityMilli());
     var request=new InventoryWorkflows.Draft(UUID.randomUUID(),w,w2,p,"4000",LocalDate.of(2026,11,18),"Move stock to destination");
     var draft=inventory.draft(request);assertEquals(draft,inventory.draft(request));
     assertThrows(IllegalStateException.class,()->TenantContext.call(reviewer,1,()->inventory.draft(request)));
     assertThrows(IllegalArgumentException.class,()->TenantContext.call(new TenantScope(t,c,UUID.randomUUID(),a),1,()->inventory.draft(request)));
     assertThrows(IllegalStateException.class,()->inventory.draft(new InventoryWorkflows.Draft(request.operationId(),w,w2,p,"5000",request.businessDate(),request.reason())));
     assertEquals(1,count(owner,"select count(*) from inventory_transfer"));
     var approval=new InventoryWorkflows.Command(UUID.randomUUID(),draft.version(),null,"Reviewed transfer");
     assertThrows(IllegalStateException.class,()->inventory.approve(draft.id(),approval));
     var approved=TenantContext.call(reviewer,1,()->inventory.approve(draft.id(),approval));assertEquals("APPROVED",approved.status());
     assertEquals(approved,TenantContext.call(reviewer,1,()->inventory.approve(draft.id(),approval)));
     assertThrows(IllegalStateException.class,()->inventory.approve(draft.id(),approval));
     var dispatch=new InventoryWorkflows.Command(UUID.randomUUID(),approved.version(),null,"Dispatched with proof");InventoryWorkflows.Transfer transit;
     try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
      var start=new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.Callable<InventoryWorkflows.Transfer> retry=()->{start.await();return TenantContext.call(scope,1,()->inventory.dispatch(draft.id(),dispatch));};
      var first=pool.submit(retry);var second=pool.submit(retry);start.countDown();
      transit=first.get(10,java.util.concurrent.TimeUnit.SECONDS);assertEquals(transit,second.get(10,java.util.concurrent.TimeUnit.SECONDS));
     }
     assertThrows(IllegalStateException.class,()->TenantContext.call(reviewer,1,()->inventory.dispatch(draft.id(),dispatch)));assertEquals("IN_TRANSIT",transit.status());assertEquals("2000",transit.valueXaf());assertEquals(transit,inventory.dispatch(draft.id(),dispatch));
     assertEquals(6000,inventory.balances(w).getFirst().quantityMilli());assertTrue(inventory.balances(w2).isEmpty());
     assertThrows(IllegalArgumentException.class,()->inventory.receive(draft.id(),new InventoryWorkflows.Command(UUID.randomUUID(),transit.version(),"5000","Reject excess")));
     var receive=new InventoryWorkflows.Command(UUID.randomUUID(),transit.version(),"1000","Partial receipt");var partial=inventory.receive(draft.id(),receive);assertEquals("PARTIALLY_RECEIVED",partial.status());assertEquals("500",partial.receivedValueXaf());assertEquals(partial,inventory.receive(draft.id(),receive));
     assertThrows(IllegalStateException.class,()->TenantContext.call(reviewer,1,()->inventory.receive(draft.id(),receive)));
     assertEquals(2,count(owner,"select count(*) from stock_movement"));
     sql(owner,"update financial_period set status='Closed'");assertThrows(IllegalStateException.class,()->inventory.receive(draft.id(),new InventoryWorkflows.Command(UUID.randomUUID(),partial.version(),"3000","Closed period rejection")));assertEquals(1000,inventory.balances(w2).getFirst().quantityMilli());
     sql(owner,"update financial_period set status='Open'");var received=inventory.receive(draft.id(),new InventoryWorkflows.Command(UUID.randomUUID(),partial.version(),"3000","Final receipt"));assertEquals("RECEIVED",received.status());assertEquals("2000",received.receivedValueXaf());assertEquals(4000,inventory.balances(w2).getFirst().quantityMilli());assertEquals(2000,inventory.balances(w2).getFirst().valueXaf());assertEquals(3,count(owner,"select count(*) from stock_movement"));
     var count=jobs.startCount(new InventoryJobs.CountStart(UUID.randomUUID(),w,p,LocalDate.of(2026,11,18),"Stock count snapshot"));
     var captured=jobs.capture(count.id(),new InventoryJobs.Action(UUID.randomUUID(),count.version(),"5000","COUNT-PROOF","Physical measurement"));
     assertThrows(IllegalStateException.class,()->jobs.approve(count.id(),new InventoryJobs.Action(UUID.randomUUID(),captured.version(),null,null,"Cannot self approve")));
     var countApproved=TenantContext.call(reviewer,1,()->jobs.approve(count.id(),new InventoryJobs.Action(UUID.randomUUID(),captured.version(),null,null,"Reviewed count")));
     var countPost=new InventoryJobs.Action(UUID.randomUUID(),countApproved.version(),null,null,"Post count loss");var counted=jobs.post(count.id(),countPost);assertEquals("POSTED",counted.status());assertEquals("500",counted.postedValueXaf());assertEquals(counted,jobs.post(count.id(),countPost));assertThrows(IllegalStateException.class,()->TenantContext.call(reviewer,1,()->jobs.post(count.id(),countPost)));assertEquals(5000,inventory.balances(w).getFirst().quantityMilli());assertEquals(1,count(owner,"select count(*) from journal_entry"));
     var stale=jobs.startCount(new InventoryJobs.CountStart(UUID.randomUUID(),w,p,LocalDate.of(2026,11,18),"Snapshot before movement"));var staleCaptured=jobs.capture(stale.id(),new InventoryJobs.Action(UUID.randomUUID(),stale.version(),"5000","STALE-PROOF","Capture count"));var staleApproved=TenantContext.call(reviewer,1,()->jobs.approve(stale.id(),new InventoryJobs.Action(UUID.randomUUID(),staleCaptured.version(),null,null,"Review count")));stock.receive(w,p,1000,500,UUID.randomUUID(),"Movement after count snapshot");assertThrows(IllegalStateException.class,()->jobs.post(stale.id(),new InventoryJobs.Action(UUID.randomUUID(),staleApproved.version(),null,null,"Reject stale count")));
     var cut=jobs.draftTransform("CUT",new InventoryJobs.Transform(UUID.randomUUID(),w,p,product.id(),"1000","2000","6000","3000",LocalDate.of(2026,11,18),"CUT-MEASURE","Zero loss cut"));var cutApproved=TenantContext.call(reviewer,1,()->jobs.approve(cut.id(),new InventoryJobs.Action(UUID.randomUUID(),cut.version(),null,null,"Review cut evidence")));var cutPosted=jobs.post(cut.id(),new InventoryJobs.Action(UUID.randomUUID(),cutApproved.version(),null,null,"Post cut"));assertEquals("500",cutPosted.postedValueXaf());assertEquals(2000,inventory.balances(w).stream().filter(x->x.productId().equals(product.id())).findFirst().orElseThrow().quantityMilli());
     assertThrows(IllegalArgumentException.class,()->jobs.draftTransform("CUT",new InventoryJobs.Transform(UUID.randomUUID(),w,p,product.id(),"1000","3000","6000","3000",LocalDate.of(2026,11,18),"INVALID-CUT","Reject material creation")));
     var repack=jobs.draftTransform("REPACK",new InventoryJobs.Transform(UUID.randomUUID(),w,product.id(),p,"2000","1000",null,null,LocalDate.of(2026,11,18),"REPACK-RECIPE","Repack the full output"));var repackApproved=TenantContext.call(reviewer,1,()->jobs.approve(repack.id(),new InventoryJobs.Action(UUID.randomUUID(),repack.version(),null,null,"Review repack")));var repacked=jobs.post(repack.id(),new InventoryJobs.Action(UUID.randomUUID(),repackApproved.version(),null,null,"Post repack"));assertEquals("500",repacked.postedValueXaf());assertEquals(6000,inventory.balances(w).stream().filter(x->x.productId().equals(p)).findFirst().orElseThrow().quantityMilli());assertEquals(3000,inventory.balances(w).stream().filter(x->x.productId().equals(p)).findFirst().orElseThrow().valueXaf());
     var quality=new DefaultInventoryQuality(em,tx,stock,finance);
     var holdRequest=new InventoryQuality.Reserve(UUID.randomUUID(),w,p,"2000","ORDER-RESERVATION","Reserve central stock");var hold=quality.reserve(holdRequest);assertEquals(hold,quality.reserve(holdRequest));assertThrows(IllegalStateException.class,()->TenantContext.call(reviewer,1,()->quality.reserve(holdRequest)));assertEquals(4000,inventory.balances(w).stream().filter(x->x.productId().equals(p)).findFirst().orElseThrow().availableMilli());
     assertThrows(IllegalArgumentException.class,()->stock.issue(w,p,5000,UUID.randomUUID(),"Cannot consume reservation"));
     var release=new InventoryQuality.Release(UUID.randomUUID(),hold.version(),"1000","Partial reservation release");var partialHold=quality.release(hold.id(),release);assertEquals(partialHold,quality.release(hold.id(),release));assertEquals("1000",partialHold.releasedMilli());
     assertThrows(RevisionConflict.class,()->quality.release(hold.id(),new InventoryQuality.Release(UUID.randomUUID(),hold.version(),"1000","Reject stale release")));
     assertEquals("RELEASED",quality.release(hold.id(),new InventoryQuality.Release(UUID.randomUUID(),partialHold.version(),"1000","Release remainder")).status());
     var lotRequest=new InventoryQuality.RegisterLot(UUID.randomUUID(),w,p,"LOT-ACCEPTANCE","2500",LocalDate.of(2027,1,1),"RECEIPT-RECONCILED","Classify existing stock");var lot=quality.registerLot(lotRequest);assertEquals(lot,quality.registerLot(lotRequest));assertEquals(6000,inventory.balances(w).stream().filter(x->x.productId().equals(p)).findFirst().orElseThrow().quantityMilli());
     assertThrows(IllegalArgumentException.class,()->quality.registerLot(new InventoryQuality.RegisterLot(UUID.randomUUID(),w,p,"EXCESS","4000",null,"BAD","Cannot fabricate lot stock")));
     var recall=quality.draftRecall(new InventoryQuality.DraftRecall(UUID.randomUUID(),lot.id(),"1000",LocalDate.of(2026,11,18),"Supplier safety recall"));
     var recallApproval=new InventoryQuality.Action(UUID.randomUUID(),recall.version(),null,"Independent inspection");assertThrows(IllegalStateException.class,()->quality.approve(recall.id(),recallApproval));var approvedRecall=TenantContext.call(reviewer,1,()->quality.approve(recall.id(),recallApproval));
     var blockCommand=new InventoryQuality.Action(UUID.randomUUID(),approvedRecall.version(),null,"Block affected goods");var blocked=quality.block(recall.id(),blockCommand);assertEquals(blocked,quality.block(recall.id(),blockCommand));assertEquals("QUARANTINED",blocked.status());assertEquals(1000,inventory.balances(w).stream().filter(x->x.productId().equals(p)).findFirst().orElseThrow().quarantinedMilli());
     assertThrows(IllegalStateException.class,()->stock.issue(w,p,4000,UUID.randomUUID(),"Cannot silently issue traced lot"));
     assertThrows(IllegalArgumentException.class,()->quality.dispose(recall.id(),new InventoryQuality.Action(UUID.randomUUID(),blocked.version()," ","Missing disposal proof")));
     sql(owner,"update financial_period set status='Closed'");assertThrows(IllegalStateException.class,()->quality.dispose(recall.id(),new InventoryQuality.Action(UUID.randomUUID(),blocked.version(),"DISPOSAL","Closed period disposal")));assertEquals("QUARANTINED",quality.recalls().getFirst().status());sql(owner,"update financial_period set status='Open'");
     var disposeCommand=new InventoryQuality.Action(UUID.randomUUID(),blocked.version(),"WITNESSED-DISPOSAL","Dispose recalled goods");var disposed=quality.dispose(recall.id(),disposeCommand);assertEquals(disposed,quality.dispose(recall.id(),disposeCommand));assertEquals("DISPOSED",disposed.status());assertEquals("1500",quality.lots().getFirst().quantityMilli());assertEquals(5000,inventory.balances(w).stream().filter(x->x.productId().equals(p)).findFirst().orElseThrow().quantityMilli());assertEquals(2500,inventory.balances(w).stream().filter(x->x.productId().equals(p)).findFirst().orElseThrow().valueXaf());
     var recall2=quality.draftRecall(new InventoryQuality.DraftRecall(UUID.randomUUID(),lot.id(),"500",LocalDate.of(2026,11,18),"Second inspection"));var approved2=TenantContext.call(reviewer,1,()->quality.approve(recall2.id(),new InventoryQuality.Action(UUID.randomUUID(),recall2.version(),null,"Review second inspection")));var blocked2=quality.block(recall2.id(),new InventoryQuality.Action(UUID.randomUUID(),approved2.version(),null,"Quarantine second batch"));assertEquals("RECOVERED",quality.recover(recall2.id(),new InventoryQuality.Action(UUID.randomUUID(),blocked2.version(),"LAB-PASS","Release after inspection")).status());assertEquals(2500,inventory.balances(w).stream().filter(x->x.productId().equals(p)).findFirst().orElseThrow().valueXaf());
     assertTrue(TenantContext.call(new TenantScope(t,c,UUID.randomUUID(),a),1,quality::lots).isEmpty());assertTrue(TenantContext.call(new TenantScope(UUID.randomUUID(),c,b,a),1,quality::reservations).isEmpty());
     assertThrows(SQLException.class,()->sql(owner,"update inventory_recall set reason='tamper' where status='DISPOSED'"));
     return null;
    });
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static void sql(Connection c,String query)throws SQLException {try(var s=c.createStatement()){s.execute(query);}}
 static long count(Connection c,String query)throws SQLException {try(var s=c.createStatement();var rs=s.executeQuery(query)){rs.next();return rs.getLong(1);}}
}
