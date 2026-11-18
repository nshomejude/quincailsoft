package com.quincaisoft.security;
import com.quincaisoft.QuincaiSoftApplication;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import java.net.*;import java.net.http.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.sql.*;import java.time.Instant;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** Complete Boot/JPA/resource-server boundary using generated test issuer and isolated PG18. */
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class FullApplicationPostgresTest {
 static void sql(Connection c,String text)throws Exception{try(var s=c.createStatement()){s.execute(text);}}
 @Test void applicationStartsAndAuthenticatesAgainstRealScopedDatabase()throws Exception{
  var issuerServer=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var key=new RSAKeyGenerator(2048).keyID("synthetic").generate();
  String issuer="http://127.0.0.1:"+issuerServer.getAddress().getPort();
  issuerServer.createContext("/.well-known/openid-configuration",x->{var bytes=("{\"issuer\":\""+issuer+"\",\"jwks_uri\":\""+issuer+"/keys\",\"authorization_endpoint\":\""+issuer+"/authorize\",\"response_types_supported\":[\"code\"],\"subject_types_supported\":[\"public\"],\"id_token_signing_alg_values_supported\":[\"RS256\"]}").getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().add("Content-Type","application/json");x.sendResponseHeaders(200,bytes.length);x.getResponseBody().write(bytes);x.close();});
  issuerServer.createContext("/keys",x->{var bytes=new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().add("Content-Type","application/json");x.sendResponseHeaders(200,bytes.length);x.getResponseBody().write(bytes);x.close();});issuerServer.start();
  String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),user=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent"),db="boot_test_"+UUID.randomUUID().toString().replace("-",""),role="boot_role_"+UUID.randomUUID().toString().replace("-","");String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
  UUID tenant=UUID.randomUUID(),company=UUID.randomUUID(),branch=UUID.randomUUID(),actor=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,user,"")){
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   try{
    try(var owner=DriverManager.getConnection(url,user,"")){
     try(var files=Files.list(Path.of("../db-migrations/src/main/resources/db/migration"))){for(var p:files.filter(p->p.toString().endsWith(".sql")).sorted(java.util.Comparator.comparingInt(migrationFile -> Integer.parseInt(migrationFile.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(p));}
     sql(owner,"INSERT INTO tenant(id,name)VALUES('"+tenant+"','Synthetic');INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+company+"','Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+branch+"','Synthetic');INSERT INTO tenant_memberships(tenant_id,actor_id,company_id,branch_id,role_code)VALUES('"+tenant+"','"+actor+"','"+company+"','"+branch+"','owner')");
    }
    var settings=new HashMap<String,Object>();settings.put("QUINCAI_DATABASE_URL",url);settings.put("QUINCAI_DATABASE_USER",role);settings.put("QUINCAI_DATABASE_PASSWORD","");settings.put("QUINCAI_OIDC_ISSUER",issuer);settings.put("QUINCAI_OIDC_AUDIENCE","quincai-test");settings.put("server.port","0");settings.put("server.address","127.0.0.1");settings.put("spring.flyway.enabled","false");
    try(var app=new SpringApplicationBuilder(QuincaiSoftApplication.class).properties(settings).run()){
     int port=((WebServerApplicationContext)app).getWebServer().getPort();var client=HttpClient.newHttpClient();URI endpoint=URI.create("http://127.0.0.1:"+port+"/api/v1/session");
     assertEquals(401,client.send(HttpRequest.newBuilder(endpoint).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
     String token=token(key,issuer,"quincai-test",tenant,company,branch,actor);
     URI salesSummary=URI.create("http://127.0.0.1:"+port+"/api/v1/sales/summary?from=2026-11-18&to=2026-11-18");
     assertEquals(401,client.send(HttpRequest.newBuilder(salesSummary).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
     var summary=client.send(HttpRequest.newBuilder(salesSummary).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
     assertEquals(200,summary.statusCode(),summary.body());
     assertTrue(summary.body().contains("\"totalXaf\":\"0\""),summary.body());

     var response=client.send(HttpRequest.newBuilder(endpoint).header("Authorization","Bearer "+token).header("X-Tenant-ID",UUID.randomUUID().toString()).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,response.statusCode(),response.body());assertTrue(response.body().contains(tenant.toString()));
     assertEquals(403,client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/admin/tenants")).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
     assertEquals(401,client.send(HttpRequest.newBuilder(endpoint).header("Authorization","Bearer "+token(key,issuer,"wrong-audience",tenant,company,branch,actor)).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
     try(var owner=DriverManager.getConnection(url,user,"")){sql(owner,"DELETE FROM tenant_memberships WHERE actor_id='"+actor+"'");}
     assertEquals(403,client.send(HttpRequest.newBuilder(endpoint).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
    }
   }finally{sql(admin,"DROP DATABASE "+db+" WITH (FORCE)");sql(admin,"DROP ROLE "+role);}
  }finally{issuerServer.stop(0);}
 }
 static String token(RSAKey key,String issuer,String audience,UUID t,UUID c,UUID b,UUID a)throws Exception{
  var claims=new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(a.toString()).issueTime(java.util.Date.from(Instant.now())).expirationTime(java.util.Date.from(Instant.now().plusSeconds(300))).claim("tenant_id",t.toString()).claim("company_id",c.toString()).claim("branch_id",b.toString()).claim("authorization_epoch",1).claim("scope","catalog:read sales:read").build();var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),claims);jwt.sign(new RSASSASigner(key));return jwt.serialize();
 }
}
