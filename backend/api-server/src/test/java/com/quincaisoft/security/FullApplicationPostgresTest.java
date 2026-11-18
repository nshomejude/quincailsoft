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
  boolean runHistory = "true".equals(System.getenv("QUINCAI_HISTORY_CATALOG_ACCEPTANCE"));
  boolean runOpening = "true".equals(System.getenv("QUINCAI_HISTORY_OPENING_ACCEPTANCE"));
  boolean runReceipt = runHistory && runOpening;
  if (runHistory || runOpening) {
   tenant=UUID.fromString("0984ca97-010e-51fc-a6a0-7c33453e214e");
   company=UUID.fromString("f6b40fb6-3f23-5891-a908-2de13a773d7d");
   branch=UUID.fromString("fdbf6a74-83c1-5ef6-a69c-3ea119eb69f3");
  }
  UUID product=UUID.randomUUID(), supplier=UUID.randomUUID(), approver=UUID.randomUUID();
  try(var admin=DriverManager.getConnection(adminUrl,user,"")){
   sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
   try{
    try(var owner=DriverManager.getConnection(url,user,"")){
     try(var files=Files.list(Path.of("../db-migrations/src/main/resources/db/migration"))){for(var p:files.filter(p->p.toString().endsWith(".sql")).sorted(java.util.Comparator.comparingInt(migrationFile -> Integer.parseInt(migrationFile.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(p));}
     sql(owner,"INSERT INTO tenant(id,name)VALUES('"+tenant+"','Synthetic');INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+company+"','Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+branch+"','Synthetic');INSERT INTO tenant_memberships(tenant_id,actor_id,company_id,branch_id,role_code)VALUES('"+tenant+"','"+actor+"','"+company+"','"+branch+"','owner')");
     if(runReceipt) sql(owner,"INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name) VALUES ('"+tenant+"','"+company+"','"+branch+"','32d21a83-c93c-5b83-af73-be07d3b094fd','Historical acceptance warehouse')");
     if(runOpening){
      sql(owner,"INSERT INTO tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code) VALUES ('"+tenant+"','"+company+"','"+branch+"','"+approver+"','owner')");
      sql(owner,"INSERT INTO financial_period(tenant_id,company_id,id,start_date,end_date,status) VALUES ('"+tenant+"','"+company+"','"+UUID.randomUUID()+"','2024-01-01','2024-01-31','Open')");
     }
     sql(owner,"INSERT INTO catalog_product(tenant_id,company_id,id,sku,name,base_unit,selling_price_xaf) VALUES ('"+tenant+"','"+company+"','"+product+"','TEST-SKU','Synthetic cable','piece',1000)");
     sql(owner,"INSERT INTO supplier(tenant_id,company_id,id,code,name) VALUES ('"+tenant+"','"+company+"','"+supplier+"','TEST-SUP','Synthetic supplier')");

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

     var writer=token(key,issuer,"quincai-test",tenant,company,branch,actor,"catalog:read catalog:write sales:read");
     var manufacturer=catalogWrite(client,port,"manufacturers",token,writer,Map.of("code","TEST-MFR","name","Synthetic maker"));
     var family=catalogWrite(client,port,"families",token,writer,Map.of("manufacturerId",manufacturer.get("id").asText(),"productType","Cable","modelNumber","M1"));
     var variant=catalogWrite(client,port,"variants",token,writer,Map.of("productId",product,"familyId",family.get("id").asText(),"colour","Blue","size","10mm"));
     assertEquals(product.toString(),variant.get("productId").asText());
     var offer=catalogWrite(client,port,"supplier-offers",token,writer,Map.of("productId",product,"supplierId",supplier,"supplierSku","TEST-OFFER","quotedCostXaf","9007199254740993","minimumOrderMilli","1000","leadTimeDays",7,"preferred",true));
     assertTrue(offer.get("quotedCostXaf").isString());
     assertEquals("9007199254740993",offer.get("quotedCostXaf").asText());

     if(runOpening){
      var maker=token(key,issuer,"quincai-test",tenant,company,branch,actor,"journals:write");
      var checker=token(key,issuer,"quincai-test",tenant,company,branch,approver,"journals:approve");
      verifyHistoricalOpening(port,maker,checker,url,user);
     }
     if(runHistory){
      var historyWriter=token(key,issuer,"quincai-test",tenant,company,branch,actor,"catalog:write purchasing:write customers:write");
      var catalog = verifyHistoricalCatalog(port,historyWriter,url,user);
      if(runReceipt){
       var receiver=token(key,issuer,"quincai-test",tenant,company,branch,actor,"purchasing:write purchasing:receive");
       var checker=token(key,issuer,"quincai-test",tenant,company,branch,approver,"purchasing:approve");
       verifyHistoricalReceipt(port,receiver,checker,catalog,url,user);
      }
     }

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
  return token(key,issuer,audience,t,c,b,a,"catalog:read sales:read");
 }
 static String token(RSAKey key,String issuer,String audience,UUID t,UUID c,UUID b,UUID a,String scopes)throws Exception{
  var claims=new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(a.toString()).issueTime(java.util.Date.from(Instant.now())).expirationTime(java.util.Date.from(Instant.now().plusSeconds(3600))).claim("tenant_id",t.toString()).claim("company_id",c.toString()).claim("branch_id",b.toString()).claim("authorization_epoch",1).claim("scope",scopes).build();var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),claims);jwt.sign(new RSASSASigner(key));return jwt.serialize();
 }
 private static void verifyHistoricalOpening(int port,String maker,String approver,String url,String user)throws Exception{
  Path output=Files.createTempDirectory(Path.of("target"),"opening-acceptance-").toAbsolutePath();
  Path root=Path.of("../..").toAbsolutePath().normalize();
  for(String pass:List.of("initial","replay")){
   var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
       "--class-path",System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")),
       root.resolve("backend/tools/history-import/src/main/java/com/quincaisoft/tools/CatalogHistoryImport.java").toString(),
       "http://127.0.0.1:"+port,root.resolve("datasets/business-history/generated").toString(),
       root.resolve("datasets/business-history/evidence/manifest.json").toString(),
       output.resolve(pass+".ndjson").toString(),"--opening").inheritIO();
   process.environment().put("QUINCAI_HISTORY_TOKEN",maker);
   process.environment().put("QUINCAI_HISTORY_APPROVER_TOKEN",approver);
   assertEquals(0,process.start().waitFor(),pass+" opening importer process");
  }
  assertEquals(-1,Files.mismatch(output.resolve("initial.ndjson"),output.resolve("replay.ndjson")));
  try(var db=DriverManager.getConnection(url,user,"");var statement=db.createStatement()){
   try(var rows=statement.executeQuery("select account_code, sum(debit_xaf-credit_xaf)::text from journal_line group by account_code")){
    var balance=new HashMap<String,String>();
    while(rows.next())balance.put(rows.getString(1),rows.getString(2));
    assertEquals(Map.of("BANK","1900000000","CASH","100000000","EQUITY","-2000000000"),balance);
   }
   try(var rows=statement.executeQuery("select count(*) from journal_entry")){
    assertTrue(rows.next());
    assertEquals(1,rows.getInt(1));
   }
  }
 }

 private static Path verifyHistoricalCatalog(int port,String token,String url,String user)throws Exception{
  Path output=Files.createTempDirectory(Path.of("target"),"history-acceptance-").toAbsolutePath();
  Path root=Path.of("../..").toAbsolutePath().normalize();
  for(String pass:List.of("initial","replay")){
   var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
       "--class-path",System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")),
       root.resolve("backend/tools/history-import/src/main/java/com/quincaisoft/tools/CatalogHistoryImport.java").toString(),
       "http://127.0.0.1:"+port,root.resolve("datasets/business-history/generated").toString(),
       root.resolve("datasets/business-history/evidence/manifest.json").toString(),
       output.resolve(pass+".ndjson").toString()).inheritIO();
   process.environment().put("QUINCAI_HISTORY_TOKEN",token);
   assertEquals(0,process.start().waitFor(),pass+" importer process");
   try(var rows=Files.lines(output.resolve(pass+".ndjson"))){assertEquals(17117,rows.count());}
  }
  assertEquals(-1,Files.mismatch(output.resolve("initial.ndjson"),output.resolve("replay.ndjson")));
  try(var db=DriverManager.getConnection(url,user,"")){
   for(var expected:Map.of("catalog_manufacturer",11,"catalog_product_family",1001,"catalog_variant_identity",3001,
       "catalog_product",3001,"catalog_supplier_offer",9001,"supplier",81,"customer",1000).entrySet()){
    try(var statement=db.createStatement();var rows=statement.executeQuery("select count(*) from "+expected.getKey())){
     assertTrue(rows.next());
     assertEquals(expected.getValue().intValue(),rows.getInt(1),expected.getKey());
    }
   }
  }
  return output.resolve("initial.ndjson");
 }

 private static void verifyHistoricalReceipt(int port,String maker,String approver,Path catalog,String url,String user)throws Exception{
  Path output=Files.createTempDirectory(Path.of("target"),"receipt-acceptance-").toAbsolutePath();
  Path root=Path.of("../..").toAbsolutePath().normalize();
  for(String pass:List.of("initial","replay")){
   var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
       "--class-path",System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")),
       root.resolve("backend/tools/history-import/src/main/java/com/quincaisoft/tools/CatalogHistoryImport.java").toString(),
       "http://127.0.0.1:"+port,root.resolve("datasets/business-history/generated").toString(),
       root.resolve("datasets/business-history/evidence/manifest.json").toString(),
       output.resolve(pass+".ndjson").toString(),"--receipt",catalog.toString()).inheritIO();
   process.environment().put("QUINCAI_HISTORY_TOKEN",maker);
   process.environment().put("QUINCAI_HISTORY_APPROVER_TOKEN",approver);
   assertEquals(0,process.start().waitFor(),pass+" receipt importer process");
  }
  var json=tools.jackson.databind.json.JsonMapper.builder().build();
  assertEquals(json.readTree(Files.readString(output.resolve("initial.ndjson"))),
      json.readTree(Files.readString(output.resolve("replay.ndjson"))));
  try(var db=DriverManager.getConnection(url,user,"");var statement=db.createStatement()){
   try(var rows=statement.executeQuery("select quantity_milli,value_xaf from stock_movement")){
    assertTrue(rows.next());
    assertEquals(20000,rows.getLong(1));
    assertEquals(52000,rows.getLong(2));
    assertFalse(rows.next());
   }
   try(var rows=statement.executeQuery("select status,total_xaf from purchase_order")){
    assertTrue(rows.next());
    assertEquals("RECEIVED",rows.getString(1));
    assertEquals(52000,rows.getLong(2));
    assertFalse(rows.next());
   }
   try(var query=db.prepareStatement("select l.account_code,l.debit_xaf-l.credit_xaf from journal_line l join journal_entry j on j.tenant_id=l.tenant_id and j.company_id=l.company_id and j.id=l.entry_id where j.operation_id=?")){
    var mapping=json.readTree(Files.readString(output.resolve("initial.ndjson")));
    query.setObject(1,UUID.fromString(mapping.required("postingOperationId").asText()));
    try(var rows=query.executeQuery()){
     var amounts=new HashMap<String,Long>();
     while(rows.next())assertNull(amounts.put(rows.getString(1),rows.getLong(2)));
     assertEquals(Map.of("MERCHANT_INVENTORY",52000L,"GOODS_RECEIVED_NOT_INVOICED",-52000L),amounts);
    }
   }
  }
 }

 private static tools.jackson.databind.JsonNode catalogWrite(HttpClient client,int port,String path,String reader,String writer,Map<String,?> fields)throws Exception{
  var json=tools.jackson.databind.json.JsonMapper.builder().build();
  var operation=UUID.randomUUID();
  var input=new HashMap<String,Object>(fields);
  input.put("operationId",operation);
  input.put("reason","Synthetic historical catalog acceptance");
  String body=json.writeValueAsString(input);
  var uri=URI.create("http://127.0.0.1:"+port+"/api/v1/catalog/"+path);
  assertEquals(401,catalogPost(client,uri,null,operation,body).statusCode(),path);
  assertEquals(403,catalogPost(client,uri,reader,operation,body).statusCode(),path);
  assertEquals(422,catalogPost(client,uri,writer,UUID.randomUUID(),body).statusCode(),path);
  var result=catalogPost(client,uri,writer,operation,body);
  assertEquals(200,result.statusCode(),result.body());
  var replay=catalogPost(client,uri,writer,operation,body);
  assertEquals(200,replay.statusCode(),replay.body());
  assertEquals(json.readTree(result.body()),json.readTree(replay.body()));
  assertEquals(403,catalogPost(client,uri,reader,operation,body).statusCode(),path);
  return json.readTree(result.body());
 }

 private static HttpResponse<String> catalogPost(HttpClient client,URI uri,String token,UUID operation,String body)throws Exception{
  var request=HttpRequest.newBuilder(uri).header("Content-Type","application/json")
      .header("Idempotency-Key",operation.toString()).header("X-Request-ID",UUID.randomUUID().toString())
      .POST(HttpRequest.BodyPublishers.ofString(body));
  if(token!=null)request.header("Authorization","Bearer "+token);
  return client.send(request.build(),HttpResponse.BodyHandlers.ofString());
 }
}
