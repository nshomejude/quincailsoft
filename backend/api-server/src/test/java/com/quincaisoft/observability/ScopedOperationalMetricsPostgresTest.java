package com.quincaisoft.observability;

import com.quincaisoft.security.PostgresScopedTransactions;
import com.quincaisoft.shared.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class ScopedOperationalMetricsPostgresTest {
 @Test void executesRealSqlUnderRuntimeRlsAndReportsSampleTruncation() throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),user=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String suffix=UUID.randomUUID().toString().replace("-",""),db="metrics_"+suffix,role="metrics_role_"+suffix;
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID t=UUID.randomUUID(),c=UUID.randomUUID(),b=UUID.randomUUID(),a=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,user,"")) {
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   try(var owner=DriverManager.getConnection(url,user,"")) {
    try(var files=Files.list(Path.of("../db-migrations/src/main/resources/db/migration"))) {
     for(var p:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(p));
    }
    seed(owner,t,c,b,a,3);seed(owner,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),7);
    var ds=new DriverManagerDataSource(url,role,"");var jdbc=new JdbcTemplate(ds);var tx=new PostgresScopedTransactions(jdbc,new DataSourceTransactionManager(ds));
    var collector=new ScopedOperationalMetrics(jdbc,tx,1000);var limited=new ScopedOperationalMetrics(jdbc,tx,2);
    SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(a,null,List.of(new SimpleGrantedAuthority("observability:read"))));
    try {
     TenantContext.call(new TenantScope(t,c,b,a),1,()->{
      var response=collector.metrics();String body=response.getBody();assertNotNull(body);
      assertTrue(body.contains("quincai_scoped_outbox_sample 3\n"));assertTrue(body.contains("quincai_scoped_outbox_sample_complete 1\n"));
      assertTrue(body.contains("quincai_scoped_unknown_sample 2\n"));assertTrue(body.contains("quincai_scoped_unknown_live 1\n"));assertTrue(body.contains("quincai_scoped_unknown_sandbox 1\n"));assertTrue(body.contains("quincai_scoped_journal_sample 1\n"));assertTrue(body.contains("quincai_scoped_journal_imbalanced 0\n"));
      assertFalse(body.contains(t.toString()));assertFalse(body.contains("private fixture"));assertEquals("no-store",response.getHeaders().getFirst("Cache-Control"));
      String bounded=limited.metrics().getBody();assertTrue(bounded.contains("quincai_scoped_outbox_sample 2\n"));assertTrue(bounded.contains("quincai_scoped_outbox_sample_complete 0\n"));return null;
     });
     assertThrows(AccessDeniedException.class,()->TenantContext.call(new TenantScope(t,c,b,a),2,collector::metrics));
     assertThrows(AccessDeniedException.class,()->TenantContext.call(new TenantScope(t,c,UUID.randomUUID(),a),1,collector::metrics));
     assertEquals(0,jdbc.queryForObject("select count(*) from outbox_event",Integer.class));
    } finally {SecurityContextHolder.clearContext();}
   } finally {sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 private static void seed(Connection connection,UUID t,UUID c,UUID b,UUID a,int count)throws SQLException {
  sql(connection,"INSERT INTO tenant(id,name)VALUES('"+t+"','private fixture');INSERT INTO company(tenant_id,id,name)VALUES('"+t+"','"+c+"','private fixture');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+t+"','"+c+"','"+b+"','private fixture');INSERT INTO tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code)VALUES('"+t+"','"+c+"','"+b+"','"+a+"','owner')");
  UUID warehouse=UUID.randomUUID(),sale=UUID.randomUUID(),journal=UUID.randomUUID();
  sql(connection,"INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name)VALUES('"+t+"','"+c+"','"+b+"','"+warehouse+"','fixture');INSERT INTO sale(tenant_id,company_id,branch_id,id,operation_id,warehouse_id,actor_id,business_date,status)VALUES('"+t+"','"+c+"','"+b+"','"+sale+"','"+UUID.randomUUID()+"','"+warehouse+"','"+a+"',CURRENT_DATE,'DRAFT')");
  for(String environment:List.of("sandbox","mtncameroon"))sql(connection,"INSERT INTO provider_intent(tenant_id,company_id,branch_id,id,operation_id,sale_id,actor_id,business_date,amount_xaf,currency,environment,phone,status,reason)VALUES('"+t+"','"+c+"','"+b+"','"+UUID.randomUUID()+"','"+UUID.randomUUID()+"','"+sale+"','"+a+"',CURRENT_DATE,1,'"+(environment.equals("sandbox")?"EUR":"XAF")+"','"+environment+"','private fixture','UNKNOWN','fixture')");
  sql(connection,"BEGIN;INSERT INTO journal_entry(tenant_id,company_id,branch_id,id,operation_id,source_type,source_id,business_date)VALUES('"+t+"','"+c+"','"+b+"','"+journal+"','"+UUID.randomUUID()+"','METRICS_FIXTURE','"+sale+"',CURRENT_DATE);INSERT INTO journal_line(tenant_id,company_id,entry_id,id,account_code,debit_xaf,credit_xaf)VALUES('"+t+"','"+c+"','"+journal+"','"+UUID.randomUUID()+"','411',9007199254740993,0),('"+t+"','"+c+"','"+journal+"','"+UUID.randomUUID()+"','701',0,9007199254740993);COMMIT");
  for(int i=0;i<count;i++)sql(connection,"INSERT INTO outbox_event(tenant_id,company_id,branch_id,id,operation_id,event_type,aggregate_type,aggregate_id,payload,created_at)VALUES('"+t+"','"+c+"','"+b+"','"+UUID.randomUUID()+"','"+UUID.randomUUID()+"','fixture','fixture','"+UUID.randomUUID()+"','{}',now()-interval '10 minutes')");
 }
 private static void sql(Connection c,String query)throws SQLException {try(var s=c.createStatement()){s.execute(query);}}
}
