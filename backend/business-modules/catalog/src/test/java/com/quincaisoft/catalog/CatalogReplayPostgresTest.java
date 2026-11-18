package com.quincaisoft.catalog;

import com.quincaisoft.catalog.application.*;
import com.quincaisoft.catalog.api.*;
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
class CatalogReplayPostgresTest {
 static void duplicateDenied(org.junit.jupiter.api.function.Executable action,String constraint){
  var denied=assertThrows(org.hibernate.exception.ConstraintViolationException.class,action);
  assertEquals("23505",denied.getSQLState());assertEquals(constraint,denied.getConstraintName());
 }
 static void sql(Connection connection,String text)throws Exception {try(var statement=connection.createStatement()){statement.execute(text);}}
 @Test void catalogReplayCannotCrossScopeOrChangeReceiptMeaning()throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String name=UUID.randomUUID().toString().replace("-",""),db="master_test_"+name,role="master_role_"+name;
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID tenant=UUID.randomUUID(),company=UUID.randomUUID(),branch=UUID.randomUUID(),actor=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")){
    try(var paths=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var path:paths.filter(p->p.toString().endsWith(".sql")).sorted(Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(path));}
    sql(owner,"INSERT INTO tenant(id,name)VALUES('"+tenant+"','Synthetic');INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+company+"','Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+branch+"','Synthetic')");
    UUID supplier = UUID.randomUUID();
    sql(owner, "insert into supplier (tenant_id, company_id, id, code, name) values ('"+tenant+"','"+company+"','"+supplier+"','TEST','Scoped supplier')");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.catalog.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions transactions=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var masters=new DefaultCatalogMasterService(em,transactions);var catalog=new DefaultCatalogService(em,transactions);
    var scope=new TenantScope(tenant,company,branch,actor);
    UUID otherCompany=UUID.randomUUID(),otherBranch=UUID.randomUUID(),companyBranch=UUID.randomUUID();
    sql(owner,"INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+otherCompany+"','Other');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+otherBranch+"','Other'),('"+tenant+"','"+otherCompany+"','"+companyBranch+"','Other')");
    var categoryCommand=new CatalogMasterService.CategorySave(UUID.randomUUID(),"CAT","Category",null,"","",true,"0","create");
    var unitCommand=new CatalogMasterService.UnitSave(UUID.randomUUID(),"piece","Piece","piece","1","1",0,true,"0","create");
    var productCommand=new CatalogService.Save(UUID.randomUUID(),"SKU","Product","piece","9007199254740993",0,"0",true,"create");
    var category=TenantContext.call(scope,1,()->masters.saveCategory(null,categoryCommand));
    var unit=TenantContext.call(scope,1,()->masters.saveUnit(null,unitCommand));
    var product=TenantContext.call(scope,1,()->catalog.create(productCommand));
    TenantContext.call(scope,1,()->{
     assertEquals(category,masters.saveCategory(null,categoryCommand));assertEquals(unit,masters.saveUnit(null,unitCommand));assertEquals(product,catalog.create(productCommand));
     assertEquals(9007199254740993L,product.sellingPriceXaf());
     assertThrows(IllegalArgumentException.class,()->masters.saveUnit(unit.id(),new CatalogMasterService.UnitSave(UUID.randomUUID(),"piece","Piece","piece","2","1",0,true,unit.version(),"rescale")));
     assertThrows(IllegalArgumentException.class,()->catalog.update(product.id(),new CatalogService.Save(UUID.randomUUID(),"SKU","Product","box","1",0,Long.toString(product.version()),true,"rescale")));
     return null;
    });
    // Receipt RLS conceals other scopes. Unique constraints must roll back the attempted
    // write without exposing the original response or leaving a second aggregate.
    for(var denied:List.of(new TenantScope(tenant,company,otherBranch,actor),new TenantScope(tenant,otherCompany,companyBranch,actor))){
     boolean sameCompany=denied.companyId().equals(company);
     TenantContext.call(denied,1,()->{
      duplicateDenied(()->masters.saveCategory(null,categoryCommand),sameCompany?"catalog_category_tenant_id_company_id_code_key":"idempotency_result_pkey");
      duplicateDenied(()->masters.saveUnit(null,unitCommand),sameCompany?"catalog_unit_tenant_id_company_id_code_key":"idempotency_result_pkey");
      duplicateDenied(()->catalog.create(productCommand),sameCompany?"catalog_product_tenant_id_company_id_sku_key":"idempotency_result_pkey");return null;
     });
    }
    TenantContext.call(new TenantScope(tenant,company,branch,UUID.randomUUID()),1,()->{assertThrows(IllegalStateException.class,()->masters.saveCategory(null,categoryCommand));assertThrows(IllegalStateException.class,()->masters.saveUnit(null,unitCommand));assertThrows(IllegalStateException.class,()->catalog.create(productCommand));return null;});
    TenantContext.call(new TenantScope(tenant,otherCompany,companyBranch,actor),1,()->{assertTrue(masters.categories().isEmpty());assertTrue(masters.units().isEmpty());assertTrue(catalog.list().isEmpty());return null;});
    // A corrupt/rejected receipt with matching bytes must not masquerade as a committed category.
    var json=JsonMapper.builder().build();
    for(String outcome:List.of("REJECTED","WRONG_SOURCE")){
     UUID op=UUID.randomUUID();var command=new CatalogMasterService.CategorySave(op,"CAT","Category",null,"","",true,"0","create");
     String material="CATEGORY|null|"+actor+"|"+json.writeValueAsString(command);
     String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(material.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
     try(var insert=owner.prepareStatement("insert into idempotency_result(tenant_id,company_id,branch_id,operation_id,actor_id,request_hash,source_type,source_id,status,response_json)values(?,?,?,?,?,?,?,?,?,cast(? as jsonb))")){
      insert.setObject(1,tenant);insert.setObject(2,company);insert.setObject(3,branch);insert.setObject(4,op);insert.setObject(5,actor);insert.setString(6,hash);insert.setString(7,outcome.equals("WRONG_SOURCE")?"CATALOG":"CATEGORY");insert.setObject(8,category.id());insert.setString(9,outcome.equals("REJECTED")?"REJECTED":"ACCEPTED");insert.setString(10,json.writeValueAsString(category));insert.executeUpdate();
     }
     TenantContext.call(scope,1,()->{assertThrows(IllegalStateException.class,()->masters.saveCategory(null,command));return null;});
    }
    TenantContext.call(scope,1,()->{assertEquals(1,masters.categories().size());assertEquals(1,masters.units().size());assertEquals(1,catalog.list().size());return null;});
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH (FORCE)");sql(admin,"DROP ROLE "+role);}
  }
 }
}
