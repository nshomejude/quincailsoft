package com.quincaisoft.suppliers.application;

import com.quincaisoft.suppliers.api.SupplierService;
import com.quincaisoft.suppliers.infrastructure.*;
import com.quincaisoft.shared.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

@Service public class DefaultSupplierService implements SupplierService {
 private final EntityManager em;private final ScopedTransactions tx;private final OperationJournal operations;
 public DefaultSupplierService(EntityManager em,ScopedTransactions tx,JsonMapper json){this.em=em;this.tx=tx;this.operations=new OperationJournal(em,json);}
 private Supplier view(SupplierEntity r){return new Supplier(r.id,r.code,r.name,r.phone,r.email,r.active,r.version,r.contactPerson,r.address,r.taxReference,r.paymentTermsDays,r.bankReference);}
 public List<Supplier> list(){return page("",0,200).items();}
 public Page page(String query,int offset,int limit){if(offset<0||limit<1||limit>200||query!=null&&query.length()>200)throw new IllegalArgumentException("Invalid page");return tx.execute(()->{String q="%"+(query==null?"":query.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_"))+"%";String where=" from SupplierEntity s where lower(s.name) like :q escape '!' or lower(s.code) like :q escape '!'";long total=em.createQuery("select count(s)"+where,Long.class).setParameter("q",q).getSingleResult();var items=em.createQuery("select s"+where+" order by s.name,s.id",SupplierEntity.class).setParameter("q",q).setFirstResult(offset).setMaxResults(limit).getResultList().stream().map(this::view).toList();return new Page(items,total,offset,limit);});}
 public Supplier get(UUID id){return tx.execute(()->view(find(id,LockModeType.NONE)));}
 private SupplierEntity find(UUID id,LockModeType lock){var s=TenantContext.current();var r=em.find(SupplierEntity.class,new SupplierEntity.Key(s.tenantId(),s.companyId(),Objects.requireNonNull(id)),lock);if(r==null)throw new NoSuchElementException("Supplier not found");return r;}
 public void requireEligible(UUID id){tx.execute(()->{if(!find(id,LockModeType.PESSIMISTIC_READ).active)throw new IllegalStateException("Supplier inactive");return null;});}
 public Supplier create(Create q){if(q==null)throw new IllegalArgumentException("Supplier required");return save(null,new Save(q.operationId(),q.code(),q.name(),"",empty(q.phone()),empty(q.email()),"","",0,"",true,"0","Create supplier"));}
 public Supplier save(UUID id,Save q){
  if(q==null||q.code()==null||!q.code().matches("[A-Za-z0-9_-]{1,40}")||q.name()==null||q.name().isBlank()||q.name().length()>200||q.reason()==null||q.reason().isBlank()||q.reason().length()>2000||q.paymentTermsDays()<0||q.paymentTermsDays()>3650)throw new IllegalArgumentException("Valid supplier code, name, terms and reason required");
  for(String s:List.of(empty(q.contactPerson()),empty(q.address()),empty(q.taxReference()),empty(q.bankReference())))if(s.length()>2000)throw new IllegalArgumentException("Supplier metadata too long");
  if(empty(q.phone()).length()>80||empty(q.email()).length()>254||!empty(q.email()).isEmpty()&&!q.email().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalArgumentException("Invalid contact details");
  return tx.execute(()->operations.once(q.operationId(),"SUPPLIER_SAVE|"+TenantContext.current().actorId()+"|"+id+"|"+q,"SupplierSaved",Supplier.class,()->{
   SupplierEntity r;if(id==null){var s=TenantContext.current();r=new SupplierEntity();r.tenantId=s.tenantId();r.companyId=s.companyId();r.id=UUID.randomUUID();}else{r=find(id,LockModeType.PESSIMISTIC_WRITE);if(q.version()==null||!q.version().matches("0|[1-9][0-9]*")||r.version!=Long.parseLong(q.version()))throw new RevisionConflict("Supplier changed");}
   r.code=q.code();r.name=q.name();r.phone=empty(q.phone());r.email=empty(q.email());r.contactPerson=empty(q.contactPerson());r.address=empty(q.address());r.taxReference=empty(q.taxReference());r.paymentTermsDays=q.paymentTermsDays();r.bankReference=empty(q.bankReference());r.active=q.active();if(id==null)em.persist(r);em.flush();return view(r);
  },Supplier::id));
 }
 private static String empty(String value){return value==null?"":value;}
}
