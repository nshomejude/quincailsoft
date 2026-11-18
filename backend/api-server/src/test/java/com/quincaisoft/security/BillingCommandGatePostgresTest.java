package com.quincaisoft.security;

import com.quincaisoft.shared.*;
import com.quincaisoft.subscriptions.api.SubscriptionLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.access.AccessDeniedException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class BillingCommandGatePostgresTest {
 public record Command(UUID operationId) {}
 public interface Commands { @NewBusinessCommand String draft(Command command); String historicalRead(); }
 public static class Service implements Commands {
  final JdbcTemplate jdbc;final UUID t,c;Service(JdbcTemplate jdbc,UUID t,UUID c){this.jdbc=jdbc;this.t=t;this.c=c;}
  public String draft(Command command){jdbc.update("insert into catalog_product(tenant_id,company_id,id,sku,name,base_unit,selling_price_xaf) values(?,?,?,'BILLING-TEST','Test','unit',100)",t,c,UUID.randomUUID());return "saved";}
  public String historicalRead(){return "history";}
 }
 @Test void enforcementSharesTransactionPreservesReceiptsAndNeverBlocksHistory() throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),user=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String suffix=UUID.randomUUID().toString().replace("-",""),db="billinggate_"+suffix,role="bg_"+suffix;
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID t=UUID.randomUUID(),c=UUID.randomUUID(),b=UUID.randomUUID(),a=UUID.randomUUID(),op=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,user,"")) {
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT;GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   try(var owner=DriverManager.getConnection(url,user,"")) {
    try(var files=Files.list(Path.of("../db-migrations/src/main/resources/db/migration"))){for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));}
    sql(owner,"insert into tenant(id,name) values('"+t+"','Test');insert into company(tenant_id,id,name)values('"+t+"','"+c+"','Test');insert into branch(tenant_id,company_id,id,name)values('"+t+"','"+c+"','"+b+"','Test');insert into tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code)values('"+t+"','"+c+"','"+b+"','"+a+"','owner')");
    var ds=new DriverManagerDataSource(url,role,"");var jdbc=new JdbcTemplate(ds);var tx=new PostgresScopedTransactions(jdbc,new DataSourceTransactionManager(ds));
    var lifecycle=mock(SubscriptionLifecycleService.class);var gate=new BillingCommandGate(tx,jdbc,lifecycle);
    var beans=new DefaultListableBeanFactory();beans.registerSingleton("gate",gate);
    var proxy=(Commands)new BillingCommandProxy(beans.getBeanProvider(BillingCommandGate.class)).postProcessAfterInitialization(new Service(jdbc,t,c),"commands");
    doThrow(new AccessDeniedException("reviewed restriction")).when(lifecycle).requireAllowed(SubscriptionLifecycleService.Operation.NEW_BUSINESS_POSTING);
    assertEquals("history",proxy.historicalRead());
    TenantContext.call(new TenantScope(t,c,b,a),1,()-> {
      assertThrows(AccessDeniedException.class,()->proxy.draft(new Command(op)));
      assertEquals(0L,tx.execute(()->jdbc.queryForObject("select count(*) from catalog_product",Long.class)));
      reset(lifecycle);
      assertThrows(IllegalStateException.class,()->gate.execute(new Object[]{new Command(op)},()->{proxy.draft(new Command(op));throw new IllegalStateException("fail after write");}));
      assertEquals(0L,tx.execute(()->jdbc.queryForObject("select count(*) from catalog_product",Long.class)));
      tx.execute(()->{jdbc.update("insert into idempotency_result(tenant_id,company_id,branch_id,operation_id,request_hash,source_type,response_json)values(?,?,?,?,'"+"a".repeat(64)+"','SALE','{}')",t,c,b,op);return null;});
      reset(lifecycle);doThrow(new AccessDeniedException("restricted")).when(lifecycle).requireAllowed(SubscriptionLifecycleService.Operation.NEW_BUSINESS_POSTING);
      // The real domain command must still validate its receipt/hash; the gate must allow retrieval.
      assertEquals("original",gate.execute(new Object[]{new Command(op)},()->"original"));
      verifyNoInteractions(lifecycle);
      assertThrows(AccessDeniedException.class,()->gate.execute(new Object[]{new Command(UUID.randomUUID())},()->"new"));
      return null;
    });
    TenantContext.call(new TenantScope(t,c,b,a),2,()->{assertThrows(AccessDeniedException.class,()->gate.execute(new Object[]{new Command(op)},()->"original"));return null;});
   }finally{sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 private static void sql(Connection c,String q)throws Exception{try(var s=c.createStatement()){s.execute(q);}}
}
