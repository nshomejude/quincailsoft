package com.quincaisoft.inventory.application;
import com.quincaisoft.shared.*;import jakarta.persistence.*;import tools.jackson.databind.json.JsonMapper;
import java.util.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.function.*;
/** Transaction-local operation claim/replay. This helper never owns an aggregate or starts a transaction. */
final class InventoryCommands {
 private final EntityManager em;private final JsonMapper json=JsonMapper.builder().build();
 InventoryCommands(EntityManager em){this.em=em;}
 <T>T once(UUID operation,String material,String type,Class<T> response,Supplier<T> action,Function<T,UUID> identity){
  if(operation==null)throw new IllegalArgumentException("Operation required");var s=TenantContext.current();String hash=hash(material);
  em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k",s.tenantId()+":"+operation).getSingleResult();
  var previous=em.createNativeQuery("select request_hash,response_json::text,company_id,branch_id,actor_id from idempotency_result where tenant_id=:t and operation_id=:o").setParameter("t",s.tenantId()).setParameter("o",operation).getResultList();
  if(!previous.isEmpty()){Object[] r=(Object[])previous.getFirst();if(!hash.equals(r[0])||!s.companyId().equals(r[2])||!s.branchId().equals(r[3])||!s.actorId().equals(r[4]))throw new IllegalStateException("Operation belongs to another request or actor");if(r[1]==null)throw new IllegalStateException("Committed result expired; reconcile original operation");return json.readValue((String)r[1],response);}
  T result=action.get();em.flush();String body=json.writeValueAsString(result);UUID id=identity.apply(result);
  em.createNativeQuery("insert into idempotency_result(tenant_id,operation_id,company_id,branch_id,request_hash,source_type,source_id,response_json)values(:t,:o,:c,:b,:h,:kind,:id,cast(:body as jsonb))").setParameter("t",s.tenantId()).setParameter("o",operation).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("h",hash).setParameter("kind",type).setParameter("id",id).setParameter("body",body).executeUpdate();
  em.createNativeQuery("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload)values(:t,:event,:c,:b,:o,:eventType,:kind,:id,cast(:body as jsonb))").setParameter("t",s.tenantId()).setParameter("event",UUID.randomUUID()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("o",operation).setParameter("eventType",type+"Changed.v1").setParameter("kind",type).setParameter("id",id).setParameter("body",json.writeValueAsString(Map.of("result",result,"actorId",s.actorId(),"command",material))).executeUpdate();return result;
 }
 static long quantity(String s,boolean zero){if(s==null||!s.matches(zero?"0|[1-9][0-9]*":"[1-9][0-9]*"))throw new IllegalArgumentException("Canonical integral quantity/version required");return Long.parseLong(s);}
 static void text(String value,int max){if(value==null||value.isBlank()||value.length()>max)throw new IllegalArgumentException("Bounded reason/reference required");}
 static UUID derived(UUID op,String label){return UUID.nameUUIDFromBytes((op+":"+label).getBytes(StandardCharsets.UTF_8));}
 private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
