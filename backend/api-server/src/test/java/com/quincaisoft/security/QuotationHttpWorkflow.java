package com.quincaisoft.security;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Quotation migration: authorized conversion and actor-bound durable retries. */
final class QuotationHttpWorkflow {
 private static final JsonMapper JSON=JsonMapper.builder().build();
 static void verify(HttpClient client,int port,UUID product,String owner,String otherActor,String quoteOnly)throws Exception{
  var sales=get(client,port,"sales",owner);var source=sales.get(0);
  var day=LocalDate.now(ZoneId.of("Africa/Douala"));
  var draft=new HashMap<String,Object>();draft.put("operationId",UUID.randomUUID());
  draft.put("customerId",source.required("customerId").asText());draft.put("warehouseId",source.required("warehouseId").asText());
  draft.put("businessDate",day.toString());draft.put("validUntil",day.plusDays(30).toString());
  draft.put("lines",List.of(Map.of("productId",product,"quantityMilli","2000")));
  var quotation=post(client,port,"quotations",owner,draft,200);
  assertEquals(quotation,post(client,port,"quotations",owner,draft,200));
  post(client,port,"quotations",otherActor,draft,409);
  var changed=new HashMap<String,Object>(draft);changed.put("validUntil",day.plusDays(31).toString());
  post(client,port,"quotations",owner,changed,409);
  var invalid=new HashMap<String,Object>(draft);invalid.put("operationId",UUID.randomUUID());
  var nullQuantity=new HashMap<String,Object>();nullQuantity.put("productId",product);nullQuantity.put("quantityMilli",null);
  invalid.put("lines",List.of(nullQuantity));post(client,port,"quotations",owner,invalid,422);
  String path="quotations/"+quotation.required("id").asText();
  var issue=command(quotation,"Issue synthetic quotation");
  var issued=post(client,port,path+"/issue",owner,issue,200);
  assertEquals(issued,post(client,port,path+"/issue",owner,issue,200));
  var conversion=command(issued,"Convert synthetic accepted terms");
  post(client,port,path+"/convert",quoteOnly,conversion,403);
  assertEquals("ISSUED",get(client,port,path,owner).required("status").asText());
  var converted=post(client,port,path+"/convert",owner,conversion,200);
  assertEquals(converted,post(client,port,path+"/convert",owner,conversion,200));
  post(client,port,path+"/convert",otherActor,conversion,409);
  var sale=get(client,port,"sales/"+converted.required("convertedSaleId").asText(),owner);
  assertEquals("DRAFT",sale.required("status").asText());assertEquals(issued.required("totalXaf"),sale.required("totalXaf"));
  assertEquals(issued.required("lines").get(0).required("quantityMilli"),sale.required("lines").get(0).required("quantityMilli"));
  assertEquals(issued.required("lines"),converted.required("lines"),"Conversion must preserve the quoted terms");
  post(client,port,path+"/convert",owner,command(converted,"Must not convert twice"),409);
  var expired=new HashMap<String,Object>(draft);expired.put("operationId",UUID.randomUUID());
  expired.put("businessDate",day.minusDays(2).toString());expired.put("validUntil",day.minusDays(1).toString());
  var expiredDraft=post(client,port,"quotations",owner,expired,200);
  String expiredPath="quotations/"+expiredDraft.required("id").asText();
  var expiredIssued=post(client,port,expiredPath+"/issue",owner,command(expiredDraft,"Issue expired fixture"),200);
  post(client,port,expiredPath+"/convert",owner,command(expiredIssued,"Expired conversion must fail"),409);
  assertEquals("ISSUED",get(client,port,expiredPath,owner).required("status").asText());
 }
 private static Map<String,Object> command(JsonNode quote,String reason){return Map.of("operationId",UUID.randomUUID(),"version",quote.required("version").asText(),"reason",reason);}
 private static URI uri(int port,String path){return URI.create("http://127.0.0.1:"+port+"/api/v1/"+path);}
 private static JsonNode get(HttpClient client,int port,String path,String token)throws Exception{
  var response=client.send(HttpRequest.newBuilder(uri(port,path)).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(200,response.statusCode(),response.body());return JSON.readTree(response.body());
 }
 private static JsonNode post(HttpClient client,int port,String path,String token,Map<String,?> fields,int expected)throws Exception{
  var response=client.send(HttpRequest.newBuilder(uri(port,path)).header("Authorization","Bearer "+token)
      .header("Content-Type","application/json").header("Idempotency-Key",fields.get("operationId").toString())
      .header("X-Request-ID",UUID.randomUUID().toString()).POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(fields))).build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(expected,response.statusCode(),path+": "+response.body());return JSON.readTree(response.body());
 }
}
