package com.quincaisoft.quotations.application;
import com.quincaisoft.quotations.api.QuotationService;import com.quincaisoft.quotations.infrastructure.*;import com.quincaisoft.sales.api.SalesService;import com.quincaisoft.catalog.api.CatalogService;import com.quincaisoft.customeraccounts.api.CustomerService;import com.quincaisoft.inventory.api.StockService;import com.quincaisoft.documents.api.DocumentService;import com.quincaisoft.shared.*;
import jakarta.persistence.*;import org.springframework.stereotype.Service;import tools.jackson.databind.json.JsonMapper;import java.util.*;import java.time.*;import java.math.BigInteger;import java.security.*;import java.nio.charset.StandardCharsets;import java.util.function.Supplier;
@Service public class DefaultQuotationService implements QuotationService {
 private final EntityManager em;private final ScopedTransactions tx;private final SalesService sales;private final CatalogService catalog;private final CustomerService customers;private final StockService stock;private final DocumentService documents;private final JsonMapper json;
 public DefaultQuotationService(EntityManager em,ScopedTransactions tx,SalesService sales,CatalogService catalog,CustomerService customers,StockService stock,DocumentService documents,JsonMapper json){this.em=em;this.tx=tx;this.sales=sales;this.catalog=catalog;this.customers=customers;this.stock=stock;this.documents=documents;this.json=json;}
 private QuotationEntity row(UUID id,boolean lock){var s=TenantContext.current();var r=em.find(QuotationEntity.class,new QuotationEntity.Key(s.tenantId(),s.companyId(),id),lock?LockModeType.PESSIMISTIC_WRITE:LockModeType.NONE);if(r==null||!r.branchId.equals(s.branchId()))throw new NoSuchElementException("Quotation not found");return r;}
 private List<QuotationLineEntity> lines(UUID id){return em.createQuery("select l from QuotationLineEntity l where l.quotationId=:id order by l.id",QuotationLineEntity.class).setParameter("id",id).getResultList();}
 private Quote view(QuotationEntity r){return new Quote(r.id,r.status,r.customerId,r.warehouseId,r.businessDate,r.validUntil,Long.toString(r.totalXaf),r.version,r.convertedSaleId,lines(r.id).stream().map(l->new SalesService.Line(l.id,l.productId,Long.toString(l.quantityMilli),Long.toString(l.unitPriceXaf),Long.toString(l.lineTotalXaf))).toList());}
 public Quote get(UUID id){permission("quotations:read");return tx.execute(()->view(row(id,false)));}
 public List<Quote> list(){permission("quotations:read");return tx.execute(()->em.createQuery("select q from QuotationEntity q order by q.createdAt desc,q.id",QuotationEntity.class).setMaxResults(100).getResultList().stream().map(this::view).toList());}
 public Quote draft(Draft d){permission("quotations:write");if(d==null||d.operationId()==null||d.customerId()==null||d.warehouseId()==null||d.businessDate()==null||d.validUntil()==null||d.validUntil().isBefore(d.businessDate())||d.lines()==null||d.lines().isEmpty()||d.lines().size()>500)throw new IllegalArgumentException("Quotation fields and 1–500 lines required");return tx.execute(()->once(d.operationId(),"DRAFT|"+d,()->{customers.requireEligible(d.customerId());stock.requireWarehouse(d.warehouseId());var s=TenantContext.current();var r=new QuotationEntity();r.id=UUID.randomUUID();r.tenantId=s.tenantId();r.companyId=s.companyId();r.branchId=s.branchId();r.actorId=s.actorId();r.operationId=d.operationId();r.customerId=d.customerId();r.warehouseId=d.warehouseId();r.businessDate=d.businessDate();r.validUntil=d.validUntil();r.status="DRAFT";r.currency="XAF";r.createdAt=Instant.now();em.persist(r);em.flush();var seen=new HashSet<UUID>();for(var input:d.lines()){if(input==null||input.productId()==null||input.quantityMilli()==null||!seen.add(input.productId())||!input.quantityMilli().matches("[1-9][0-9]*"))throw new IllegalArgumentException("Distinct products and positive milli quantities required");var p=catalog.get(input.productId());if(!p.active()||p.taxBasisPoints()!=0)throw new IllegalArgumentException("Only active zero-tax quotation products supported");var l=new QuotationLineEntity();l.tenantId=r.tenantId;l.companyId=r.companyId;l.quotationId=r.id;l.id=UUID.randomUUID();l.productId=p.id();l.quantityMilli=Long.parseLong(input.quantityMilli());l.unitPriceXaf=p.sellingPriceXaf();var n=BigInteger.valueOf(l.quantityMilli).multiply(BigInteger.valueOf(l.unitPriceXaf));var qr=n.divideAndRemainder(BigInteger.valueOf(1000));if(qr[1].signum()!=0)throw new IllegalArgumentException("Fractional FCFA requires configured rounding");l.lineTotalXaf=qr[0].longValueExact();r.totalXaf=Math.addExact(r.totalXaf,l.lineTotalXaf);em.persist(l);}em.flush();return r;}));}
 private void validate(QuotationEntity r,Command c,String status){if(c.operationId()==null||c.reason()==null||c.reason().isBlank()||c.reason().length()>2000)throw new IllegalArgumentException("Operation and reason required");if(r.version!=c.version())throw new RevisionConflict("Quotation changed");if(!r.status.equals(status))throw new IllegalStateException("Quotation is not "+status);}
 public Quote issue(UUID id,Command c){permission("quotations:issue");return tx.execute(()->once(c.operationId(),"ISSUE|"+id+"|"+c,()->{var r=row(id,true);validate(r,c,"DRAFT");documents.issue(new DocumentService.Issue(r.id,c.operationId(),"QUOTATION",r.businessDate,r.customerId,Long.toString(r.totalXaf),lines(id).stream().map(l->{var p=catalog.get(l.productId);return new DocumentService.Item(p.id(),p.sku(),p.name(),p.baseUnit(),Long.toString(l.quantityMilli),Long.toString(l.unitPriceXaf),Long.toString(l.lineTotalXaf));}).toList()));r.status="ISSUED";r.version++;em.flush();event(r,c.operationId(),"QuotationIssued.v1");return r;}));}
 public Quote convert(UUID id,Command c){permission("quotations:write");permission("sales:write");return tx.execute(()->once(c.operationId(),"CONVERT|"+id+"|"+c,()->{var r=row(id,true);validate(r,c,"ACCEPTED");var date=LocalDate.now(ZoneId.of("Africa/Douala"));if(date.isAfter(r.validUntil))throw new IllegalStateException("Quotation expired; issue a revision");var items=lines(id);for(var l:items){var p=catalog.get(l.productId);if(p.sellingPriceXaf()!=l.unitPriceXaf||p.taxBasisPoints()!=0)throw new IllegalStateException("Catalog price changed; issue a revised quotation before conversion");}var op=UUID.nameUUIDFromBytes((c.operationId()+":sale").getBytes(StandardCharsets.UTF_8));var sale=sales.draft(new SalesService.Draft(op,r.warehouseId,r.customerId,date,items.stream().map(l->new SalesService.RequestedLine(l.productId,Long.toString(l.quantityMilli))).toList()));r.convertedSaleId=sale.id();r.status="CONVERTED";r.version++;em.flush();event(r,c.operationId(),"QuotationConverted.v1");return r;}));}
 public Quote accept(UUID id,Decision request){return decide(id,request,"ACCEPTED");}
 public Quote reject(UUID id,Decision request){return decide(id,request,"REJECTED");}
 public List<CustomerDecision> decisions(UUID id){
  permission("quotations:read");return tx.execute(()->{row(id,false);return em.createQuery("select d from QuotationDecisionEntity d where d.quotationId=:id order by d.recordedAt,d.id",QuotationDecisionEntity.class)
   .setParameter("id",id).getResultList().stream().map(d->new CustomerDecision(d.id,d.decision,d.actorId,d.reason,d.evidenceReference,d.recordedAt)).toList();});
 }
 private Quote decide(UUID id,Decision request,String decision){
  permission("quotations:write");
  if(request==null||request.evidenceReference()==null||request.evidenceReference().isBlank()||request.evidenceReference().length()>200)
   throw new IllegalArgumentException("Customer decision evidence reference required (1–200 characters)");
  return tx.execute(()->once(request.operationId(),decision+"|"+id+"|"+json.writeValueAsString(request),()->{
   var quote=row(id,true);validate(quote,new Command(request.operationId(),request.version(),request.reason()),"ISSUED");
   if(decision.equals("ACCEPTED")&&LocalDate.now(ZoneId.of("Africa/Douala")).isAfter(quote.validUntil))
    throw new IllegalStateException("Quotation expired; issue a revision");
   var scope=TenantContext.current();var fact=new QuotationDecisionEntity();
   fact.tenantId=scope.tenantId();fact.companyId=scope.companyId();fact.branchId=scope.branchId();fact.id=UUID.randomUUID();
   fact.quotationId=id;fact.operationId=request.operationId();fact.expectedVersion=request.version();fact.decision=decision;
   fact.actorId=scope.actorId();fact.reason=request.reason().strip();fact.evidenceReference=request.evidenceReference().strip();
   fact.recordedAt=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);em.persist(fact);em.flush();
   quote.status=decision;quote.version++;em.flush();event(quote,request.operationId(),decision.equals("ACCEPTED")?"QuotationAccepted.v1":"QuotationRejected.v1");
   return quote;
  }));
 }
 private void event(QuotationEntity r,UUID op,String type){em.createNativeQuery("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload) values(:t,:id,:c,:b,:o,:type,'quotation',:r,cast(:p as jsonb))").setParameter("t",r.tenantId).setParameter("id",UUID.randomUUID()).setParameter("c",r.companyId).setParameter("b",r.branchId).setParameter("o",op).setParameter("type",type).setParameter("r",r.id).setParameter("p",json.writeValueAsString(view(r))).executeUpdate();}
 private void permission(String capability) {
  var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
  if(auth==null||!auth.isAuthenticated()||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals(capability)||a.getAuthority().equals("SCOPE_"+capability)))
   throw new org.springframework.security.access.AccessDeniedException("Required permission: "+capability);
 }
 private Quote once(UUID op,String material,Supplier<QuotationEntity> work) {
  if(op==null)throw new IllegalArgumentException("Operation required");
  var scope=TenantContext.current();String hash;
  try{hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))")
      .setParameter("k",scope.tenantId()+":"+op).getSingleResult();
  var prior=em.createNativeQuery("select request_hash,source_type,response_json::text,company_id,branch_id,actor_id,status from idempotency_result where tenant_id=:t and operation_id=:o")
      .setParameter("t",scope.tenantId()).setParameter("o",op).getResultList();
  if(!prior.isEmpty()) {
   Object[] old=(Object[])prior.getFirst();
   if(!hash.equals(old[0])||!"QUOTATION".equals(old[1])||!scope.companyId().equals(old[3])||!scope.branchId().equals(old[4])||!scope.actorId().equals(old[5])||!"ACCEPTED".equals(old[6]))
    throw new IllegalStateException("Operation belongs to another request, scope, actor, source or status");
   if(old[2]==null)throw new IllegalStateException("Committed operation response expired");
   return json.readValue((String)old[2],Quote.class);
  }
  var row=work.get();var result=view(row);
  em.createNativeQuery("insert into idempotency_result(tenant_id,operation_id,company_id,branch_id,request_hash,source_type,source_id,response_json) values(:t,:o,:c,:b,:h,'QUOTATION',:s,cast(:r as jsonb))")
      .setParameter("t",scope.tenantId()).setParameter("o",op).setParameter("c",scope.companyId()).setParameter("b",scope.branchId())
      .setParameter("h",hash).setParameter("s",row.id).setParameter("r",json.writeValueAsString(result)).executeUpdate();
  return result;
 }
}
