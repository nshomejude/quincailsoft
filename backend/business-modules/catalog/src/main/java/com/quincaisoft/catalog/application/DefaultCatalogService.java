package com.quincaisoft.catalog.application;
import com.quincaisoft.catalog.api.CatalogService;
import com.quincaisoft.catalog.infrastructure.*;
import com.quincaisoft.shared.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.*;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class DefaultCatalogService implements CatalogService {
 private final EntityManager em; private final ScopedTransactions transactions;
 public DefaultCatalogService(EntityManager em, ScopedTransactions transactions){this.em=em;this.transactions=transactions;}
 public List<Product> list(){return transactions.execute(()->{var s=TenantContext.current();return em.createQuery("select p from CatalogProduct p where p.tenantId=:t and p.companyId=:c order by p.sku",CatalogProduct.class).setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setMaxResults(200).getResultList().stream().map(CatalogProduct::view).toList();});}
 public Product get(UUID id){return transactions.execute(()->{var s=TenantContext.current();return em.createQuery("select p from CatalogProduct p where p.tenantId=:t and p.companyId=:c and p.id=:id",CatalogProduct.class).setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("id",Objects.requireNonNull(id)).getResultStream().findFirst().orElseThrow(()->new NoSuchElementException("Product not found in company scope")).view();});}
 public Product create(Save command){return save(null,command);}
 public Product update(UUID id,Save command){return save(Objects.requireNonNull(id),command);}
 private Product save(UUID id,Save command){
  if(command==null||command.operationId()==null||command.reason()==null||command.reason().isBlank()||command.reason().length()>2000)throw new IllegalArgumentException("Operation and reason required");
  String sku=required(command.sku(),80),name=required(command.name(),240),unit=required(command.baseUnit(),32);
  if(!sku.matches("[A-Za-z0-9._/-]+")||!unit.matches("[a-z][a-z0-9_-]*"))throw new IllegalArgumentException("Invalid SKU or unit");
  if(command.sellingPriceXaf()==null||!command.sellingPriceXaf().matches("0|[1-9][0-9]*"))throw new IllegalArgumentException("Price must be integral XAF string");
  long price=Long.parseLong(command.sellingPriceXaf());if(command.taxBasisPoints()<0||command.taxBasisPoints()>10000)throw new IllegalArgumentException("Invalid tax rate");
  return transactions.execute(()->{
   var scope=TenantContext.current();String hash=hash("CATALOG|"+id+"|"+command);var json=JsonMapper.builder().build();
   em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:key,0))").setParameter("key",scope.tenantId()+":"+command.operationId()).getSingleResult();
   var prior=em.createNativeQuery("select request_hash,response_json::text from idempotency_result where tenant_id=:t and company_id=:c and branch_id=:b and operation_id=:o").setParameter("t",scope.tenantId()).setParameter("c",scope.companyId()).setParameter("b",scope.branchId()).setParameter("o",command.operationId()).getResultList();
   if(!prior.isEmpty()){Object[] row=(Object[])prior.getFirst();if(!hash.equals(row[0])||row[1]==null)throw new IllegalStateException("Operation previously used or response expired");return json.readValue((String)row[1],Product.class);}
   CatalogProduct p;
   if(id==null){p=new CatalogProduct();p.tenantId=scope.tenantId();p.companyId=scope.companyId();p.id=UUID.randomUUID();p.active=true;p.baseUnit=unit;}
   else {p=em.find(CatalogProduct.class,new CatalogProduct.Key(scope.tenantId(),scope.companyId(),id),LockModeType.PESSIMISTIC_WRITE);if(p==null)throw new NoSuchElementException("Product not found");if(command.version()==null||!command.version().matches("0|[1-9][0-9]*")||p.version!=Long.parseLong(command.version()))throw new RevisionConflict("Product changed");if(!unit.equals(p.baseUnit))throw new IllegalArgumentException("Base unit cannot change after product creation");if(command.active()==null)throw new IllegalArgumentException("Active status required");p.active=command.active();}
   p.sku=sku;p.name=name;p.sellingPriceXaf=price;p.taxBasisPoints=command.taxBasisPoints();if(id==null)em.persist(p);em.flush();var result=p.view();
   em.createNativeQuery("insert into idempotency_result(tenant_id,operation_id,company_id,branch_id,request_hash,source_type,source_id,response_json) values(:t,:o,:c,:b,:h,'CATALOG',:id,cast(:body as jsonb))").setParameter("t",scope.tenantId()).setParameter("o",command.operationId()).setParameter("c",scope.companyId()).setParameter("b",scope.branchId()).setParameter("h",hash).setParameter("id",p.id).setParameter("body",json.writeValueAsString(result)).executeUpdate();
   em.createNativeQuery("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload) values(:t,:event,:c,:b,:o,'CatalogChanged.v1','product',:id,cast(:body as jsonb))").setParameter("t",scope.tenantId()).setParameter("event",UUID.randomUUID()).setParameter("c",scope.companyId()).setParameter("b",scope.branchId()).setParameter("o",command.operationId()).setParameter("id",p.id).setParameter("body",json.writeValueAsString(Map.of("product",result,"reason",command.reason(),"actorId",scope.actorId()))).executeUpdate();return result;
  });
 }
 private static String required(String value,int max){if(value==null||value.isBlank()||!value.equals(value.trim())||value.length()>max)throw new IllegalArgumentException("Required text missing or too long");return value;}
 private static String hash(String input){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
