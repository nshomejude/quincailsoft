import java.sql.*;
import java.nio.file.*;
import java.util.*;
/** Real PostgreSQL 18 migration/negative authorization acceptance; isolated database. */
public class MigrationAcceptance {
 static final String T="10000000-0000-0000-0000-000000000001", C="20000000-0000-0000-0000-000000000001", B="30000000-0000-0000-0000-000000000001", W="40000000-0000-0000-0000-000000000001", P="50000000-0000-0000-0000-000000000001", A="60000000-0000-0000-0000-000000000001";
 static int checks;
 static void sql(Connection c,String text)throws Exception {try(var s=c.createStatement()){s.execute(text);}}
 static long count(Connection c,String q)throws Exception {try(var s=c.createStatement();var r=s.executeQuery(q)){r.next();return r.getLong(1);}}
 static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 static void denied(Connection c,String query,String state,String name)throws Exception {c.setAutoCommit(false);try {sql(c,query);c.commit();throw new AssertionError(name+" was accepted");} catch(SQLException e){check(e.getSQLState().equals(state),name+" ["+e.getSQLState()+"]");c.rollback();}finally{c.setAutoCommit(true);}}
 static void scope(Connection c,String tenant)throws Exception {sql(c,"select set_config('app.tenant_id','"+tenant+"',false),set_config('app.company_id','"+C+"',false),set_config('app.branch_id','"+B+"',false),set_config('app.actor_id','"+A+"',false)");}
 public static void main(String[] args)throws Exception {
  String adminUrl=args[0],adminUser=args[1],root=args[2],db="qc_accept_"+UUID.randomUUID().toString().replace("-","");String runtime="qc_test_"+UUID.randomUUID().toString().replace("-","");
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")) {
   check(count(admin,"select current_setting('server_version_num')::int/10000")==18,"PostgreSQL major is 18");
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+runtime+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT NOBYPASSRLS");sql(admin,"GRANT quincai_app TO "+runtime);sql(admin,"CREATE DATABASE "+db);
   String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
   try(var owner=DriverManager.getConnection(url,adminUser,"")) {
    for(Path p:Files.list(Path.of(root)).filter(x->x.toString().endsWith(".sql")).sorted(java.util.Comparator.comparingInt(migrationFile -> Integer.parseInt(migrationFile.getFileName().toString().split("__")[0].substring(1)))).toList()){owner.setAutoCommit(false);sql(owner,Files.readString(p));owner.commit();owner.setAutoCommit(true);System.out.println("MIGRATED "+p.getFileName());}
    sql(owner,"INSERT INTO tenant VALUES('"+T+"','test',now());INSERT INTO company(tenant_id,id,name)VALUES('"+T+"','"+C+"','test');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+T+"','"+C+"','"+B+"','test');INSERT INTO tenant_memberships(tenant_id,actor_id,company_id,branch_id,role_code)VALUES('"+T+"','"+A+"','"+C+"','"+B+"','owner');INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name)VALUES('"+T+"','"+C+"','"+B+"','"+W+"','test');INSERT INTO catalog_product(tenant_id,company_id,id,sku,name,base_unit,selling_price_xaf)VALUES('"+T+"','"+C+"','"+P+"','test','test','piece',1000)");
    sql(owner,"INSERT INTO customer(tenant_id,company_id,id,name)VALUES('"+T+"','"+C+"','70000000-0000-0000-0000-000000000001','Synthetic');INSERT INTO financial_period(tenant_id,company_id,id,start_date,end_date,status)VALUES('"+T+"','"+C+"','80000000-0000-0000-0000-000000000001','2026-01-01','2026-12-31','Open')");
    check(count(owner,"select count(*) from pg_tables t join pg_class c on c.relname=t.tablename where t.schemaname='public' and not(c.relrowsecurity and c.relforcerowsecurity)")==0,"All tenant tables force RLS");
    try(var app=DriverManager.getConnection(url,runtime,"")) {
     check(count(app,"select count(*) from catalog_product")==0,"Missing tenant context returns no data");scope(app,T);check(count(app,"select count(*) from catalog_product")==1,"Authorized product visible");check(count(app,"select count(*) from tenant_memberships")==1,"Own membership visible");
     scope(app,"10000000-0000-0000-0000-000000000002");check(count(app,"select count(*) from catalog_product")==0,"Other tenant concealed");scope(app,T);
     app.setAutoCommit(false);sql(app,"SELECT * FROM customer FOR SHARE;SELECT status FROM financial_period FOR SHARE;SELECT * FROM warehouse FOR UPDATE");app.commit();app.setAutoCommit(true);check(true,"Runtime can lock customer period and warehouse rows");
     denied(app,"UPDATE customer SET active=false","42501","Row-lock grant cannot change customer eligibility");
     denied(app,"UPDATE financial_period SET status='Closed'","42501","Row-lock grant cannot close periods");
     denied(app,"UPDATE warehouse SET active=false","42501","Row-lock grant cannot deactivate warehouses");
     denied(app,"UPDATE tenant_memberships SET role_code='owner'","42501","Runtime cannot grant memberships");denied(app,"TRUNCATE catalog_product","42501","Runtime cannot bypass RLS by truncation");
     denied(app,"INSERT INTO catalog_product(tenant_id,company_id,id,sku,name,base_unit,selling_price_xaf)VALUES('10000000-0000-0000-0000-000000000002','"+C+"',gen_random_uuid(),'bad','bad','piece',10)","42501","Cross-tenant insert denied");
     denied(app,"INSERT INTO stock_balance(tenant_id,company_id,branch_id,warehouse_id,product_id)VALUES('"+T+"','"+C+"','"+B+"','"+W+"','50000000-0000-0000-0000-000000000002')","23503","Stock composite reference enforced");
     String e=UUID.randomUUID().toString();denied(app,"INSERT INTO journal_entry(tenant_id,company_id,branch_id,id,operation_id,source_type,source_id,business_date)VALUES('"+T+"','"+C+"','"+B+"','"+e+"',gen_random_uuid(),'test',gen_random_uuid(),current_date)","23514","Empty journal rejected at commit");
     app.setAutoCommit(false);e=UUID.randomUUID().toString();sql(app,"INSERT INTO journal_entry(tenant_id,company_id,branch_id,id,operation_id,source_type,source_id,business_date)VALUES('"+T+"','"+C+"','"+B+"','"+e+"',gen_random_uuid(),'test',gen_random_uuid(),current_date);INSERT INTO journal_line(tenant_id,company_id,entry_id,id,account_code,debit_xaf,credit_xaf)VALUES('"+T+"','"+C+"','"+e+"',gen_random_uuid(),'AR',100,0),('"+T+"','"+C+"','"+e+"',gen_random_uuid(),'REVENUE',0,100)");app.commit();app.setAutoCommit(true);check(count(app,"select count(*) from journal_entry")==1,"Balanced journal committed atomically");
     denied(app,"INSERT INTO journal_line(tenant_id,company_id,entry_id,id,account_code,debit_xaf,credit_xaf)VALUES('"+T+"','"+C+"','"+e+"',gen_random_uuid(),'AR',50,0),('"+T+"','"+C+"','"+e+"',gen_random_uuid(),'REVENUE',0,50)","23514","Committed journal cannot receive extra balancing lines");
     denied(app,"UPDATE journal_line SET debit_xaf=200 WHERE debit_xaf=100","42501","Runtime cannot mutate journal facts");
     sql(app,"select set_config('app.company_id','20000000-0000-0000-0000-000000000002',false)");check(count(app,"select count(*) from catalog_product")==0,"Same tenant other company concealed");
    }
   } finally {sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+runtime);}
  }
  System.out.println("PASS "+checks+" PostgreSQL18 migration assertions");
 }
}
