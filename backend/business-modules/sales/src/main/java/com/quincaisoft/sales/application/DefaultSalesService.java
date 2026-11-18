package com.quincaisoft.sales.application;
import com.quincaisoft.sales.api.SalesService;
import com.quincaisoft.documents.api.DocumentService;
import com.quincaisoft.shared.*;
import com.quincaisoft.sales.infrastructure.*;
import com.quincaisoft.sales.domain.SaleRules;
import com.quincaisoft.finance.api.FinanceService;
import com.quincaisoft.inventory.api.*;
import com.quincaisoft.catalog.api.CatalogService;
import tools.jackson.databind.json.JsonMapper;
import com.quincaisoft.customeraccounts.api.CustomerService;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import java.util.*;import java.time.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.function.Supplier;
@Service
public class DefaultSalesService implements SalesService {
 private final DocumentService documents;private final JsonMapper json;private final EntityManager em;private final ScopedTransactions tx;private final StockService stock;private final CatalogService catalog;private final FinanceService finance;private final CustomerService customers;
 public DefaultSalesService(EntityManager em,ScopedTransactions tx,StockService stock,CatalogService catalog,FinanceService finance,CustomerService customers,JsonMapper json,DocumentService documents){this.documents=documents;this.json=json;this.em=em;this.tx=tx;this.stock=stock;this.catalog=catalog;this.finance=finance;this.customers=customers;}
 public Valuation valuation(UUID id){return tx.execute(()->{var r=find(id,true);if(!"POSTED".equals(r.status))throw new IllegalStateException("Only posted sales have return valuation");return new Valuation(r.id,r.customerId,r.warehouseId,r.businessDate,lines(r).stream().map(l->new ValuationLine(l.productId,l.quantityMilli,l.lineTotalXaf,l.costXaf)).toList());});}
 public List<Sale> list(){return tx.execute(()->{var s=TenantContext.current();return em.createQuery("select s from SaleEntity s where s.tenantId=:t and s.companyId=:c and s.branchId=:b order by s.createdAt desc,s.id",SaleEntity.class).setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setMaxResults(100).getResultList().stream().map(this::view).toList();});}
 @Override
 public PostedSalesSummary postedSalesSummary(LocalDate from, LocalDate to) {
  if (from == null || to == null || to.isBefore(from) || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 365) {
   throw new IllegalArgumentException("A date range of 1 to 366 days is required");
  }
  return tx.execute(() -> {
   var scope = TenantContext.current();
   var total = (java.math.BigDecimal) em.createNativeQuery("""
       select coalesce(sum(total_xaf), 0) from sale
        where tenant_id = :tenant and company_id = :company and branch_id = :branch
          and status = 'POSTED' and business_date between :from and :to
       """)
       .setParameter("tenant", scope.tenantId()).setParameter("company", scope.companyId())
       .setParameter("branch", scope.branchId()).setParameter("from", from).setParameter("to", to)
       .getSingleResult();
   return new PostedSalesSummary(from, to, total.toBigIntegerExact().toString());
  });
 }
 public Sale get(UUID id){return tx.execute(()->view(find(id,false)));}
 private SaleEntity find(UUID id,boolean lock){var s=TenantContext.current();var row=em.find(SaleEntity.class,new SaleEntity.Key(s.tenantId(),s.companyId(),id),lock?LockModeType.PESSIMISTIC_WRITE:LockModeType.NONE);if(row==null||!row.branchId.equals(s.branchId()))throw new NoSuchElementException("Sale not found");return row;}
 private List<SaleLineEntity> lines(SaleEntity r){return em.createQuery("select l from SaleLineEntity l where l.tenantId=:t and l.companyId=:c and l.saleId=:s order by l.id",SaleLineEntity.class).setParameter("t",r.tenantId).setParameter("c",r.companyId).setParameter("s",r.id).getResultList();}
 private Sale view(SaleEntity r){return new Sale(r.id,r.status,r.businessDate,Long.toString(r.totalXaf),r.version,r.warehouseId,r.customerId,lines(r).stream().map(l->new Line(l.id,l.productId,Long.toString(l.quantityMilli),Long.toString(l.unitPriceXaf),Long.toString(l.lineTotalXaf))).toList(),r.recognitionPolicy);}
 public Sale draft(Draft request){
  if(request==null||request.operationId()==null||request.warehouseId()==null||request.customerId()==null||request.businessDate()==null||request.lines()==null||request.lines().isEmpty()||request.lines().size()>500)throw new IllegalArgumentException("Operation, customer, warehouse, date and 1–500 lines required");
  var unique=new HashSet<UUID>();for(var l:request.lines())if(l==null||l.productId()==null||!unique.add(l.productId()))throw new IllegalArgumentException("One line per product required");
  return tx.execute(()->once(request.operationId(),"DRAFT|"+request,()->{
   var s=TenantContext.current();customers.requireEligible(request.customerId());
   stock.requireWarehouse(request.warehouseId());
   var r=new SaleEntity();r.tenantId=s.tenantId();r.companyId=s.companyId();r.branchId=s.branchId();r.id=UUID.randomUUID();r.operationId=request.operationId();r.warehouseId=request.warehouseId();r.customerId=request.customerId();r.actorId=s.actorId();r.businessDate=request.businessDate();r.status="DRAFT";r.currency="XAF";r.createdAt=Instant.now();em.persist(r);em.flush();
   for(var input:request.lines()){var p=catalog.get(input.productId());if(!p.active()||!p.trackStock()||p.trackSerial()||p.trackExpiry()||p.taxBasisPoints()!=0)throw new IllegalArgumentException("Only active ordinary stock zero-tax products supported; service, serial and expiry workflows require dedicated recognition/allocation");var l=new SaleLineEntity();l.tenantId=r.tenantId;l.companyId=r.companyId;l.saleId=r.id;l.id=UUID.randomUUID();l.productId=p.id();l.quantityMilli=SaleRules.quantity(input.quantityMilli());l.unitPriceXaf=p.sellingPriceXaf();l.lineTotalXaf=SaleRules.amount(l.quantityMilli,l.unitPriceXaf);r.totalXaf=Math.addExact(r.totalXaf,l.lineTotalXaf);em.persist(l);}em.flush();return r;
  }));
 }
 public Sale post(UUID id,Post request){
  if(request==null||request.operationId()==null||request.reason()==null||request.reason().isBlank()||request.reason().length()>2000)throw new IllegalArgumentException("Operation ID and reason required");
  return tx.execute(()->once(request.operationId(),"POST|"+id+"|"+request,()->{
   var r=find(id,true);if(r.version!=request.version())throw new RevisionMismatch();if(!"DRAFT".equals(r.status))throw new IllegalStateException("Only a draft sale may post");customers.requireEligible(r.customerId);
   String recognition=request.recognitionPolicy()==null?"HANDOVER":request.recognitionPolicy();if(!Set.of("HANDOVER","SALE_WITH_CUSTOMER_CUSTODY").contains(recognition))throw new IllegalArgumentException("Unknown recognition policy");finance.requireOpenPeriod(r.businessDate);r.recognitionPolicy=recognition;
   var items=lines(r);if(items.isEmpty())throw new IllegalStateException("Sale has no items");
   long cost=0;for(var line:items){var movementId=UUID.nameUUIDFromBytes((request.operationId()+":"+line.id).getBytes(StandardCharsets.UTF_8));var issued=recognition.equals("SALE_WITH_CUSTOMER_CUSTODY")?stock.issueToCustomerCustody(r.warehouseId,line.productId,line.quantityMilli,movementId,request.reason(),r.id,r.customerId,r.businessDate):stock.issue(r.warehouseId,line.productId,line.quantityMilli,movementId,request.reason());line.costXaf=issued.valueXaf();cost=Math.addExact(cost,line.costXaf);}
   finance.recognizeSale(r.id,request.operationId(),r.businessDate,r.totalXaf,cost);
   // Flush line valuations while parent is DRAFT; immutable-posted trigger seals them afterwards.
   em.flush();r.costXaf=cost;r.status="POSTED";r.version=Math.incrementExact(r.version);em.flush();
   documents.issue(new DocumentService.Issue(r.id,request.operationId(),"INVOICE",r.businessDate,r.customerId,Long.toString(r.totalXaf),items.stream().map(l->{var product=catalog.get(l.productId);return new DocumentService.Item(l.productId,product.sku(),product.name(),product.baseUnit(),Long.toString(l.quantityMilli),Long.toString(l.unitPriceXaf),Long.toString(l.lineTotalXaf));}).toList()));
   outbox(r,request.operationId());return r;
  }));
 }
 private Sale once(UUID operation,String material,Supplier<SaleEntity> action){
  var s=TenantContext.current();String hash=digest(material);
  // Transaction-scoped advisory lock serializes this tenant operation before checking its durable result.
  em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k",s.tenantId()+":"+operation).getSingleResult();
  var existing=em.createNativeQuery("select request_hash,source_id,company_id,branch_id,response_json::text from idempotency_result where tenant_id=:t and operation_id=:o").setParameter("t",s.tenantId()).setParameter("o",operation).getResultList();
  if(!existing.isEmpty()){Object[] row=(Object[])existing.getFirst();if(!hash.equals(row[0])||!s.companyId().equals(row[2])||!s.branchId().equals(row[3]))throw new IllegalStateException("Operation ID already used with another request or scope");var original=find((UUID)row[1],false);if(row[4]==null)throw new IllegalStateException("Operation already committed; response expired, retrieve sale "+original.id);return json.readValue((String)row[4],Sale.class);}
  var result=action.get();var response=view(result);em.createNativeQuery("insert into idempotency_result(tenant_id,operation_id,company_id,branch_id,request_hash,source_type,source_id,response_json) values(:t,:o,:c,:b,:h,'SALE',:s,cast(:r as jsonb))")
   .setParameter("t",s.tenantId()).setParameter("o",operation).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("h",hash).setParameter("s",result.id).setParameter("r",json.writeValueAsString(response)).executeUpdate();return response;
 }
 private void outbox(SaleEntity r,UUID operation){em.createNativeQuery("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload) values(:t,:id,:c,:b,:o,'SalePosted.v1','sale',:s,cast(:p as jsonb))")
  .setParameter("t",r.tenantId).setParameter("id",UUID.randomUUID()).setParameter("c",r.companyId).setParameter("b",r.branchId).setParameter("o",operation).setParameter("s",r.id).setParameter("p","{\"saleId\":\""+r.id+"\",\"totalXaf\":\""+r.totalXaf+"\"}").executeUpdate();}
 static String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}

}
