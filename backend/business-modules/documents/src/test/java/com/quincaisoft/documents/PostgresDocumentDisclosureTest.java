package com.quincaisoft.documents;

import com.quincaisoft.documents.api.*;
import com.quincaisoft.documents.application.*;
import com.quincaisoft.documents.web.DocumentController;
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
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Documents-only acceptance: no claim of full sale posting, OIDC or physical printer acceptance. */
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresDocumentDisclosureTest {
 @Test void ownerCaptureRemainsScopedAndSafeForCashierWithoutMutableLabelsOrCostProjection()throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),user=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String suffix=UUID.randomUUID().toString().replace("-",""),db="disclosure_"+suffix,role="disclosure_role_"+suffix;
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID tenant=UUID.randomUUID(),company=UUID.randomUUID(),branch=UUID.randomUUID(),ownerActor=UUID.randomUUID(),cashier=UUID.randomUUID();
  var ownerScope=new TenantScope(tenant,company,branch,ownerActor);var cashierScope=new TenantScope(tenant,company,branch,cashier);
  try(var admin=DriverManager.getConnection(adminUrl,user,"")){
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT;GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var connection=DriverManager.getConnection(url,user,"")){
    try(var paths=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var path:paths.filter(p->p.toString().endsWith(".sql")).sorted(Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1)))).toList())sql(connection,Files.readString(path));}
    sql(connection,"INSERT INTO tenant(id,name)VALUES('"+tenant+"','Synthetic');INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+company+"','Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+branch+"','Synthetic')");
    // A real unrelated supplier exists in scope; receipt capture must never enumerate all labels.
    sql(connection,"INSERT INTO supplier(tenant_id,company_id,id,code,name)VALUES('"+tenant+"','"+company+"','"+UUID.randomUUID()+"','PRIVATE-SUPPLIER','PRIVATE-SUPPLIER-LABEL')");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.documents.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var em=SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());var transactions=new TransactionTemplate(new JpaTransactionManager(factory.getObject()));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return transactions.execute(status->{var s=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",s.tenantId().toString()).setParameter("c",s.companyId().toString()).setParameter("b",s.branchId().toString()).setParameter("a",s.actorId().toString()).getSingleResult();return action.get();});}};
    var json=JsonMapper.builder().build();var originals=new DefaultDocumentService(em,tx,json);var reader=new DocumentSourceReader(em,originals,json);var workspace=new DocumentWorkspaceService(em,tx,json,originals,reader);var controller=new DocumentController(originals);
    auth("documents:read","documents:write","documents:print","sales:read","purchasing:read","finance:read");
    var original=TenantContext.call(ownerScope,1,()->{
     originals.configure(new DocumentService.Settings("Original issuer","Douala","REG","TEST","fr",0));
     return originals.issue(new DocumentService.Issue(UUID.randomUUID(),UUID.randomUUID(),"INVOICE",LocalDate.of(2026,11,18),UUID.randomUUID(),"1200",List.of(new DocumentService.Item(UUID.randomUUID(),"BOLT","Écrou <acier>","piece","2000","600","1200"))));
    });
    var request=new DocumentWorkspace.CaptureRequest(UUID.randomUUID(),"DOC01",original.sourceId());
    var captured=TenantContext.call(ownerScope,1,()->workspace.capture(request));
    String immutable=json.writeValueAsString(captured);
    for(String forbidden:List.of("PRIVATE-SUPPLIER-LABEL","supplier:","staff:","role:","integration:","unit_cost","costXaf","recognized_cost","return_valuation"))assertFalse(immutable.contains(forbidden),forbidden);
    TenantContext.call(ownerScope,1,()->{originals.configure(new DocumentService.Settings("Changed issuer","Yaoundé","NEW-REG","NEW","en",1));assertEquals(captured,workspace.capture(request));return null;});
    auth("documents:read","documents:print","sales:read");
    TenantContext.call(cashierScope,1,()->{
     assertEquals(captured,workspace.get(captured.id()));assertEquals(List.of(captured),workspace.list());
     String preview=workspace.preview(captured.id(),"80mm");assertTrue(preview.contains("Original issuer"));assertFalse(preview.contains("Changed issuer"));assertTrue(preview.contains("Écrou &lt;acier&gt;"));
     String copy=controller.print(original.id(),false);assertTrue(copy.contains("COPIE"));assertTrue(copy.contains("Original issuer"));assertEquals(original,controller.get(original.id()));
     var print=workspace.print(captured.id(),true,new DocumentWorkspace.PrintRequest(UUID.randomUUID(),"Cashier first physical print","80mm"));assertEquals("REQUESTED",print.status());assertEquals(captured.sha256(),print.documentHash());
     return null;
    });
    auth("documents:read","documents:print");
    TenantContext.call(cashierScope,1,()->{assertTrue(workspace.list().isEmpty());assertTrue(controller.list(null).isEmpty());assertThrows(AccessDeniedException.class,()->workspace.get(captured.id()));assertThrows(AccessDeniedException.class,()->controller.get(original.id()));assertThrows(AccessDeniedException.class,()->controller.print(original.id(),false));return null;});
    auth("documents:read","sales:read");
    for(var hidden:List.of(new TenantScope(tenant,company,UUID.randomUUID(),cashier),new TenantScope(tenant,UUID.randomUUID(),branch,cashier),new TenantScope(UUID.randomUUID(),company,branch,cashier)))TenantContext.call(hidden,1,()->{assertTrue(workspace.list().isEmpty());assertTrue(controller.list(null).isEmpty());assertThrows(NoSuchElementException.class,()->workspace.get(captured.id()));assertThrows(NoSuchElementException.class,()->controller.get(original.id()));return null;});
    assertThrows(SQLException.class,()->sql(connection,"UPDATE document_capture SET content='{}' WHERE id='"+captured.id()+"'"));
    assertThrows(SQLException.class,()->sql(connection,"UPDATE document_snapshot SET content='{}' WHERE id='"+original.id()+"'"));
   }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH(FORCE)");sql(admin,"DROP ROLE "+role);}
  }
 }
 static void sql(Connection connection,String query)throws SQLException{try(var s=connection.createStatement()){s.execute(query);}}
 static void auth(String... permissions){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("test",null,Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));}
}
