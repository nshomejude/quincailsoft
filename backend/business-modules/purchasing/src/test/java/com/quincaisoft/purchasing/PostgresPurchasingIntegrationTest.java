package com.quincaisoft.purchasing;
import com.quincaisoft.purchasing.api.PurchasingService;
import com.quincaisoft.purchasing.application.DefaultPurchasingService;
import com.quincaisoft.suppliers.api.SupplierService;
import com.quincaisoft.suppliers.application.DefaultSupplierService;



import com.quincaisoft.finance.application.DefaultFinanceService;

import com.quincaisoft.shared.*;
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
class PostgresPurchasingIntegrationTest {
 @Test void purchaseReceiptInvoiceAndRollback() throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String db="purchase_test_"+UUID.randomUUID().toString().replace("-",""),role="purchase_role_"+UUID.randomUUID().toString().replace("-","");
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
    var ds=new DriverManagerDataSource(url,role,"");factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.purchasing.infrastructure","com.quincaisoft.suppliers.infrastructure","com.quincaisoft.finance.infrastructure","com.quincaisoft.inventory.infrastructure","com.quincaisoft.catalog.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var catalog=new DefaultCatalogService(em,tx);var stock=new DefaultStockService(em,tx,catalog);var finance=new DefaultFinanceService(em,tx);var suppliers=new DefaultSupplierService(em,tx,JsonMapper.builder().build());var purchases=new DefaultPurchasingService(em,tx,suppliers,catalog,stock,finance,JsonMapper.builder().build());
    var scope=new TenantScope(t,c,b,a);
    final Connection ownerConnection=owner;
    var supplier=TenantContext.call(scope,1,()->suppliers.create(new SupplierService.Create(UUID.randomUUID(),"SUP-1","Synthetic supplier","","")));
    // Receipt provenance and acceptance are checked before the action, even with the exact hash.
    for(int invalid=0;invalid<3;invalid++){
     var forged=new PurchasingService.Draft(UUID.randomUUID(),supplier.id(),w,LocalDate.of(2026,11,18),List.of(new PurchasingService.RequestedLine(p,"10000","500")));
     String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(("PO_DRAFT|"+forged).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
     try(var insert=owner.prepareStatement("insert into idempotency_result(tenant_id,company_id,branch_id,operation_id,request_hash,source_type,actor_id,status,response_json)values(?,?,?,?,?,?,?,?,cast('{}' as jsonb))")){
      insert.setObject(1,t);insert.setObject(2,c);insert.setObject(3,b);insert.setObject(4,forged.operationId());insert.setString(5,hash);insert.setString(6,invalid==0?"CATALOG":"PurchaseDrafted");insert.setObject(7,invalid==1?UUID.randomUUID():a);insert.setString(8,invalid==2?"REJECTED":"ACCEPTED");insert.executeUpdate();
     }
     assertThrows(IllegalStateException.class,()->TenantContext.call(scope,1,()->purchases.draft(forged)));assertEquals(0,count(owner,"select count(*) from purchase_order"));
    }
    var orderRequest=new PurchasingService.Draft(UUID.randomUUID(),supplier.id(),w,LocalDate.of(2026,11,18),List.of(new PurchasingService.RequestedLine(p,"10000","500")));
    var draft=TenantContext.call(scope,1,()->purchases.draft(orderRequest));assertEquals("5000",draft.totalXaf());
    assertEquals(10000,count(owner,"select quantity_milli from stock_balance"));
    var submitted=TenantContext.call(scope,1,()->purchases.submit(draft.id(),new PurchasingService.Decision(UUID.randomUUID(),draft.version(),"Submit order")));
    assertThrows(IllegalStateException.class,()->TenantContext.call(scope,1,()->purchases.approve(draft.id(),new PurchasingService.Decision(UUID.randomUUID(),submitted.version(),"Self approve rejected"))));
    var manager=new TenantScope(t,c,b,UUID.randomUUID());
    var approved=TenantContext.call(manager,1,()->purchases.approve(draft.id(),new PurchasingService.Decision(UUID.randomUUID(),submitted.version(),"Independent approval")));
    var request=new PurchasingService.Receive(UUID.randomUUID(),approved.version(),"Receive inspected stock",LocalDate.of(2026,11,18),List.of(new PurchasingService.ReceiveLine(approved.lines().getFirst().id(),"4000")));
    var receipt=TenantContext.call(scope,1,()->purchases.receive(draft.id(),request));assertEquals("2000",receipt.totalXaf());
    assertEquals(receipt,TenantContext.call(scope,1,()->purchases.receive(draft.id(),request)));
    assertEquals(draft,TenantContext.call(scope,1,()->purchases.draft(orderRequest)));
    assertEquals(14000,count(owner,"select quantity_milli from stock_balance"));assertEquals(7000,count(owner,"select value_xaf from stock_balance"));
    assertEquals(1,count(owner,"select count(*) from journal_entry"));assertEquals(0,count(owner,"select sum(debit_xaf-credit_xaf) from journal_line"));
    var partial=TenantContext.call(scope,1,()->purchases.get(draft.id()));assertEquals("PARTIALLY_RECEIVED",partial.status());
    assertThrows(IllegalArgumentException.class,()->TenantContext.call(scope,1,()->purchases.receive(draft.id(),new PurchasingService.Receive(UUID.randomUUID(),partial.version(),"Excess receipt",LocalDate.of(2026,11,18),List.of(new PurchasingService.ReceiveLine(partial.lines().getFirst().id(),"6001"))))));
    assertEquals(14000,count(owner,"select quantity_milli from stock_balance"));
    var invoiceRequest=new PurchasingService.InvoiceRequest(UUID.randomUUID(),"  SUP-INV-1  ",LocalDate.of(2026,11,18),"Matched original receipt");
    var invoice=TenantContext.call(manager,1,()->purchases.invoice(receipt.id(),invoiceRequest));assertEquals("2000",invoice.totalXaf());assertEquals("  SUP-INV-1  ",invoice.supplierReference());
    assertEquals(invoice,TenantContext.call(manager,1,()->purchases.invoice(receipt.id(),invoiceRequest)));
    assertThrows(IllegalStateException.class,()->TenantContext.call(manager,1,()->purchases.invoice(receipt.id(),new PurchasingService.InvoiceRequest(UUID.randomUUID(),"SUP-INV-2",LocalDate.of(2026,11,18),"Duplicate invoice rejected"))));
    assertEquals(2,count(owner,"select count(*) from journal_entry"));
    sql(owner,"update financial_period set status='Closed'");
    assertThrows(IllegalStateException.class,()->TenantContext.call(scope,1,()->purchases.receive(draft.id(),new PurchasingService.Receive(UUID.randomUUID(),partial.version(),"Closed period rollback",LocalDate.of(2026,11,18),List.of(new PurchasingService.ReceiveLine(partial.lines().getFirst().id(),"1000"))))));
    assertEquals(14000,count(owner,"select quantity_milli from stock_balance"));assertEquals(1,count(owner,"select count(*) from purchase_receipt"));
    assertEquals("4000",TenantContext.call(scope,1,()->purchases.get(draft.id())).lines().getFirst().receivedMilli());
    sql(owner,"update financial_period set status='Open'");
    var second=TenantContext.call(scope,1,()->purchases.receive(draft.id(),new PurchasingService.Receive(UUID.randomUUID(),partial.version(),"Receive remainder",LocalDate.of(2026,11,18),List.of(new PurchasingService.ReceiveLine(partial.lines().getFirst().id(),"6000")))));
    // A new receipt must not let a case/outer-whitespace variant create a second payable.
    for(String duplicate:List.of("sup-inv-1","\tSuP-InV-1\n")){
     var duplicateRequest=new PurchasingService.InvoiceRequest(UUID.randomUUID(),duplicate,LocalDate.of(2026,11,18),"Duplicate normalized supplier identity");
     var denied=assertThrows(RuntimeException.class,()->TenantContext.call(manager,1,()->purchases.invoice(second.id(),duplicateRequest)));
     Throwable cause=denied;while(cause!=null&&!(cause instanceof SQLException))cause=cause.getCause();
     assertInstanceOf(SQLException.class,cause);assertEquals("23505",((SQLException)cause).getSQLState());
     assertEquals(1,count(owner,"select count(*) from supplier_invoice"));
     assertEquals(1,count(owner,"select count(*) from payable_balance"));
     assertEquals(3,count(owner,"select count(*) from journal_entry"));
     assertEquals(0,count(owner,"select count(*) from idempotency_result where operation_id='"+duplicateRequest.operationId()+"'"));
     assertEquals(0,count(owner,"select count(*) from outbox_event where operation_id='"+duplicateRequest.operationId()+"'"));
    }
    assertEquals("  SUP-INV-1  ",TenantContext.call(manager,1,()->purchases.invoice(receipt.id(),invoiceRequest)).supplierReference());
    var returnRequest=new PurchasingService.ReturnRequest(UUID.randomUUID(),LocalDate.of(2026,11,18),"Return inspected goods to supplier",List.of(new PurchasingService.ReturnLineRequest(second.lines().getFirst().id(),"2000")));
    var returned=TenantContext.call(manager,1,()->purchases.returnToSupplier(second.id(),returnRequest));assertEquals("1000",returned.totalXaf());assertEquals(returned,TenantContext.call(manager,1,()->purchases.returnToSupplier(second.id(),returnRequest)));
    assertEquals(18000,count(owner,"select quantity_milli from stock_balance"));assertEquals(9000,count(owner,"select value_xaf from stock_balance"));
    assertThrows(IllegalArgumentException.class,()->TenantContext.call(manager,1,()->purchases.returnToSupplier(second.id(),new PurchasingService.ReturnRequest(UUID.randomUUID(),LocalDate.of(2026,11,18),"Excess supplier return",List.of(new PurchasingService.ReturnLineRequest(second.lines().getFirst().id(),"4001"))))));
    assertThrows(IllegalStateException.class,()->TenantContext.call(manager,1,()->purchases.invoice(second.id(),new PurchasingService.InvoiceRequest(UUID.randomUUID(),"NET-INV",LocalDate.of(2026,11,18),"Net invoice not supported"))));
    assertThrows(IllegalStateException.class,()->TenantContext.call(manager,1,()->purchases.returnToSupplier(receipt.id(),new PurchasingService.ReturnRequest(UUID.randomUUID(),LocalDate.of(2026,11,18),"Already invoiced return blocked",List.of(new PurchasingService.ReturnLineRequest(receipt.lines().getFirst().id(),"1000"))))));
    assertEquals(4,count(owner,"select count(*) from journal_entry"));assertEquals(0,count(owner,"select sum(debit_xaf-credit_xaf) from journal_line"));
    var wrongScope=new TenantScope(UUID.randomUUID(),c,b,a);assertThrows(NoSuchElementException.class,()->TenantContext.call(wrongScope,1,()->purchases.get(draft.id())));

   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static void sql(Connection c,String query)throws SQLException {try(var s=c.createStatement()){s.execute(query);}}
 static long count(Connection c,String query)throws SQLException {try(var s=c.createStatement();var rs=s.executeQuery(query)){rs.next();return rs.getLong(1);}}
}
