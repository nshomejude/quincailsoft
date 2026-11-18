package com.quincaisoft.tenders;
import com.quincaisoft.tenders.api.*;import com.quincaisoft.tenders.application.*;import com.quincaisoft.purchasing.application.DefaultPayablesService;import com.quincaisoft.finance.api.ManualJournalService;import com.quincaisoft.finance.application.DefaultManualJournalService;
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
class SettlementPostgresTest {
 @Test void supplierBankAndManualJournalsConserveAndReplay() throws Exception {
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
    var ds=new DriverManagerDataSource(url,role,"");factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.purchasing.infrastructure","com.quincaisoft.suppliers.infrastructure","com.quincaisoft.finance.infrastructure","com.quincaisoft.inventory.infrastructure","com.quincaisoft.catalog.infrastructure","com.quincaisoft.tenders.infrastructure","com.quincaisoft.customeraccounts.infrastructure","com.quincaisoft.sales.infrastructure","com.quincaisoft.documents.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var catalog=new DefaultCatalogService(em,tx);var stock=new DefaultStockService(em,tx,catalog);var finance=new DefaultFinanceService(em,tx);var suppliers=new DefaultSupplierService(em,tx,JsonMapper.builder().build());var purchases=new DefaultPurchasingService(em,tx,suppliers,catalog,stock,finance,JsonMapper.builder().build());
    var scope=new TenantScope(t,c,b,a);
    final Connection ownerConnection=owner;
    var supplier=TenantContext.call(scope,1,()->suppliers.create(new SupplierService.Create(UUID.randomUUID(),"SUP-1","Synthetic supplier","","")));
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
    var invoiceRequest=new PurchasingService.InvoiceRequest(UUID.randomUUID(),"SUP-INV-1",LocalDate.of(2026,11,18),"Matched original receipt");
    var invoice=TenantContext.call(manager,1,()->purchases.invoice(receipt.id(),invoiceRequest));assertEquals("2000",invoice.totalXaf());
    assertEquals(invoice,TenantContext.call(manager,1,()->purchases.invoice(receipt.id(),invoiceRequest)));
    assertThrows(IllegalStateException.class,()->TenantContext.call(manager,1,()->purchases.invoice(receipt.id(),new PurchasingService.InvoiceRequest(UUID.randomUUID(),"SUP-INV-2",LocalDate.of(2026,11,18),"Duplicate invoice rejected"))));
    var json=JsonMapper.builder().build();var date=LocalDate.of(2026,11,18);
    var customers=new com.quincaisoft.customeraccounts.application.DefaultCustomerService(em,tx);
    var documents=new com.quincaisoft.documents.application.DefaultDocumentService(em,tx,json);
    var sales=new com.quincaisoft.sales.application.DefaultSalesService(em,tx,stock,catalog,finance,customers,json,documents);
    var cash=new DefaultTenderService(em,tx,finance,sales,customers,json);var bank=new DefaultBankService(em,tx,finance,json);var payables=new DefaultPayablesService(em,tx,suppliers);
    var settlements=new DefaultSettlementService(em,tx,finance,payables,cash,bank,sales,customers,json);var journals=new DefaultManualJournalService(em,tx,finance,json);
    var shift=TenantContext.call(scope,1,()->cash.open(new TenderService.Open(UUID.randomUUID(),"AP-TILL",date,"10000","Verified opening float")));
    var pay=new SettlementService.Payment(UUID.randomUUID(),supplier.id(),invoice.id(),date,"500",new SettlementService.Funding(shift.id(),null),"Supplier cash payment");
    var paid=TenantContext.call(manager,1,()->settlements.supplierPayment(pay));assertEquals(paid,TenantContext.call(manager,1,()->settlements.supplierPayment(pay)));
    assertEquals("1500",TenantContext.call(scope,1,()->payables.balances().getFirst().outstandingXaf()));
    assertThrows(IllegalArgumentException.class,()->TenantContext.call(manager,1,()->settlements.supplierPayment(new SettlementService.Payment(UUID.randomUUID(),supplier.id(),invoice.id(),date,"1501",new SettlementService.Funding(shift.id(),null),"Overpayment rejected"))));
    assertEquals("9500",TenantContext.call(scope,1,()->cash.sessions().getFirst().expectedXaf()));
    var advance=TenantContext.call(manager,1,()->settlements.supplierAdvance(new SettlementService.Payment(UUID.randomUUID(),supplier.id(),null,date,"1000",new SettlementService.Funding(shift.id(),null),"Supplier advance")));
    var allocation=new SettlementService.Allocate(UUID.randomUUID(),advance.id(),invoice.id(),date,"1000","Allocate advance once");var allocated=TenantContext.call(manager,1,()->settlements.allocateAdvance(allocation));assertEquals(allocated,TenantContext.call(manager,1,()->settlements.allocateAdvance(allocation)));
    assertEquals("8500",TenantContext.call(scope,1,()->cash.sessions().getFirst().expectedXaf()));assertEquals("500",TenantContext.call(scope,1,()->payables.balances().getFirst().outstandingXaf()));
    assertThrows(IllegalArgumentException.class,()->TenantContext.call(manager,1,()->settlements.allocateAdvance(new SettlementService.Allocate(UUID.randomUUID(),advance.id(),invoice.id(),date,"1","Advance exhausted"))));
    var evidence=TenantContext.call(scope,1,()->bank.request(new BankService.Request(UUID.randomUUID(),date,"BANK-OUT-500","OUT","500","Bank payment evidence")));
    assertThrows(IllegalArgumentException.class,()->TenantContext.call(scope,1,()->bank.approve(evidence.id(),new BankService.Approve(UUID.randomUUID(),evidence.version(),"Self verification denied"))));
    TenantContext.call(manager,1,()->bank.approve(evidence.id(),new BankService.Approve(UUID.randomUUID(),evidence.version(),"Independent bank verification")));
    TenantContext.call(manager,1,()->settlements.supplierPayment(new SettlementService.Payment(UUID.randomUUID(),supplier.id(),invoice.id(),date,"500",new SettlementService.Funding(null,evidence.id()),"Pay remainder by bank")));
    assertEquals("0",TenantContext.call(scope,1,()->payables.balances().getFirst().outstandingXaf()));
    assertThrows(IllegalArgumentException.class,()->TenantContext.call(manager,1,()->bank.match(new BankService.MatchRequest(UUID.randomUUID(),evidence.id(),date,"STAT-OUT","504","5","Wrong net denied"))));
    var matchRequest=new BankService.MatchRequest(UUID.randomUUID(),evidence.id(),date,"STAT-OUT","505","5","Exact bank statement");var match=TenantContext.call(manager,1,()->bank.match(matchRequest));assertEquals(match,TenantContext.call(manager,1,()->bank.match(matchRequest)));
    assertThrows(RuntimeException.class,()->TenantContext.call(manager,1,()->bank.match(new BankService.MatchRequest(UUID.randomUUID(),evidence.id(),date,"OTHER-STAT","505","5","Already matched denied"))));
    var depositEvidence=TenantContext.call(scope,1,()->bank.request(new BankService.Request(UUID.randomUUID(),date,"BANK-IN-500","IN","500","Customer bank deposit")));
    TenantContext.call(manager,1,()->bank.approve(depositEvidence.id(),new BankService.Approve(UUID.randomUUID(),depositEvidence.version(),"Independent incoming verification")));
    var deposit=TenantContext.call(scope,1,()->settlements.bankDeposit(new SettlementService.Payment(UUID.randomUUID(),customer,null,date,"500",new SettlementService.Funding(null,depositEvidence.id()),"Confirmed bank advance")));
    assertThrows(IllegalStateException.class,()->TenantContext.call(manager,1,()->cash.refund(deposit.id(),new TenderService.Allocation(UUID.randomUUID(),null,shift.id(),date,"1","Bank refund cannot use cash"))));
    TenantContext.call(manager,1,()->bank.match(new BankService.MatchRequest(UUID.randomUUID(),depositEvidence.id(),date,"STAT-IN","498","2","Incoming gross less fee")));
    assertThrows(NoSuchElementException.class,()->TenantContext.call(new TenantScope(t,c,UUID.randomUUID(),a),1,()->bank.get(evidence.id())));
    assertThrows(IllegalStateException.class,()->TenantContext.call(scope,1,()->settlements.supplierPayment(new SettlementService.Payment(pay.operationId(),supplier.id(),invoice.id(),date,"501",pay.funding(),"Conflict key rejected"))));
    // Balanced manual journal requires independent reviewer; reversal is another approved immutable batch.
    assertThrows(IllegalArgumentException.class,()->TenantContext.call(scope,1,()->journals.draft(new ManualJournalService.Draft(UUID.randomUUID(),date,"Unbalanced rejected",List.of(new ManualJournalService.Line("A","10","0"),new ManualJournalService.Line("B","0","9"))))));
    var manual=TenantContext.call(scope,1,()->journals.draft(new ManualJournalService.Draft(UUID.randomUUID(),date,"Reviewed adjustment",List.of(new ManualJournalService.Line("A","10","0"),new ManualJournalService.Line("B","0","10")))));
    var review=TenantContext.call(scope,1,()->journals.command(manual.id(),"submit",new ManualJournalService.Command(UUID.randomUUID(),manual.version(),"Submit journal")));
    assertThrows(IllegalArgumentException.class,()->TenantContext.call(scope,1,()->journals.command(manual.id(),"approve",new ManualJournalService.Command(UUID.randomUUID(),review.version(),"Self review denied"))));
    var posted=TenantContext.call(manager,1,()->journals.command(manual.id(),"approve",new ManualJournalService.Command(UUID.randomUUID(),review.version(),"Independent journal review")));
    var reverse=TenantContext.call(scope,1,()->journals.reversal(posted.id(),new ManualJournalService.Reversal(UUID.randomUUID(),posted.version(),date,"Reverse original adjustment")));
    var reverseReview=TenantContext.call(scope,1,()->journals.command(reverse.id(),"submit",new ManualJournalService.Command(UUID.randomUUID(),reverse.version(),"Submit reversal")));
    TenantContext.call(manager,1,()->journals.command(reverse.id(),"approve",new ManualJournalService.Command(UUID.randomUUID(),reverseReview.version(),"Approve reversal")));
    assertEquals(0,count(owner,"select sum(debit_xaf-credit_xaf) from journal_line where account_code in ('A','B')"));
    assertThrows(RuntimeException.class,()->TenantContext.call(scope,1,()->journals.reversal(posted.id(),new ManualJournalService.Reversal(UUID.randomUUID(),posted.version(),date,"Duplicate reversal denied"))));
    assertEquals(0,count(owner,"select sum(debit_xaf-credit_xaf) from journal_line"));
    // A consumed evidence reference cannot fund a second action, including simultaneous requests.
    var concurrentEvidence=TenantContext.call(scope,1,()->bank.request(new BankService.Request(UUID.randomUUID(),date,"BANK-RACE","OUT","100","Race evidence")));
    TenantContext.call(manager,1,()->bank.approve(concurrentEvidence.id(),new BankService.Approve(UUID.randomUUID(),concurrentEvidence.version(),"Race approval")));
    var start=new java.util.concurrent.CountDownLatch(1);try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
     var calls=new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();for(int i=0;i<2;i++)calls.add(pool.submit(()->{start.await();try{TenantContext.call(manager,1,()->settlements.supplierAdvance(new SettlementService.Payment(UUID.randomUUID(),supplier.id(),null,date,"100",new SettlementService.Funding(null,concurrentEvidence.id()),"One funding use")));return true;}catch(IllegalArgumentException expected){return false;}}));start.countDown();int successful=0;for(var future:calls)if(future.get())successful++;assertEquals(1,successful);
    }
    long before=count(owner,"select count(*) from settlement");sql(owner,"update financial_period set status='Closed'");
    assertThrows(IllegalStateException.class,()->TenantContext.call(manager,1,()->settlements.supplierAdvance(new SettlementService.Payment(UUID.randomUUID(),supplier.id(),null,date,"100",new SettlementService.Funding(shift.id(),null),"Closed period rejected"))));
    assertEquals(before,count(owner,"select count(*) from settlement"));assertEquals("8500",TenantContext.call(scope,1,()->cash.sessions().getFirst().expectedXaf()));
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static void sql(Connection c,String query)throws SQLException {try(var s=c.createStatement()){s.execute(query);}}
 static long count(Connection c,String query)throws SQLException {try(var s=c.createStatement();var rs=s.executeQuery(query)){rs.next();return rs.getLong(1);}}
}
