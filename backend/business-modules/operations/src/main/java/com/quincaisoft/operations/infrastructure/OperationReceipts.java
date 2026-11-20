package com.quincaisoft.operations.infrastructure;

import com.quincaisoft.shared.*;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.*;

public final class OperationReceipts {
 private final EntityManager em;private final JsonMapper json;
 public OperationReceipts(EntityManager em,JsonMapper json){this.em=em;this.json=json;}
 public static String digest(String input){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 public <T>T once(UUID op,String kind,Object request,Class<T> type,Supplier<T> action,Function<T,UUID> source){
  if(op==null)throw new IllegalArgumentException("Operation required");var s=TenantContext.current();String hash=digest(kind+"|"+s.actorId()+"|"+json.writeValueAsString(request));
  em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k",s.tenantId()+":"+op).getSingleResult();
  var prior=em.createNativeQuery("select request_hash,company_id,branch_id,response_json::text from idempotency_result where tenant_id=:t and operation_id=:o").setParameter("t",s.tenantId()).setParameter("o",op).getResultList();
  if(!prior.isEmpty()){Object[] r=(Object[])prior.getFirst();if(!hash.equals(r[0])||!s.companyId().equals(r[1])||!s.branchId().equals(r[2]))throw new IllegalStateException("Operation reused with different actor, request or scope");if(r[3]==null)throw new IllegalStateException("Committed response expired; inspect source");return json.readValue((String)r[3],type);}
  T result=action.get();UUID id=source.apply(result);if(id==null)return result;em.flush();
  em.createNativeQuery("insert into idempotency_result(tenant_id,operation_id,company_id,branch_id,request_hash,source_type,source_id,response_json)values(:t,:o,:c,:b,:h,:k,:id,cast(:j as jsonb))").setParameter("t",s.tenantId()).setParameter("o",op).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("h",hash).setParameter("k",kind).setParameter("id",id).setParameter("j",json.writeValueAsString(result)).executeUpdate();
  em.createNativeQuery("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload)values(:t,:e,:c,:b,:o,:k,'operations',:id,cast(:j as jsonb))").setParameter("t",s.tenantId()).setParameter("e",UUID.randomUUID()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("o",op).setParameter("k",kind+".v1").setParameter("id",id).setParameter("j",json.writeValueAsString(Map.of("sourceId",id,"actorId",s.actorId()))).executeUpdate();return result;
 }
}
