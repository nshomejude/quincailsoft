package com.quincaisoft.commerce;

import com.quincaisoft.commerce.api.CommerceService;
import com.quincaisoft.commerce.application.DefaultCommerceService;
import com.quincaisoft.catalog.application.DefaultCatalogService;
import com.quincaisoft.identity.api.IdentityService;
import com.quincaisoft.shared.*;
import com.quincaisoft.subscriptions.application.DefaultEntitlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Real PG18/JPA/runtime-role proof that public catalog is an explicit, reversible snapshot. */
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresCommerceIntegrationTest {
 @Test void publicationDoesNotExposePrivateCatalogOrImplyCheckout() throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String db="commerce_test_"+UUID.randomUUID().toString().replace("-",""),role="commerce_role_"+UUID.randomUUID().toString().replace("-","");
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID t=UUID.randomUUID(),c=UUID.randomUUID(),b=UUID.randomUUID(),a=UUID.randomUUID(),p=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")) {
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")) {
    try(var files=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));}
    sql(owner,"INSERT INTO tenant(id,name)VALUES('"+t+"','Private tenant');INSERT INTO company(tenant_id,id,name)VALUES('"+t+"','"+c+"','Private company');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+t+"','"+c+"','"+b+"','Private branch');INSERT INTO tenant_memberships(tenant_id,actor_id,company_id,branch_id,role_code)VALUES('"+t+"','"+a+"','"+c+"','"+b+"','owner');INSERT INTO catalog_product(tenant_id,company_id,id,sku,name,base_unit,selling_price_xaf)VALUES('"+t+"','"+c+"','"+p+"','PRIVATE-SKU','Private source name','piece',9007199254740993)");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.commerce.infrastructure","com.quincaisoft.catalog.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var s=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",s.tenantId().toString()).setParameter("c",s.companyId().toString()).setParameter("b",s.branchId().toString()).setParameter("a",s.actorId().toString()).getSingleResult();return action.get();});}};
    IdentityService identity=new IdentityService(){public List<Member> members(){return List.of(new Member(a,"owner",true,1));}public List<Change> changes(){throw new UnsupportedOperationException();}public Change request(Request input){throw new UnsupportedOperationException();}public Change approve(UUID id,UUID operation,long version){throw new UnsupportedOperationException();}};
    var json=JsonMapper.builder().build();var service=new DefaultCommerceService(em,tx,new DefaultCatalogService(em,tx),identity,json,new DefaultEntitlementService(em,tx));var scope=new TenantScope(t,c,b,a);
    var configure=new CommerceService.Configure(UUID.randomUUID(),"public-shop","Published shop",false,0,"Review publication");
    var store=TenantContext.call(scope,1,()->service.configure(configure));
    assertThrows(org.springframework.security.access.AccessDeniedException.class,()->TenantContext.call(scope,1,()->service.configure(new CommerceService.Configure(UUID.randomUUID(),"public-shop","Published shop",true,0,"No entitlement"))));
    sql(owner,"INSERT INTO subscription_entitlement VALUES('"+t+"',gen_random_uuid(),'COMMERCE',true,now()+interval '1 day')");
    var publish=new CommerceService.Publish(UUID.randomUUID(),p,"Public item","Explicit public description",true,0,"Publish reviewed snapshot");
    var listing=TenantContext.call(scope,1,()->service.publish(publish));assertNotEquals(p,listing.id());
    assertThrows(NoSuchElementException.class,()->service.publicStore("public-shop"));
    var enabled=TenantContext.call(scope,1,()->service.configure(new CommerceService.Configure(UUID.randomUUID(),"public-shop","Published shop",true,Long.parseLong(store.version()),"Enable reviewed storefront")));
    var publicStore=service.publicStore("public-shop");assertEquals("CHECKOUT_NOT_ENABLED",publicStore.checkoutStatus());assertEquals(1,publicStore.items().size());assertEquals("9007199254740993",publicStore.items().getFirst().priceXaf());
    String payload=json.writeValueAsString(publicStore);for(String privateValue:List.of(t.toString(),c.toString(),b.toString(),p.toString(),"PRIVATE-SKU","Private source name","quantityMilli","costXaf"))assertFalse(payload.contains(privateValue),privateValue);
    assertEquals(listing,TenantContext.call(scope,1,()->service.publish(publish)));
    assertEquals(store,TenantContext.call(scope,1,()->service.configure(configure)),"Replay preserves original response, not current enable state");
    sql(owner,"UPDATE catalog_product SET selling_price_xaf=123 WHERE id='"+p+"'");assertEquals("9007199254740993",service.publicStore("public-shop").items().getFirst().priceXaf(),"Catalog edit cannot silently republish");
    var wrongActor=new TenantScope(t,c,b,UUID.randomUUID());assertThrows(org.springframework.security.access.AccessDeniedException.class,()->TenantContext.call(wrongActor,1,service::listings));
    var wrongTenant=new TenantScope(UUID.randomUUID(),c,b,a);assertTrue(TenantContext.call(wrongTenant,1,service::listings).isEmpty());
    try(var runtime=DriverManager.getConnection(url,role,"")){assertEquals(0,count(runtime,"SELECT count(*) FROM commerce_listing"));assertEquals(0,count(runtime,"SELECT count(*) FROM catalog_product"));assertEquals(1,count(runtime,"SELECT count(*) FROM public_listing_projection"));assertThrows(SQLException.class,()->sql(runtime,"INSERT INTO public_store_projection VALUES('forged-shop','Forbidden',gen_random_uuid())"));assertThrows(SQLException.class,()->sql(runtime,"UPDATE subscription_entitlement SET enabled=true"));assertThrows(SQLException.class,()->sql(runtime,"INSERT INTO platform_operator VALUES(gen_random_uuid(),true)"));}
    sql(owner,"UPDATE subscription_entitlement SET enabled=false");assertThrows(NoSuchElementException.class,()->service.publicStore("public-shop"));
    sql(owner,"UPDATE subscription_entitlement SET enabled=true,expires_at=now()-interval '1 second'");assertThrows(NoSuchElementException.class,()->service.publicStore("public-shop"));
    sql(owner,"UPDATE subscription_entitlement SET expires_at=now()+interval '1 day'");
    TenantContext.call(scope,1,()->service.publish(new CommerceService.Publish(UUID.randomUUID(),p,"Public item","Explicit public description",false,Long.parseLong(listing.version()),"Withdraw listing")));
    assertTrue(service.publicStore("public-shop").items().isEmpty());
    TenantContext.call(scope,1,()->service.configure(new CommerceService.Configure(UUID.randomUUID(),"public-shop","Published shop",false,Long.parseLong(enabled.version()),"Disable storefront")));
    assertThrows(NoSuchElementException.class,()->service.publicStore("public-shop"));
   }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static void sql(Connection c,String q)throws SQLException{try(var s=c.createStatement()){s.execute(q);}}
 static long count(Connection c,String q)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(q)){r.next();return r.getLong(1);}}
}
