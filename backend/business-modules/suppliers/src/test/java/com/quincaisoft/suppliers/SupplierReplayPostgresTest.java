package com.quincaisoft.suppliers;

import com.quincaisoft.suppliers.api.SupplierService;
import com.quincaisoft.suppliers.application.DefaultSupplierService;


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
class SupplierReplayPostgresTest {
 static void sql(Connection connection,String text)throws Exception {try(var statement=connection.createStatement()){statement.execute(text);}}
 @Test void supplierReplayRequiresAcceptedOriginalActorAndSource()throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String name=UUID.randomUUID().toString().replace("-",""),db="supplier_test_"+name,role="supplier_role_"+name;
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID tenant=UUID.randomUUID(),company=UUID.randomUUID(),branch=UUID.randomUUID(),actor=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")){
    try(var paths=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var path:paths.filter(p->p.toString().endsWith(".sql")).sorted(Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(path));}
    sql(owner,"INSERT INTO tenant(id,name)VALUES('"+tenant+"','Synthetic');INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+company+"','Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+branch+"','Synthetic')");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.suppliers.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions transactions=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var service=new DefaultSupplierService(em,transactions,JsonMapper.builder().build());
    var scope=new TenantScope(tenant,company,branch,actor);
    var request=new SupplierService.Create(UUID.randomUUID(),"SUP-1","Scoped supplier","","contact@example.invalid");
    var created=TenantContext.call(scope,1,()->service.create(request));
    TenantContext.call(scope,1,()->{assertEquals(created,service.create(request));return null;});
    TenantContext.call(new TenantScope(tenant,company,branch,UUID.randomUUID()),1,()->{assertThrows(IllegalStateException.class,()->service.create(request));return null;});
    UUID otherCompany=UUID.randomUUID(),otherCompanyBranch=UUID.randomUUID(),otherBranch=UUID.randomUUID();
    sql(owner,"INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+otherCompany+"','Other company');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+otherCompany+"','"+otherCompanyBranch+"','Other company branch'),('"+tenant+"','"+company+"','"+otherBranch+"','Other branch')");
    // RLS conceals the original receipt across scope. The unique constraints then
    // reject the new mutation; assert exact cause and full transaction rollback.
    TenantContext.call(new TenantScope(tenant,otherCompany,otherCompanyBranch,actor),1,()->{
      var failure=assertThrows(org.hibernate.exception.ConstraintViolationException.class,()->service.create(request));
      assertEquals("23505",failure.getSQLState());assertEquals("idempotency_result_pkey",failure.getConstraintName());return null;
    });
    TenantContext.call(new TenantScope(tenant,company,otherBranch,actor),1,()->{
      var failure=assertThrows(org.hibernate.exception.ConstraintViolationException.class,()->service.create(request));
      assertEquals("23505",failure.getSQLState());assertEquals("supplier_tenant_id_company_id_code_key",failure.getConstraintName());return null;
    });
    try(var statement=owner.createStatement();var rows=statement.executeQuery("SELECT (SELECT count(*) FROM supplier),(SELECT count(*) FROM idempotency_result),(SELECT count(*) FROM outbox_event)")){
      assertTrue(rows.next());assertEquals(1,rows.getLong(1));assertEquals(1,rows.getLong(2));assertEquals(1,rows.getLong(3));
    }
    // Synthetic foreign/rejected receipts must never be decoded as a supplier success.
    var journal=new com.quincaisoft.suppliers.infrastructure.OperationJournal(em,JsonMapper.builder().build());
    for(String kind:List.of("WRONG_SOURCE","REJECTED","WRONG_ACTOR")){
      UUID op=UUID.randomUUID();String material="Synthetic receipt "+kind;
      String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(material.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      String source=kind.equals("WRONG_SOURCE")?"OtherModule":"SupplierSaved";
      String status=kind.equals("REJECTED")?"REJECTED":"ACCEPTED";
      UUID receiptActor=kind.equals("WRONG_ACTOR")?UUID.randomUUID():actor;
      sql(owner,"INSERT INTO idempotency_result(tenant_id,operation_id,company_id,branch_id,actor_id,request_hash,source_type,source_id,status,response_json)VALUES('"+tenant+"','"+op+"','"+company+"','"+branch+"','"+receiptActor+"','"+hash+"','"+source+"','"+created.id()+"','"+status+"','{}')");
      TenantContext.call(scope,1,()->{assertThrows(IllegalStateException.class,()->transactions.execute(()->journal.once(op,material,"SupplierSaved",SupplierService.Supplier.class,()->{fail("Foreign receipt must not execute supplier mutation");return created;},SupplierService.Supplier::id)));return null;});
    }
    TenantContext.call(scope,1,()->{assertEquals(created,service.get(created.id()));assertEquals(1,service.page("",0,10).total());return null;});
    try(var statement=owner.createStatement();var rows=statement.executeQuery("SELECT count(*) FROM outbox_event WHERE event_type='SupplierSaved.v1'")){assertTrue(rows.next());assertEquals(1,rows.getLong(1));}
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH(FORCE)");sql(admin,"DROP ROLE "+role);}
  }
 }
}
