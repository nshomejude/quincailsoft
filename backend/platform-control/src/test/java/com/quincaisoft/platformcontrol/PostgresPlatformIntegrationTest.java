package com.quincaisoft.platformcontrol;

import com.quincaisoft.platformcontrol.api.PlatformService;
import com.quincaisoft.platformcontrol.application.DefaultPlatformService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Platform operations require an enrolled operator and an independent reviewer; no tenant is fabricated. */
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class PostgresPlatformIntegrationTest {
 @Test void independentReviewCannotBecomeMerchantProvisioning()throws Exception{
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String db="platform_test_"+UUID.randomUUID().toString().replace("-",""),role="platform_role_"+UUID.randomUUID().toString().replace("-","");
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;UUID maker=UUID.randomUUID(),checker=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")){
    try(var files=Files.list(Path.of("../db-migrations/src/main/resources/db/migration"))){for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));}
    sql(owner,"INSERT INTO platform_operator VALUES('"+maker+"',true),('"+checker+"',true)");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.platformcontrol.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var service=new DefaultPlatformService(em,new JpaTransactionManager(emf));
    as(UUID.randomUUID());assertThrows(AccessDeniedException.class,service::list);
    as(maker);var input=new PlatformService.Request(UUID.randomUUID(),"Prospective merchant","Review before provisioning");var created=service.request(input);assertEquals("REQUESTED",created.status());assertEquals(created,service.request(input));
    assertThrows(AccessDeniedException.class,()->service.approve(created.id(),new PlatformService.Review(UUID.randomUUID(),0)));
    as(checker);assertEquals(1,service.list().size());var review=new PlatformService.Review(UUID.randomUUID(),0);var approved=service.approve(created.id(),review);assertEquals("APPROVED_FOR_PROVISIONING",approved.status());assertEquals(checker,approved.approvedBy());assertEquals(approved,service.approve(created.id(),review));
    assertThrows(IllegalStateException.class,()->service.approve(created.id(),new PlatformService.Review(UUID.randomUUID(),0)));
    as(maker);assertEquals(created,service.request(input),"Request replay remains the original response after independent approval");assertThrows(AccessDeniedException.class,()->service.approve(created.id(),review));
    assertEquals(0,count(owner,"SELECT count(*) FROM tenant"));assertEquals(0,count(owner,"SELECT count(*) FROM tenant_memberships"));
    sql(owner,"UPDATE platform_operator SET active=false WHERE actor_id='"+maker+"'");assertThrows(AccessDeniedException.class,service::list);
    try(var runtime=DriverManager.getConnection(url,role,"")){assertEquals(0,count(runtime,"SELECT count(*) FROM platform_operator"));assertEquals(0,count(runtime,"SELECT count(*) FROM platform_onboarding_request"));assertThrows(SQLException.class,()->sql(runtime,"UPDATE platform_operator SET active=true"));}
   }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 static void as(UUID id){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg","RS256").subject(id.toString()).build()));}
 static void sql(Connection c,String q)throws SQLException{try(var s=c.createStatement()){s.execute(q);}}
 static long count(Connection c,String q)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(q)){r.next();return r.getLong(1);}}
}
