import java.sql.*;
import java.nio.file.*;
import java.util.*;
/** Creates a disposable PostgreSQL database for Spring/JPA integration tests only. */
public class IntegrationFixture {
 public static void main(String[] args)throws Exception {
  String db="qc_jpa_"+UUID.randomUUID().toString().replace("-",""), role="qc_jpa_"+UUID.randomUUID().toString().replace("-","");
  String base=args[0],user=args[1],url=base.substring(0,base.lastIndexOf('/')+1)+db;
  try(var c=DriverManager.getConnection(base,user,"");var s=c.createStatement()) {
   s.execute("DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   s.execute("CREATE ROLE "+role+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT NOBYPASSRLS");s.execute("GRANT quincai_app TO "+role);s.execute("CREATE DATABASE "+db);
  }
  try(var c=DriverManager.getConnection(url,user,"");var s=c.createStatement()) {
   for(Path p:Files.list(Path.of(args[2])).filter(x->x.toString().endsWith(".sql")).sorted().toList()){c.setAutoCommit(false);s.execute(Files.readString(p));c.commit();c.setAutoCommit(true);}
   s.execute("INSERT INTO tenant(id,name)VALUES('10000000-0000-0000-0000-000000000001','JPA integration');INSERT INTO company(tenant_id,id,name)VALUES('10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','JPA integration');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','30000000-0000-0000-0000-000000000001','JPA integration');INSERT INTO tenant_memberships(tenant_id,actor_id,company_id,branch_id,role_code)VALUES('10000000-0000-0000-0000-000000000001','60000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','30000000-0000-0000-0000-000000000001','owner');INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name)VALUES('10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','30000000-0000-0000-0000-000000000001','40000000-0000-0000-0000-000000000001','JPA integration');INSERT INTO catalog_product(tenant_id,company_id,id,sku,name,base_unit,selling_price_xaf)VALUES('10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000001','JPA-001','Test item','piece',1000);INSERT INTO customer(tenant_id,company_id,id,name)VALUES('10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','70000000-0000-0000-0000-000000000001','Synthetic customer');INSERT INTO stock_balance(tenant_id,company_id,branch_id,warehouse_id,product_id,quantity_milli,value_xaf)VALUES('10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','30000000-0000-0000-0000-000000000001','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000001',100000,50000);INSERT INTO financial_period(tenant_id,company_id,id,start_date,end_date,status)VALUES('10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','80000000-0000-0000-0000-000000000001','2026-01-01','2026-12-31','Open')");
  }
  Properties p=new Properties();p.setProperty("spring.datasource.url",url);p.setProperty("spring.datasource.username",role);p.setProperty("spring.datasource.password","");p.setProperty("spring.flyway.enabled","false");p.setProperty("spring.jpa.hibernate.ddl-auto","validate");p.setProperty("fixture.admin.url",base);p.setProperty("fixture.admin.user",user);p.setProperty("fixture.database",db);p.setProperty("fixture.role",role);
  try(var out=Files.newOutputStream(Path.of(args[3]))){p.store(out,"Disposable synthetic integration database; loopback trust test cluster only");}
  System.out.println("Wrote synthetic fixture configuration to "+args[3]);
 }
}
