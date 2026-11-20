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
 /**
  * Bounded directory search. Tenant/company restrictions are explicit in both
  * queries and enforced again by PostgreSQL FORCE RLS through ScopedTransactions.
  * Escape LIKE wildcards so a literal SKU containing '_' or '%' cannot turn
  * the requested search into an unbounded wildcard query.
  */
 public Page page(String query,int offset,int limit) {
  if(query==null)query="";
  if(query.length()>120||query.chars().anyMatch(Character::isISOControl)
      ||offset<0||offset>1000000||limit<1||limit>100)
   throw new IllegalArgumentException("Query length, offset or page limit outside bounds");
  String term=query.strip().toLowerCase(Locale.ROOT)
      .replace("!","!!").replace("%","!%").replace("_","!_");
  String pattern="%"+term+"%";
  return transactions.execute(()->{
   var scope=TenantContext.current();
   String predicate="p.tenantId=:tenant and p.companyId=:company and ("
       +"lower(p.sku) like :term escape '!' or lower(p.name) like :term escape '!'"
       +" or lower(coalesce(p.barcode,'')) like :term escape '!'"
       +" or lower(coalesce(p.brand,'')) like :term escape '!')";
   var count=em.createQuery("select count(p) from CatalogProduct p where "+predicate,Long.class)
       .setParameter("tenant",scope.tenantId())
       .setParameter("company",scope.companyId()).setParameter("term",pattern)
       .getSingleResult();
   var products=em.createQuery("select p from CatalogProduct p where "+predicate
       +" order by p.sku,p.id",CatalogProduct.class)
       .setParameter("tenant",scope.tenantId())
       .setParameter("company",scope.companyId()).setParameter("term",pattern)
       .setFirstResult(offset).setMaxResults(limit).getResultList().stream()
       .map(CatalogProduct::view).toList();
   return new Page(products,count,offset,limit);
  });
 }
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
   var scope=TenantContext.current();String hash=hash("CATALOG|"+id+"|"+scope.actorId()+"|"+command);var json=JsonMapper.builder().build();
   em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:key,0))").setParameter("key",scope.tenantId()+":"+command.operationId()).getSingleResult();
   var prior=em.createNativeQuery("select request_hash,response_json::text,company_id,branch_id,actor_id,source_type,status from idempotency_result where tenant_id=:t and operation_id=:o").setParameter("t",scope.tenantId()).setParameter("o",command.operationId()).getResultList();
   if(!prior.isEmpty()){Object[] row=(Object[])prior.getFirst();if(!hash.equals(row[0])||row[1]==null||!scope.companyId().equals(row[2])||!scope.branchId().equals(row[3])||!scope.actorId().equals(row[4])||!"CATALOG".equals(row[5])||!"ACCEPTED".equals(row[6]))throw new IllegalStateException("Operation previously used or response expired");return json.readValue((String)row[1],Product.class);}
   CatalogProduct p;
   if(id==null){p=new CatalogProduct();p.tenantId=scope.tenantId();p.companyId=scope.companyId();p.id=UUID.randomUUID();p.active=command.active()==null||command.active();p.baseUnit=unit;}
   else {p=em.find(CatalogProduct.class,new CatalogProduct.Key(scope.tenantId(),scope.companyId(),id),LockModeType.PESSIMISTIC_WRITE);if(p==null)throw new NoSuchElementException("Product not found");if(command.version()==null||!command.version().matches("0|[1-9][0-9]*")||p.version!=Long.parseLong(command.version()))throw new RevisionConflict("Product changed");if(!unit.equals(p.baseUnit))throw new IllegalArgumentException("Base unit cannot change after product creation");if(command.active()==null)throw new IllegalArgumentException("Active status required");p.active=command.active();}
   if(command.categoryId()!=null){var category=em.find(CatalogCategory.class,new CatalogCategory.Key(scope.tenantId(),scope.companyId(),command.categoryId()),LockModeType.PESSIMISTIC_READ);if(category==null||!category.active)throw new IllegalArgumentException("Active company category required");p.categoryId=category.id;}
   if(command.brand()!=null)p.brand=optional(command.brand(),120);if(command.barcode()!=null)p.barcode=optional(command.barcode(),120);if(command.description()!=null)p.description=optional(command.description(),4000);
   if(command.packSizeMilli()!=null)p.packSizeMilli=integer(command.packSizeMilli(),true);if(command.reorderLevelMilli()!=null)p.reorderLevelMilli=integer(command.reorderLevelMilli(),false);
   if(id!=null&&((command.trackStock()!=null&&p.trackStock!=command.trackStock())||(command.trackSerial()!=null&&p.trackSerial!=command.trackSerial())||(command.trackExpiry()!=null&&p.trackExpiry!=command.trackExpiry())))throw new IllegalArgumentException("Stock tracking identity is immutable; use reviewed product migration");
   if(command.trackStock()!=null)p.trackStock=command.trackStock();if(command.trackSerial()!=null)p.trackSerial=command.trackSerial();if(command.trackExpiry()!=null)p.trackExpiry=command.trackExpiry();
   if(!p.trackStock&&(p.trackSerial||p.trackExpiry))throw new IllegalArgumentException("Serial and expiry tracking require stock tracking");
   p.sku=sku;p.name=name;p.sellingPriceXaf=price;p.taxBasisPoints=command.taxBasisPoints();if(id==null)em.persist(p);em.flush();var result=p.view();
   em.createNativeQuery("insert into idempotency_result(tenant_id,operation_id,company_id,branch_id,request_hash,source_type,source_id,response_json) values(:t,:o,:c,:b,:h,'CATALOG',:id,cast(:body as jsonb))").setParameter("t",scope.tenantId()).setParameter("o",command.operationId()).setParameter("c",scope.companyId()).setParameter("b",scope.branchId()).setParameter("h",hash).setParameter("id",p.id).setParameter("body",json.writeValueAsString(result)).executeUpdate();
   em.createNativeQuery("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload) values(:t,:event,:c,:b,:o,'CatalogChanged.v1','product',:id,cast(:body as jsonb))").setParameter("t",scope.tenantId()).setParameter("event",UUID.randomUUID()).setParameter("c",scope.companyId()).setParameter("b",scope.branchId()).setParameter("o",command.operationId()).setParameter("id",p.id).setParameter("body",json.writeValueAsString(Map.of("product",result,"reason",command.reason(),"actorId",scope.actorId()))).executeUpdate();return result;
  });
 }
 private static String required(String value,int max){if(value==null||value.isBlank()||!value.equals(value.trim())||value.length()>max)throw new IllegalArgumentException("Required text missing or too long");return value;}
 private static String optional(String value,int max){if(value.length()>max||value.indexOf('\0')>=0)throw new IllegalArgumentException("Text field too long or invalid");return value;}
 private static long integer(String value,boolean positive){if(!value.matches(positive?"[1-9][0-9]*":"0|[1-9][0-9]*"))throw new IllegalArgumentException("Integral quantity required");return Long.parseLong(value);}
 private static String hash(String input){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
