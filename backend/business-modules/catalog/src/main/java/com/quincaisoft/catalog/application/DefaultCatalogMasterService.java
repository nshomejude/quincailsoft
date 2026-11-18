package com.quincaisoft.catalog.application;
import com.quincaisoft.catalog.api.CatalogMasterService;
import com.quincaisoft.catalog.infrastructure.*;
import com.quincaisoft.shared.*;
import jakarta.persistence.*;import org.springframework.stereotype.Service;import tools.jackson.databind.json.JsonMapper;
import java.util.*;import java.nio.charset.StandardCharsets;import java.security.*;

@Service
public class DefaultCatalogMasterService implements CatalogMasterService {
 private final EntityManager em;private final ScopedTransactions tx;private final JsonMapper json=JsonMapper.builder().build();
 public DefaultCatalogMasterService(EntityManager em,ScopedTransactions tx){this.em=em;this.tx=tx;}
 public List<Category> categories(){return tx.execute(()->em.createQuery("select c from CatalogCategory c order by c.code",CatalogCategory.class).setMaxResults(1000).getResultList().stream().map(this::view).toList());}
 public List<Unit> units(){return tx.execute(()->em.createQuery("select u from CatalogUnit u order by u.code",CatalogUnit.class).setMaxResults(1000).getResultList().stream().map(this::view).toList());}
 public Category category(UUID id){return tx.execute(()->view(findCategory(id,LockModeType.NONE)));}
 public Unit unit(UUID id){return tx.execute(()->view(findUnit(id,LockModeType.NONE)));}
 public Category saveCategory(UUID id,CategorySave c){
  if(c==null)throw new IllegalArgumentException("Category required");validate(c.operationId(),c.code(),c.name(),c.reason());text(c.brand(),120,false);text(c.description(),4000,false);
  return tx.execute(()->{
   var s=TenantContext.current();String hash=digest("CATEGORY|"+id+"|"+s.actorId()+"|"+json.writeValueAsString(c));var replay=replay(c.operationId(),hash,Category.class);if(replay!=null)return replay;
   // Serialize parent changes so two concurrent moves cannot introduce a cycle.
   em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k","category-tree:"+s.tenantId()+":"+s.companyId()).getSingleResult();
   CatalogCategory e;
   if(id==null){e=new CatalogCategory();e.tenantId=s.tenantId();e.companyId=s.companyId();e.id=UUID.randomUUID();newVersion(c.version());}
   else {e=findCategory(id,LockModeType.PESSIMISTIC_WRITE);if(e.version!=number(c.version(),false))throw new RevisionConflict("Category changed");}
   UUID cursor=c.parentId();Set<UUID> visited=new HashSet<>();
   while(cursor!=null){if(cursor.equals(e.id)||!visited.add(cursor)||visited.size()>100)throw new IllegalArgumentException("Category parent cycle or excessive depth");var parent=findCategory(cursor,LockModeType.PESSIMISTIC_READ);if(!parent.active)throw new IllegalArgumentException("Parent category inactive");cursor=parent.parentId;}
   e.code=c.code();e.name=c.name();e.parentId=c.parentId();e.brand=text(c.brand(),120,false);e.description=text(c.description(),4000,false);e.active=c.active();
   if(id==null)em.persist(e);em.flush();var result=view(e);receipt(c.operationId(),e.id,"CATEGORY",hash,result,c.reason());return result;
  });
 }
 public Unit saveUnit(UUID id,UnitSave c){
  if(c==null)throw new IllegalArgumentException("Unit required");validate(c.operationId(),c.code(),c.name(),c.reason());text(c.baseUnit(),80,true);long numerator=number(c.factorNumerator(),true),denominator=number(c.factorDenominator(),true);
  if(c.precision()<0||c.precision()>6)throw new IllegalArgumentException("Unit precision outside bounds");
  return tx.execute(()->{
   var s=TenantContext.current();String hash=digest("UNIT|"+id+"|"+s.actorId()+"|"+json.writeValueAsString(c));var replay=replay(c.operationId(),hash,Unit.class);if(replay!=null)return replay;
   if(c.baseUnit().equals(c.code())){if(numerator!=1||denominator!=1)throw new IllegalArgumentException("Base unit factor must be one");}
   else {var bases=em.createQuery("select u from CatalogUnit u where u.code=:code and u.active=true",CatalogUnit.class).setParameter("code",c.baseUnit()).setLockMode(LockModeType.PESSIMISTIC_READ).getResultList();if(bases.isEmpty()||!bases.getFirst().baseUnit.equals(bases.getFirst().code)||bases.getFirst().factorNumerator!=1||bases.getFirst().factorDenominator!=1)throw new IllegalArgumentException("Active base unit required");}
   CatalogUnit e;
   if(id==null){e=new CatalogUnit();e.tenantId=s.tenantId();e.companyId=s.companyId();e.id=UUID.randomUUID();e.code=c.code();e.baseUnit=c.baseUnit();e.factorNumerator=numerator;e.factorDenominator=denominator;e.precision=c.precision();newVersion(c.version());}
   else {e=findUnit(id,LockModeType.PESSIMISTIC_WRITE);if(e.version!=number(c.version(),false))throw new RevisionConflict("Unit changed");if(!e.code.equals(c.code())||!e.baseUnit.equals(c.baseUnit())||e.factorNumerator!=numerator||e.factorDenominator!=denominator||e.precision!=c.precision())throw new IllegalArgumentException("Unit identity and scale are immutable; create a versioned unit");}
   e.name=c.name();e.active=c.active();if(id==null)em.persist(e);em.flush();var result=view(e);receipt(c.operationId(),e.id,"UNIT",hash,result,c.reason());return result;
  });
 }
 private CatalogCategory findCategory(UUID id,LockModeType lock){var s=TenantContext.current();var e=em.find(CatalogCategory.class,new CatalogCategory.Key(s.tenantId(),s.companyId(),id),lock);if(e==null)throw new NoSuchElementException("Category not found");return e;}
 private CatalogUnit findUnit(UUID id,LockModeType lock){var s=TenantContext.current();var e=em.find(CatalogUnit.class,new CatalogUnit.Key(s.tenantId(),s.companyId(),id),lock);if(e==null)throw new NoSuchElementException("Unit not found");return e;}
 private Category view(CatalogCategory e){return new Category(e.id,e.code,e.name,e.parentId,e.brand,e.description,e.active,Long.toString(e.version));}
 private Unit view(CatalogUnit e){return new Unit(e.id,e.code,e.name,e.baseUnit,Long.toString(e.factorNumerator),Long.toString(e.factorDenominator),e.precision,e.active,Long.toString(e.version));}
 private <T>T replay(UUID operation,String hash,Class<T> type){
  var s=TenantContext.current();em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k",s.tenantId()+":"+operation).getSingleResult();
  var rows=em.createNativeQuery("select request_hash,response_json::text from idempotency_result where tenant_id=:t and operation_id=:o").setParameter("t",s.tenantId()).setParameter("o",operation).getResultList();
  if(rows.isEmpty())return null;Object[] r=(Object[])rows.getFirst();if(!hash.equals(r[0])||r[1]==null)throw new IllegalStateException("Operation already used");return json.readValue((String)r[1],type);
 }
 private void receipt(UUID operation,UUID id,String kind,String hash,Object result,String reason){
  var s=TenantContext.current();em.createNativeQuery("insert into idempotency_result(tenant_id,company_id,branch_id,operation_id,request_hash,source_type,source_id,response_json) values(:t,:c,:b,:o,:h,:kind,:id,cast(:r as jsonb))")
   .setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("o",operation).setParameter("h",hash).setParameter("kind",kind).setParameter("id",id).setParameter("r",json.writeValueAsString(result)).executeUpdate();
  em.createNativeQuery("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload) values(:t,:event,:c,:b,:o,:kind,'catalog_master',:id,cast(:r as jsonb))")
   .setParameter("t",s.tenantId()).setParameter("event",UUID.randomUUID()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("o",operation).setParameter("kind",kind+"Changed.v1").setParameter("id",id).setParameter("r",json.writeValueAsString(Map.of("id",id,"actorId",s.actorId(),"reason",reason))).executeUpdate();
 }
 private static void validate(UUID op,String code,String name,String reason){if(op==null)throw new IllegalArgumentException("Operation required");text(code,80,true);if(!code.matches("[A-Za-z0-9._/-]+"))throw new IllegalArgumentException("Invalid code");text(name,240,true);text(reason,2000,true);}
 private static String text(String v,int max,boolean required){if(v==null)v="";if(v.length()>max||required&&(v.isBlank()||!v.equals(v.trim()))||v.indexOf('\0')>=0)throw new IllegalArgumentException("Invalid text");return v;}
 private static long number(String v,boolean positive){if(v==null||!v.matches(positive?"[1-9][0-9]*":"0|[1-9][0-9]*"))throw new IllegalArgumentException("Integral value required");return Long.parseLong(v);}
 private static void newVersion(String v){if(v!=null&&!v.equals("0"))throw new RevisionConflict("New record version must be zero");}
 private static String digest(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
