package com.quincaisoft.customeraccounts.application;

import com.quincaisoft.customeraccounts.api.CustomerDirectory;
import com.quincaisoft.customeraccounts.infrastructure.CustomerEntity;
import com.quincaisoft.shared.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Service
public class DefaultCustomerDirectory implements CustomerDirectory {
 private final EntityManager em;
 private final ScopedTransactions tx;
 private final JsonMapper json=JsonMapper.builder().build();
 public DefaultCustomerDirectory(EntityManager em,ScopedTransactions tx){this.em=em;this.tx=tx;}
 public Entry create(Save command){return save(null,command);}
 public Entry update(UUID id,Save command){return save(Objects.requireNonNull(id),command);}
 public Entry get(UUID id){return tx.execute(()->view(find(id,LockModeType.NONE)));}
 public Page page(String query,int offset,int limit){
  if(offset<0||offset>100000||limit<1||limit>100)throw new IllegalArgumentException("Invalid page bounds");
  String q=text(query,240,false).toLowerCase(Locale.ROOT);
  String pattern="%"+q.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
  return tx.execute(()->{
   var rows=em.createQuery("select c from CustomerEntity c where lower(c.name) like :q escape '!' order by c.name,c.id",CustomerEntity.class)
      .setParameter("q",pattern).setFirstResult(offset).setMaxResults(limit).getResultList();
   long total=em.createQuery("select count(c) from CustomerEntity c where lower(c.name) like :q escape '!'",Long.class).setParameter("q",pattern).getSingleResult();
   return new Page(rows.stream().map(this::view).toList(),total,offset,limit);
  });
 }
 private Entry save(UUID id,Save c){
  if(c==null||c.operationId()==null)throw new IllegalArgumentException("Operation identity required");
  text(c.name(),240,true);text(c.phone(),80,false);text(c.email(),254,false);text(c.address(),2000,false);text(c.taxReference(),120,false);text(c.reason(),2000,true);
  if(c.type()==null||!Set.of("Individual","Contractor","Company").contains(c.type()))throw new IllegalArgumentException("Invalid customer type");
  if(c.email()!=null&&!c.email().isEmpty()&&!c.email().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalArgumentException("Invalid email");
  long credit=integer(c.creditLimitXaf());
  if(c.paymentTermsDays()<0||c.paymentTermsDays()>3650)throw new IllegalArgumentException("Payment terms outside bounds");
  return tx.execute(()->{
   var s=TenantContext.current();String hash=sha("CUSTOMER|"+id+"|"+s.actorId()+"|"+json.writeValueAsString(c));
   em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k",s.tenantId()+":"+c.operationId()).getSingleResult();
   var prior=em.createNativeQuery("select request_hash,response_json::text from idempotency_result where tenant_id=:t and operation_id=:o")
      .setParameter("t",s.tenantId()).setParameter("o",c.operationId()).getResultList();
   if(!prior.isEmpty()){
    Object[] p=(Object[])prior.getFirst();if(!hash.equals(p[0])||p[1]==null)throw new IllegalStateException("Operation already used with different content or actor");
    return json.readValue((String)p[1],Entry.class);
   }
   CustomerEntity e;
   if(id==null){if(c.version()!=null&&!c.version().equals("0"))throw new RevisionConflict("New customer version must be zero");e=new CustomerEntity();e.tenantId=s.tenantId();e.companyId=s.companyId();e.id=UUID.randomUUID();}
   else {e=find(id,LockModeType.PESSIMISTIC_WRITE);if(e.version!=integer(c.version()))throw new RevisionConflict("Customer changed");}
   em.createNativeQuery("select set_config('app.customer_operation',:o,true)").setParameter("o",c.operationId().toString()).getSingleResult();
   e.name=c.name();e.type=c.type();e.phone=text(c.phone(),80,false);e.email=text(c.email(),254,false);e.address=text(c.address(),2000,false);e.taxReference=text(c.taxReference(),120,false);
   e.creditLimitXaf=credit;e.paymentTermsDays=c.paymentTermsDays();e.consent=c.consent();e.active=c.active();
   if(id==null)em.persist(e);em.flush();var result=view(e);String response=json.writeValueAsString(result);
   em.createNativeQuery("insert into idempotency_result(tenant_id,company_id,branch_id,operation_id,request_hash,source_type,source_id,response_json) values(:t,:c,:b,:o,:h,'CUSTOMER',:id,cast(:r as jsonb))")
      .setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("o",c.operationId()).setParameter("h",hash).setParameter("id",e.id).setParameter("r",response).executeUpdate();
   em.createNativeQuery("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload) values(:t,:event,:c,:b,:o,'CustomerChanged.v1','customer',:id,cast(:r as jsonb))")
      .setParameter("t",s.tenantId()).setParameter("event",UUID.randomUUID()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("o",c.operationId()).setParameter("id",e.id)
      .setParameter("r",json.writeValueAsString(Map.of("customerId",e.id,"version",e.version,"actorId",s.actorId(),"reason",c.reason()))).executeUpdate();
   return result;
  });
 }
 private CustomerEntity find(UUID id,LockModeType lock){var s=TenantContext.current();var e=em.find(CustomerEntity.class,new CustomerEntity.Key(s.tenantId(),s.companyId(),Objects.requireNonNull(id)),lock);if(e==null)throw new NoSuchElementException("Customer not found");return e;}
 private Entry view(CustomerEntity e){return new Entry(e.id,e.name,e.type,e.phone,e.email,e.address,e.taxReference,Long.toString(e.creditLimitXaf),e.paymentTermsDays,e.consent,e.active,Long.toString(e.version));}
 private static String text(String v,int max,boolean required){if(v==null)v="";if(v.length()>max||required&&(v.isBlank()||!v.equals(v.trim()))||v.indexOf('\0')>=0)throw new IllegalArgumentException("Invalid text field");return v;}
 private static long integer(String v){if(v==null||!v.matches("0|[1-9][0-9]*"))throw new IllegalArgumentException("Unsigned integral string required");return Long.parseLong(v);}
 private static String sha(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
