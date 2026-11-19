package com.quincaisoft.security;

import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Ordinary authenticated workflows; no imported history or direct ledger fixture writes. */
final class FulfillmentHttpWorkflow {
 private static final UUID WAREHOUSE=UUID.fromString("b10d7cb7-afc8-4f6f-87c7-845d1909416a");
 private static final UUID CUSTOMER=UUID.fromString("a38d926e-5154-4149-a931-e48e4ce6d57a");
 private static final String DATE="2026-11-18";
 private static final JsonMapper JSON=JsonMapper.builder().build();
 static void bootstrap(Connection db,UUID tenant,UUID company,UUID branch,UUID checker)throws Exception{
  FullApplicationPostgresTest.sql(db,"INSERT INTO warehouse(tenant_id,company_id,branch_id,id,name) VALUES ('"+tenant+"','"+company+"','"+branch+"','"+WAREHOUSE+"','Synthetic workflow warehouse');"
      +"INSERT INTO customer(tenant_id,company_id,id,name) VALUES ('"+tenant+"','"+company+"','"+CUSTOMER+"','Synthetic workflow customer');"
      +"INSERT INTO tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code) VALUES ('"+tenant+"','"+company+"','"+branch+"','"+checker+"','owner');"
      +"INSERT INTO financial_period(tenant_id,company_id,id,start_date,end_date,status) VALUES ('"+tenant+"','"+company+"',gen_random_uuid(),'2026-01-01','2026-12-31','Open')");
 }
 static void verify(HttpClient client,int port,String maker,String checker,UUID product,UUID supplier,String url,String user)throws Exception{
  var purchase=command(client,port,maker,"purchases",Map.of("supplierId",supplier,"warehouseId",WAREHOUSE,
      "businessDate",DATE,"lines",List.of(Map.of("productId",product,"quantityMilli","10000","unitPriceXaf","500"))),200);
  String purchasePath="purchases/"+purchase.required("id").asText();
  var submitted=command(client,port,maker,purchasePath+"/submit",decision(purchase),200);
  var denied=command(client,port,maker,purchasePath+"/approve",decision(submitted),409);
  assertEquals("STATE_CONFLICT",denied.required("code").asText());
  var approved=command(client,port,checker,purchasePath+"/approve",decision(submitted),200);
  command(client,port,maker,purchasePath+"/receipts",Map.of("version",approved.required("version").asText(),
      "reason","Synthetic receive","businessDate",DATE,"lines",List.of(Map.of("lineId",
          approved.required("lines").get(0).required("id").asText(),"quantityMilli","10000"))),200);
  var settings=HttpRequest.newBuilder(uri(port,"documents/settings")).header("Authorization","Bearer "+maker)
      .header("Content-Type","application/json").header("Idempotency-Key",UUID.randomUUID().toString())
      .header("X-Request-ID",UUID.randomUUID().toString()).PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(
          Map.of("issuerName","Synthetic workflow merchant","issuerAddress","Synthetic address","registrationNumber","TEST",
              "fiscalSeries","TEST","locale","en","version",0)))).build();
  var configured=client.send(settings,HttpResponse.BodyHandlers.ofString());
  assertEquals(200,configured.statusCode(),configured.body());
  var sale=command(client,port,maker,"sales",Map.of("warehouseId",WAREHOUSE,"customerId",CUSTOMER,"businessDate",DATE,
      "lines",List.of(Map.of("productId",product,"quantityMilli","2000"))),200);
  var posted=command(client,port,maker,"sales/"+sale.required("id").asText()+"/post",decision(sale),201);
  assertEquals("POSTED",posted.required("status").asText());
  var task=command(client,port,maker,"fulfillments",Map.of("saleId",posted.required("id").asText(),"customerId",CUSTOMER,
      "businessDate",DATE,"reason","Synthetic delivery","delivery",Map.of("method","DELIVERY","scheduledDate","2026-11-19",
          "address","Synthetic destination","recipientName","Synthetic recipient")),200);
  String path="fulfillments/"+task.required("id").asText();
  try(var db=DriverManager.getConnection(url,user,"")){
   String before=economics(db);
   assertEquals("8000:4000/2/2/8000:8000/1",before,"One receipt and one recognized sale");
   var partial=command(client,port,maker,path+"/dispatch-lines",dispatch(task,product,"500"),200);
   assertEquals("PARTIAL",partial.required("status").asText());
   command(client,port,maker,path+"/dispatch-lines",dispatch(partial,product,"1501"),422);
   var pick=get(client,port,maker,path+"/picklist");
   assertEquals("1500",pick.required("lines").get(0).required("quantityMilli").asText());
   var shipped=command(client,port,maker,path+"/dispatch-lines",dispatch(partial,product,"1500"),200);
   assertEquals("DISPATCHED",shipped.required("status").asText());
   assertEquals(0,get(client,port,maker,path+"/picklist").required("lines").size());
   var completion=new HashMap<String,Object>(decision(shipped));
   completion.put("proofReference","SYNTHETIC-SIGNED-PROOF");
   var completed=command(client,port,maker,path+"/complete",completion,200);
   assertEquals("COMPLETED",completed.required("status").asText());
   assertEquals("SYNTHETIC-SIGNED-PROOF",get(client,port,maker,path).required("proofReference").asText());
   assertEquals(2,get(client,port,maker,path+"/dispatches").size());
   var page=get(client,port,maker,"fulfillments/page?status=COMPLETED&q=Synthetic&limit=25");
   assertEquals(task.required("id"),page.required("items").get(0).required("fulfillment").required("id"));
   assertEquals(before,economics(db),"Delivery must not issue stock or post financial recognition again");
  }
 }
 private static Map<String,Object> decision(JsonNode record){return Map.of("version",record.required("version").asText(),"reason","Synthetic workflow acceptance");}
 private static Map<String,Object> dispatch(JsonNode record,UUID product,String quantity){
  return Map.of("version",record.required("version").asText(),"reason","Synthetic dispatch",
      "lines",List.of(Map.of("productId",product,"quantityMilli",quantity)));
 }
 private static URI uri(int port,String path){return URI.create("http://127.0.0.1:"+port+"/api/v1/"+path);}
 private static JsonNode get(HttpClient client,int port,String token,String path)throws Exception{
  var response=client.send(HttpRequest.newBuilder(uri(port,path)).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(200,response.statusCode(),response.body());return JSON.readTree(response.body());
 }
 private static JsonNode command(HttpClient client,int port,String token,String path,Map<String,?> fields,int expected)throws Exception{
  var operation=UUID.randomUUID();var body=new HashMap<String,Object>(fields);body.put("operationId",operation);
  var request=HttpRequest.newBuilder(uri(port,path)).header("Authorization","Bearer "+token).header("Content-Type","application/json")
      .header("Idempotency-Key",operation.toString()).header("X-Request-ID",UUID.randomUUID().toString())
      .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build();
  var result=client.send(request,HttpResponse.BodyHandlers.ofString());assertEquals(expected,result.statusCode(),path+": "+result.body());
  if(expected<300){
   var replay=client.send(request,HttpResponse.BodyHandlers.ofString());
   assertEquals(expected,replay.statusCode(),path+": "+replay.body());
   assertEquals(JSON.readTree(result.body()),JSON.readTree(replay.body()),"Durable replay: "+path);
  }
  return JSON.readTree(result.body());
 }
 private static String economics(Connection db)throws Exception{
  try(var query=db.createStatement();var result=query.executeQuery("select "
      +"(select sum(quantity_milli)::text||':'||sum(value_xaf)::text from stock_balance),"
      +"(select count(*) from stock_movement),(select count(*) from journal_entry),"
      +"(select sum(debit_xaf)::text||':'||sum(credit_xaf)::text from journal_line),"
      +"(select count(*) from document_snapshot)")){
   assertTrue(result.next());return result.getString(1)+"/"+result.getString(2)+"/"+result.getString(3)+"/"+result.getString(4)+"/"+result.getString(5);
  }
 }
}
