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
class PostgresOnboardingConcurrencyTest {
 @Test void concurrentReplayAndReviewPreserveSingleDecision()throws Exception{
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
  String db="platform_test_"+UUID.randomUUID().toString().replace("-",""),role="platform_role_"+UUID.randomUUID().toString().replace("-","");
  String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;UUID maker=UUID.randomUUID(),checker=UUID.randomUUID(), otherChecker=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
   sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   LocalContainerEntityManagerFactoryBean factory=null;
   try(var owner=DriverManager.getConnection(url,adminUser,"")){
    try(var files=Files.list(Path.of("../db-migrations/src/main/resources/db/migration"))){for(var file:files.filter(x->x.toString().endsWith(".sql")).sorted(Comparator.comparingInt(x->Integer.parseInt(x.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(file));}
    sql(owner,"INSERT INTO platform_operator VALUES('"+maker+"',true),('"+checker+"',true),('"+otherChecker+"',true)");
    factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.platformcontrol.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
    var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var service=new DefaultPlatformService(em,new JpaTransactionManager(emf));
    var input = new PlatformService.Request(UUID.randomUUID(), "Concurrent merchant", "Review only");
    var identical = race(java.util.stream.IntStream.range(0, 6)
        .mapToObj(i -> (java.util.concurrent.Callable<Object>) () -> authenticated(maker, () -> service.request(input))).toList());
    assertTrue(identical.stream().allMatch(PlatformService.Item.class::isInstance), identical.toString());
    var created = (PlatformService.Item) identical.getFirst();
    identical.forEach(result -> assertEquals(created, result));
    assertEquals(1, count(owner, "SELECT count(*) FROM platform_onboarding_request"));

    UUID conflictId = UUID.randomUUID();
    var conflicting = race(List.of(
        () -> authenticated(maker, () -> service.request(new PlatformService.Request(conflictId, "A", "Reason A"))),
        () -> authenticated(maker, () -> service.request(new PlatformService.Request(conflictId, "B", "Reason B")))));
    assertEquals(1, conflicting.stream().filter(PlatformService.Item.class::isInstance).count());
    assertEquals(1, conflicting.stream().filter(IllegalStateException.class::isInstance).count());
    var winner = (PlatformService.Item) conflicting.stream().filter(PlatformService.Item.class::isInstance).findFirst().orElseThrow();
    as(maker);
    assertEquals(winner, service.request(new PlatformService.Request(conflictId, winner.displayName(), winner.reason())));

    var approvalA = new PlatformService.Review(UUID.randomUUID(), 0);
    var approvalB = new PlatformService.Review(UUID.randomUUID(), 0);
    var approvals = race(List.of(
        () -> authenticated(checker, () -> service.approve(created.id(), approvalA)),
        () -> authenticated(otherChecker, () -> service.approve(created.id(), approvalB))));
    assertEquals(1, approvals.stream().filter(PlatformService.Item.class::isInstance).count());
    assertEquals(1, approvals.stream().filter(IllegalStateException.class::isInstance).count());
    var approved = (PlatformService.Item) approvals.stream().filter(PlatformService.Item.class::isInstance).findFirst().orElseThrow();
    assertEquals("APPROVED_FOR_PROVISIONING", approved.status());
    assertEquals("1", approved.version());
    assertNotEquals(maker, approved.approvedBy());
    var winningReview = approved.approvedBy().equals(checker) ? approvalA : approvalB;
    as(approved.approvedBy());
    assertEquals(approved, service.approve(created.id(), winningReview));
    sql(owner, "UPDATE platform_operator SET active=false WHERE actor_id='" + approved.approvedBy() + "'");
    assertThrows(AccessDeniedException.class, () -> service.approve(created.id(), winningReview));
    as(maker);
    assertEquals(created, service.request(input));
    sql(owner, "UPDATE platform_operator SET active=false WHERE actor_id='" + maker + "'");
    assertThrows(AccessDeniedException.class, () -> service.request(input));
    assertEquals(2, count(owner, "SELECT count(*) FROM platform_onboarding_request"));
    assertEquals(1, count(owner, "SELECT count(*) FROM platform_onboarding_request WHERE status='APPROVED_FOR_PROVISIONING'"));
    assertEquals(0, count(owner, "SELECT count(*) FROM tenant"));
    assertEquals(0, count(owner, "SELECT count(*) FROM tenant_memberships"));

   }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db);sql(admin,"DROP ROLE "+role);}
  }
 }
 /** Start workers together; bound waits so a lock regression fails instead of hanging CI. */
 static List<Object> race(List<java.util.concurrent.Callable<Object>> actions) throws Exception {
  var start = new java.util.concurrent.CyclicBarrier(actions.size());
  var executor = java.util.concurrent.Executors.newFixedThreadPool(actions.size());
  try {
   var futures = actions.stream().map(action -> executor.submit(() -> {
    start.await(10, java.util.concurrent.TimeUnit.SECONDS);
    try { return action.call(); }
    catch (RuntimeException failure) { return failure; }
   })).toList();
   var results = new ArrayList<Object>();
   for (var future : futures) results.add(future.get(30, java.util.concurrent.TimeUnit.SECONDS));
   return results;
  } finally {
   executor.shutdownNow();
   assertTrue(executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS), "Workers must terminate");
  }
 }
 static Object authenticated(UUID actor, java.util.concurrent.Callable<Object> action) throws Exception {
  as(actor);
  try { return action.call(); }
  finally { SecurityContextHolder.clearContext(); }
 }
 static void as(UUID id){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg","RS256").subject(id.toString()).build()));}
 static void sql(Connection c,String q)throws SQLException{try(var s=c.createStatement()){s.execute(q);}}
 static long count(Connection c,String q)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(q)){r.next();return r.getLong(1);}}
}
