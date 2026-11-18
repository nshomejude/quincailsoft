package com.quincaisoft.reporting;

import com.quincaisoft.reporting.application.DefaultReportingService;
import com.quincaisoft.shared.*;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Actual PostgreSQL views, jOOQ reads, non-owner RLS and exact full-result totals. */
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class ReportingPostgresTest {
 @Test void exactScopedPagedStockAndSafeCsv() throws Exception {
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),user=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String suffix=UUID.randomUUID().toString().replace("-",""),db="report_test_"+suffix,role="report_role_"+suffix;
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID t=UUID.randomUUID(),c=UUID.randomUUID(),b=UUID.randomUUID(),a=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,user,"")) {
   exec(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   exec(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");exec(admin,"GRANT quincai_app TO "+role);exec(admin,"CREATE DATABASE "+db);
   try(var owner=DriverManager.getConnection(url,user,"")) {
    try(var files=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))) {
     for(var p:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())exec(owner,Files.readString(p));
    }
    seed(owner,t,c,b,"10000000000000001");seed(owner,t,c,b,"9");
    seed(owner,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"77");
    var ds=new DriverManagerDataSource(url,role,"");var jdbc=new JdbcTemplate(ds);var template=new TransactionTemplate(new DataSourceTransactionManager(ds));
    ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var s=TenantContext.current();jdbc.queryForObject("select set_config('app.tenant_id',?,true)||set_config('app.company_id',?,true)||set_config('app.branch_id',?,true)||set_config('app.actor_id',?,true)",String.class,s.tenantId().toString(),s.companyId().toString(),s.branchId().toString(),s.actorId().toString());return action.get();});}};
    var reports=new DefaultReportingService(DSL.using(new TransactionAwareDataSourceProxy(ds),SQLDialect.POSTGRES),tx);
    assertEquals(0,jdbc.queryForObject("select count(*) from report_stock_v1",Integer.class));
    assertThrows(AccessDeniedException.class, () -> reports.run("stock", null, null, 0, 1));
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
        a, null, List.of(new SimpleGrantedAuthority("inventory:read"))));
    SecurityContextHolder.setContext(context);
    try {
        assertThrows(AccessDeniedException.class, () -> reports.run("posted-sales", null, null, 0, 1));
        TenantContext.call(new TenantScope(t,c,b,a),1,()->{
         var page=reports.run("stock",null,null,0,1);assertEquals(1,page.rows().size());assertTrue(page.hasMore());assertEquals("10000000000000010",page.totals().get("value_xaf"));assertEquals(Set.of("value_xaf"),page.totals().keySet());assertTrue(reports.csv(page).contains("'=HYPERLINK"));
         var last=reports.run("stock",null,null,1,1);assertFalse(last.hasMore());assertEquals(page.totals(),last.totals());assertNotEquals(page.rows(),last.rows());assertTrue(reports.run("stock",null,null,2,1).totals().isEmpty());
         assertThrows(IllegalArgumentException.class,()->reports.run("stock",java.time.LocalDate.now(),null,0,10));
         assertThrows(NoSuchElementException.class,()->reports.run("arbitrary_table",null,null,0,10));return null;
        });
        assertEquals(0,jdbc.queryForObject("select count(*) from report_stock_v1",Integer.class));
        TenantContext.call(new TenantScope(t,c,UUID.randomUUID(),a),1,()->{assertTrue(reports.run("stock",null,null,0,10).rows().isEmpty());return null;});
    } finally {
        SecurityContextHolder.clearContext();
    }
   } finally {exec(admin,"DROP DATABASE "+db);exec(admin,"DROP ROLE "+role);}
  }
 }
 static void seed(Connection c,UUID t,UUID company,UUID branch,String value)throws SQLException {
  UUID warehouse=UUID.randomUUID(),product=UUID.randomUUID();
  exec(c,"INSERT INTO tenant(id,name)VALUES('"+t+"','test') ON CONFLICT DO NOTHING;INSERT INTO company(tenant_id,id,name)VALUES('"+t+"','"+company+"','test') ON CONFLICT DO NOTHING;INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+t+"','"+company+"','"+branch+"','test') ON CONFLICT DO NOTHING;INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name)VALUES('"+t+"','"+company+"','"+branch+"','"+warehouse+"','test');INSERT INTO catalog_product(tenant_id,company_id,id,sku,name,base_unit,selling_price_xaf)VALUES('"+t+"','"+company+"','"+product+"','"+product+"','=HYPERLINK','piece',1);INSERT INTO stock_balance(tenant_id,company_id,branch_id,warehouse_id,product_id,quantity_milli,value_xaf)VALUES('"+t+"','"+company+"','"+branch+"','"+warehouse+"','"+product+"',1000,"+value+")");
 }
 static void exec(Connection c,String q)throws SQLException {try(var s=c.createStatement()){s.execute(q);}}
}
