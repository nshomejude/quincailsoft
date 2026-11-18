package com.quincaisoft.documents;

import com.quincaisoft.documents.api.DocumentService.Settings;
import com.quincaisoft.documents.application.DefaultDocumentService;
import com.quincaisoft.documents.web.DocumentController;
import com.quincaisoft.shared.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresDocumentSettingsTest {
 static void sql(Connection connection,String text)throws Exception {try(var statement=connection.createStatement()){statement.execute(text);}}
 @Test void concurrentReplayRetainsOriginalReceiptAndRejectsChangedPayloadActorAndStaleVersion()throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String name=UUID.randomUUID().toString().replace("-",""),db="docs_test_"+name,role="docs_role_"+name;
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID tenant=UUID.randomUUID(),company=UUID.randomUUID(),branch=UUID.randomUUID(),actor=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")){
    try(var paths=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var path:paths.filter(p->p.toString().endsWith(".sql")).sorted(Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(path));}
    sql(owner,"INSERT INTO tenant(id,name)VALUES('"+tenant+"','Synthetic');INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+company+"','Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+branch+"','Synthetic')");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.documents.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions transactions=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var service=new DefaultDocumentService(em,transactions,JsonMapper.builder().build());var controller=new DocumentController(service);
    var scope=new TenantScope(tenant,company,branch,actor);var operation=UUID.randomUUID();var original=new Settings("Synthetic issuer","Synthetic address","SYNTHETIC","INV","en",0);
    Settings first;
    try(var executor=Executors.newFixedThreadPool(2)){
     var start=new CountDownLatch(1);
     Callable<Settings> command=()->{start.await();return TenantContext.call(scope,1,()->controller.configure(operation,original));};
     var one=executor.submit(command);var two=executor.submit(command);start.countDown();first=one.get(20,TimeUnit.SECONDS);assertEquals(first,two.get(20,TimeUnit.SECONDS));assertEquals(1,first.version());
    }
    var stored=first;
    TenantContext.call(scope,1,()->{
     var revision=new Settings("Changed issuer",original.issuerAddress(),original.registrationNumber(),original.fiscalSeries(),"fr",stored.version());
     var changed=controller.configure(UUID.randomUUID(),revision);assertEquals(2,changed.version());
     assertEquals(stored,controller.configure(operation,original),"Retry must return original receipt, not current settings");
     assertEquals(changed,service.settings());
     assertThrows(IllegalStateException.class,()->controller.configure(operation,revision));
     UUID stale=UUID.randomUUID();assertThrows(RevisionConflict.class,()->controller.configure(stale,original));
     assertEquals(3,controller.configure(stale,new Settings("Third issuer",original.issuerAddress(),original.registrationNumber(),original.fiscalSeries(),"en",changed.version())).version(),"A rolled-back attempt must not leave a receipt");
     return null;
    });
    TenantContext.call(new TenantScope(tenant,company,branch,UUID.randomUUID()),1,()->{assertThrows(IllegalStateException.class,()->controller.configure(operation,original));return null;});
    TenantContext.call(new TenantScope(tenant,company,UUID.randomUUID(),actor),1,()->{assertEquals(0,service.settings().version());return null;});
    try(var statement=owner.createStatement();var results=statement.executeQuery("select count(*) from idempotency_result where source_type='DOCUMENT_SETTINGS'")){results.next();assertEquals(3,results.getInt(1));}
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH (FORCE)");sql(admin,"DROP ROLE "+role);}
  }
 }
}
