package com.quincaisoft.documents;

import com.quincaisoft.documents.api.DocumentWorkspace;
import com.quincaisoft.documents.application.*;
import com.quincaisoft.shared.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresOperationalDocumentsTest {
 @Test void capturesActualCustodyPickupDeliveryAndRecallWithIsolationAndImmutableReplay() throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),user=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String suffix=UUID.randomUUID().toString().replace("-",""),db="opdocs_"+suffix,role="opdocs_role_"+suffix;
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID t=UUID.randomUUID(),c=UUID.randomUUID(),b=UUID.randomUUID(),a=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,user,"")) {
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT;GRANT quincai_app TO "+role);
   sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,user,"")) {
    try(var files=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))) {
     for(var file:files.filter(p->p.toString().endsWith(".sql")).sorted(Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));
    }
    var fixture=seed(owner,t,c,b,a);
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.documents.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var transactions=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return transactions.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var json=JsonMapper.builder().build();var originals=new DefaultDocumentService(em,tx,json);var reader=new DocumentSourceReader(em,originals,json);var workspace=new DocumentWorkspaceService(em,tx,json,originals,reader);
    auth("inventory:read","documents:read","documents:write","documents:print");
    var scope=new TenantScope(t,c,b,a);
    var request=new DocumentWorkspace.CaptureRequest(UUID.randomUUID(),"DOC18",fixture.custody());
    var custody=TenantContext.call(scope,1,()->{
     assertEquals(2,workspace.sources("DOC12").size(),"Completed delivery and actual pickup are separately selectable");
     var pickup=workspace.capture(new DocumentWorkspace.CaptureRequest(UUID.randomUUID(),"DOC12",fixture.pickup()));
     var row=pickup.content().source().rows().getFirst();
     assertEquals("400",row.get(4));assertEquals("CUSTODY_PICKUP",row.get(5));assertEquals("signed-pickup-001",row.get(6));assertEquals(a.toString(),row.get(7));
     var delivery=workspace.capture(new DocumentWorkspace.CaptureRequest(UUID.randomUUID(),"DOC12",fixture.delivery()));
     assertEquals("1000",delivery.content().source().rows().getFirst().get(4));assertEquals("delivery-proof-001",delivery.content().source().rows().getFirst().get(6));
     var recall=workspace.capture(new DocumentWorkspace.CaptureRequest(UUID.randomUUID(),"DOC25",fixture.recall()));
     var recallBody=recall.content().source();var rr=recallBody.rows().getFirst();assertEquals("LOT-2024",rr.getFirst());assertEquals("300",rr.get(5));assertEquals("300",rr.get(6));
     assertEquals("Safety recall",rr.get(recallBody.columns().indexOf("Reason|Motif")));
     assertEquals("manufacturer-notice",rr.get(recallBody.columns().indexOf("Evidence|Justificatif")));
     assertEquals(1,workspace.sources("DOC25").size(),"Draft recall is never an approved notice");
     assertThrows(NoSuchElementException.class,()->workspace.capture(new DocumentWorkspace.CaptureRequest(UUID.randomUUID(),"DOC25",fixture.draftRecall())));
     var captured=workspace.capture(request);var balance=captured.content().source().rows().getFirst();assertEquals(List.of("1000","400","600"),balance.subList(5,8));assertEquals("",captured.content().source().totalXaf());
     assertTrue(workspace.preview(captured.id(),"A4").contains("&lt;acier&gt;"));
     return captured;
    });
    sql(owner,"UPDATE catalog_product SET name='Changed after capture' WHERE id='"+fixture.product()+"'");
    TenantContext.call(scope,1,()->{assertEquals(custody,workspace.capture(request));assertEquals(custody,workspace.get(custody.id()));assertTrue(custody.content().source().rows().getFirst().contains("Écrou <acier>"));assertThrows(IllegalStateException.class,()->workspace.capture(new DocumentWorkspace.CaptureRequest(request.operationId(),"DOC25",fixture.recall())));return null;});
    TenantContext.call(scope,1,()->{
     var issue=new com.quincaisoft.documents.api.DocumentService.Issue(UUID.randomUUID(),UUID.randomUUID(),"QUOTATION",java.time.LocalDate.of(2026,11,18),null,"1000",List.of(new com.quincaisoft.documents.api.DocumentService.Item(fixture.product(),"BOLT-01","Timestamp fixture","piece","1000","1000","1000")));
     var issued=originals.issue(issue);
     assertEquals(issued,originals.get(issued.id()),"Issued response must equal a fresh PostgreSQL read, including the hashed timestamp");
     assertEquals(issued,originals.issue(issue),"Original issuance replay must retain the complete original response");
     assertEquals(issued.issuedAt().toString(),json.readTree(issued.content()).get("issuedAt").asText());
     var printRequest=new DocumentWorkspace.PrintRequest(UUID.randomUUID(),"Requested test print","A4");
     var print=workspace.print(custody.id(),true,printRequest);
     assertEquals(print,workspace.print(custody.id(),true,printRequest));
     assertEquals(print.requestedAt(),workspace.attempts().stream().filter(p->p.id().equals(print.id())).findFirst().orElseThrow().requestedAt(),"Print timestamp must survive a new database transaction");
     var outcomeRequest=new DocumentWorkspace.OutcomeRequest(UUID.randomUUID(),"PRINTED","Synthetic client-attested print outcome");
     var outcome=workspace.outcome(print.id(),outcomeRequest);
     assertEquals(outcome,workspace.outcome(print.id(),outcomeRequest));
     var storedTime=tx.execute(()->em.find(com.quincaisoft.documents.infrastructure.DocumentPrintOutcome.class,new com.quincaisoft.documents.infrastructure.DocumentSnapshot.Key(t,c,outcome.id())).recordedAt);
     assertEquals(outcome.recordedAt(),storedTime,"Outcome timestamp must survive a fresh PostgreSQL read");
     return null;
    });
    var competingAttempt=TenantContext.call(scope,1,()->workspace.print(custody.id(),true,new DocumentWorkspace.PrintRequest(UUID.randomUUID(),"Concurrent outcome test","A4")));
    var competing=concurrentOutcomes(workspace,scope,competingAttempt.id(),false);
    assertEquals(1,competing.stream().filter("REJECTED"::equals).count(),"Distinct concurrent outcomes must have exactly one winner");
    var replayAttempt=TenantContext.call(scope,1,()->workspace.print(custody.id(),true,new DocumentWorkspace.PrintRequest(UUID.randomUUID(),"Concurrent replay test","A4")));
    var replay=concurrentOutcomes(workspace,scope,replayAttempt.id(),true);
    assertFalse(replay.contains("REJECTED"));assertEquals(1,new HashSet<>(replay).size(),"Identical concurrent requests must return one persisted outcome identity");
    for(var hidden:List.of(new TenantScope(t,c,UUID.randomUUID(),a),new TenantScope(t,UUID.randomUUID(),b,a),new TenantScope(UUID.randomUUID(),c,b,a)))TenantContext.call(hidden,1,()->{assertTrue(workspace.sources("DOC18").isEmpty());assertThrows(NoSuchElementException.class,()->workspace.get(custody.id()));return null;});
    auth("documents:read","documents:write");TenantContext.call(scope,1,()->{assertThrows(AccessDeniedException.class,()->workspace.capture(request));assertThrows(AccessDeniedException.class,()->workspace.get(custody.id()));return null;});
    assertThrows(SQLException.class,()->sql(owner,"UPDATE document_capture SET content='tampered' WHERE id='"+custody.id()+"'"));
    try(var runtime=DriverManager.getConnection(url,role,"");var statement=runtime.createStatement();var result=statement.executeQuery("select count(*) from document_custody_v1")){result.next();assertEquals(0,result.getInt(1));}
   } finally {SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH(FORCE)");sql(admin,"DROP ROLE "+role);}
  }
 }
 record Fixture(UUID product,UUID custody,UUID pickup,UUID delivery,UUID recall,UUID draftRecall){}
 static Fixture seed(Connection db,UUID t,UUID c,UUID b,UUID a)throws SQLException {
  UUID w=UUID.randomUUID(),p=UUID.randomUUID(),customer=UUID.randomUUID(),custody=UUID.randomUUID(),pickup=UUID.randomUUID(),delivery=UUID.randomUUID(),lot=UUID.randomUUID(),recall=UUID.randomUUID(),draft=UUID.randomUUID();
  String scope=q(t)+","+q(c)+","+q(b),company=q(t)+","+q(c);
  sql(db,"INSERT INTO tenant(id,name)VALUES("+q(t)+",'Synthetic');INSERT INTO company(tenant_id,id,name)VALUES("+company+",'Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES("+scope+",'Douala');INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name)VALUES("+scope+","+q(w)+",'Main warehouse');INSERT INTO catalog_product(tenant_id,company_id,id,sku,name,base_unit,selling_price_xaf)VALUES("+company+","+q(p)+",'BOLT-01','Écrou <acier>','piece',1000);INSERT INTO customer(tenant_id,company_id,id,name)VALUES("+company+","+q(customer)+",'Synthetic customer');INSERT INTO document_settings VALUES("+scope+",'QuincaiSoft fixture','Douala','SYNTHETIC','TEST','fr',1)");
  UUID sale=seedSale(db,t,c,b,a,w,p,customer),deliveredSale=seedSale(db,t,c,b,a,w,p,customer);
  sql(db,"INSERT INTO customer_custody(tenant_id,company_id,branch_id,id,sale_id,customer_id,warehouse_id,product_id,quantity_milli,collected_milli,frozen_cost_xaf,status,operation_id,created_at)VALUES("+scope+","+q(custody)+","+q(sale)+","+q(customer)+","+q(w)+","+q(p)+",1000,400,700,'PARTIALLY_COLLECTED',"+q(UUID.randomUUID())+",now());INSERT INTO custody_collection VALUES("+scope+","+q(pickup)+","+q(custody)+","+q(UUID.randomUUID())+",400,'signed-pickup-001',"+q(a)+",'2026-11-18T10:00:00Z');INSERT INTO sale_fulfillment(tenant_id,company_id,branch_id,id,sale_id,customer_id,status,proof_reference,business_date,created_by,created_at)VALUES("+scope+","+q(delivery)+","+q(deliveredSale)+","+q(customer)+",'COMPLETED','delivery-proof-001','2026-11-18',"+q(a)+",now());INSERT INTO inventory_lot(tenant_id,company_id,branch_id,id,warehouse_id,product_id,lot_number,source_reference,quantity_milli,quarantined_milli,created_by,created_at)VALUES("+scope+","+q(lot)+","+q(w)+","+q(p)+",'LOT-2024','GRN-2024',1000,300,"+q(a)+",now());INSERT INTO inventory_recall(tenant_id,company_id,branch_id,id,lot_id,quantity_milli,status,business_date,reason,evidence_reference,created_by,approved_by,created_at)VALUES("+scope+","+q(recall)+","+q(lot)+",300,'QUARANTINED','2026-11-18','Safety recall','manufacturer-notice',"+q(a)+","+q(UUID.randomUUID())+",now()),("+scope+","+q(draft)+","+q(lot)+",100,'DRAFT','2026-11-18','Not approved','draft',"+q(a)+",NULL,now())");
  return new Fixture(p,custody,pickup,delivery,recall,draft);
 }
 static UUID seedSale(Connection db,UUID t,UUID c,UUID b,UUID a,UUID w,UUID p,UUID customer)throws SQLException {
  UUID sale=UUID.randomUUID();String scope=q(t)+","+q(c)+","+q(b);
  db.setAutoCommit(false);
  try {
   sql(db,"INSERT INTO sale(tenant_id,company_id,branch_id,id,operation_id,warehouse_id,customer_id,actor_id,business_date,status,total_xaf,cost_xaf)VALUES("+scope+","+q(sale)+","+q(UUID.randomUUID())+","+q(w)+","+q(customer)+","+q(a)+",'2026-11-18','DRAFT',1000,700);INSERT INTO sale_line VALUES("+q(t)+","+q(c)+","+q(sale)+","+q(UUID.randomUUID())+","+q(p)+",1000,1000,1000,700);INSERT INTO document_snapshot VALUES("+scope+","+q(UUID.randomUUID())+","+q(sale)+","+q(UUID.randomUUID())+",'INV-"+sale+"','INVOICE','2026-11-18',1000,repeat('a',64),'{}',now());UPDATE sale SET status='POSTED' WHERE id="+q(sale));db.commit();
  } catch(SQLException failure){db.rollback();throw failure;}finally{db.setAutoCommit(true);}
  return sale;
 }
 static String q(UUID id){return "'"+id+"'";}
 static void sql(Connection c,String q)throws SQLException {try(var s=c.createStatement()){s.execute(q);}}
 static void auth(String... permissions){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("test",null,Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));}
 static List<String> concurrentOutcomes(DocumentWorkspace workspace,TenantScope scope,UUID attempt,boolean sameOperation)throws Exception {
  var start=new java.util.concurrent.CountDownLatch(1);UUID operation=UUID.randomUUID();
  try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
   var futures=new ArrayList<java.util.concurrent.Future<String>>();
   for(int i=0;i<2;i++) {
    UUID key=sameOperation?operation:UUID.randomUUID();
    futures.add(pool.submit(()->{
     auth("inventory:read","documents:read","documents:print");
     try {start.await();return TenantContext.call(scope,1,()->workspace.outcome(attempt,new DocumentWorkspace.OutcomeRequest(key,"PRINTED","Concurrent client-attested outcome")).id().toString());}
     catch(IllegalStateException conflict){return "REJECTED";}
     finally{SecurityContextHolder.clearContext();}
    }));
   }
   start.countDown();var result=new ArrayList<String>();for(var f:futures)result.add(f.get(20,java.util.concurrent.TimeUnit.SECONDS));return result;
  }
 }
}
