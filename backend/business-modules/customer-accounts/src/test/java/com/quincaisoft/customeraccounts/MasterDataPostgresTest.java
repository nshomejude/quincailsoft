package com.quincaisoft.customeraccounts;

import com.quincaisoft.customeraccounts.api.CustomerDirectory;
import com.quincaisoft.customeraccounts.application.DefaultCustomerDirectory;
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
class MasterDataPostgresTest {
 static void sql(Connection connection,String text)throws Exception {try(var statement=connection.createStatement()){statement.execute(text);}}
 @Test void mastersPreserveScopeReplayMetadataAndVersionedUnitIdentity()throws Exception {
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
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.customeraccounts.infrastructure","com.quincaisoft.catalog.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions transactions=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var scope=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",scope.tenantId().toString()).setParameter("c",scope.companyId().toString()).setParameter("b",scope.branchId().toString()).setParameter("a",scope.actorId().toString()).getSingleResult();return action.get();});}};
    var customers=new DefaultCustomerDirectory(em,transactions);var masters=new DefaultCatalogMasterService(em,transactions);var catalog=new DefaultCatalogService(em,transactions);
    var scope=new TenantScope(tenant,company,branch,actor);var operation=UUID.randomUUID();
    var original=new CustomerDirectory.Save(operation,"Synthetic customer","Contractor","+237600000000","synthetic@example.invalid","Address","REG","9007199254740993",30,true,false,"0","test customer");
    CustomerDirectory.Entry first;
    try(var executor=Executors.newFixedThreadPool(2)){
     var start=new CountDownLatch(1);Callable<CustomerDirectory.Entry> command=()->{start.await();return TenantContext.call(scope,1,()->customers.create(original));};
     var one=executor.submit(command);var two=executor.submit(command);start.countDown();first=one.get(20,TimeUnit.SECONDS);assertEquals(first,two.get(20,TimeUnit.SECONDS));
    }
    var stored=first;
    TenantContext.call(scope,1,()->{
     assertFalse(stored.active());assertEquals("9007199254740993",stored.creditLimitXaf());assertEquals(1,customers.page("Synthetic",0,10).total());assertEquals(0,customers.page("%",0,10).total());
     var update=new CustomerDirectory.Save(UUID.randomUUID(),"Changed customer","Company","","","Address","REG","1000",14,false,true,stored.version(),"update");
     var changed=customers.update(stored.id(),update);assertTrue(changed.active());assertEquals("1",changed.version());assertEquals(stored,customers.create(original));
     assertThrows(RevisionConflict.class,()->customers.update(stored.id(),new CustomerDirectory.Save(UUID.randomUUID(),"Stale","Company","","","","","1000",0,false,true,"0","stale")));
     assertThrows(IllegalStateException.class,()->customers.create(new CustomerDirectory.Save(operation,"Other","Company","","","","","0",0,false,true,"0","changed")));
     var parent=masters.saveCategory(null,new CatalogMasterService.CategorySave(UUID.randomUUID(),"PARENT","Parent",null,"","",true,"0","create"));
     var child=masters.saveCategory(null,new CatalogMasterService.CategorySave(UUID.randomUUID(),"CHILD","Child",parent.id(),"Brand","Description",true,"0","create"));
     assertThrows(IllegalArgumentException.class,()->masters.saveCategory(parent.id(),new CatalogMasterService.CategorySave(UUID.randomUUID(),parent.code(),parent.name(),child.id(),"","",true,parent.version(),"cycle")));
     var unitCommand=new CatalogMasterService.UnitSave(UUID.randomUUID(),"piece","Piece","piece","1","1",0,true,"0","create");var unit=masters.saveUnit(null,unitCommand);assertEquals(unit,masters.saveUnit(null,unitCommand));
     var pack=masters.saveUnit(null,new CatalogMasterService.UnitSave(UUID.randomUUID(),"box","Box","piece","12","1",0,true,"0","create"));assertEquals("12",pack.factorNumerator());
     assertThrows(IllegalArgumentException.class,()->masters.saveUnit(pack.id(),new CatalogMasterService.UnitSave(UUID.randomUUID(),"box","Box","piece","10","1",0,true,pack.version(),"rescale")));
     var product=catalog.create(new CatalogService.Save(UUID.randomUUID(),"MASTER-SKU","Master product","piece","9007199254740993",0,"0",false,"import inactive",child.id(),"Brand","123456","12000","3000",true,false,false,"Full description"));
     assertFalse(product.active());assertEquals("12000",product.packSizeMilli());assertEquals(9007199254740993L,product.sellingPriceXaf());assertEquals(child.id(),product.categoryId());
     assertThrows(IllegalArgumentException.class,()->catalog.create(new CatalogService.Save(UUID.randomUUID(),"BAD","Bad","piece","10",0,"0",true,"invalid relation",UUID.randomUUID(),"","","1000","0",true,false,false,"")));
     return null;
    });
    TenantContext.call(new TenantScope(tenant,company,branch,UUID.randomUUID()),1,()->{assertThrows(IllegalStateException.class,()->customers.create(original));return null;});
    TenantContext.call(new TenantScope(UUID.randomUUID(),company,branch,actor),1,()->{assertEquals(0,customers.page("",0,10).total());assertThrows(NoSuchElementException.class,()->customers.get(stored.id()));assertTrue(masters.categories().isEmpty());return null;});
    TenantContext.call(scope,1,()->{assertThrows(RuntimeException.class,()->transactions.execute(()->{em.createNativeQuery("update customer set active=false").executeUpdate();return null;}));return null;});
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH (FORCE)");sql(admin,"DROP ROLE "+role);}
  }
 }
}
