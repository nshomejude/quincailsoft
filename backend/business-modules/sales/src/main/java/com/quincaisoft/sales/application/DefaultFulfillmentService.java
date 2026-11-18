package com.quincaisoft.sales.application;
import com.quincaisoft.sales.api.*;import com.quincaisoft.sales.infrastructure.*;import com.quincaisoft.inventory.api.StockService;import com.quincaisoft.finance.api.FinanceService;import com.quincaisoft.shared.*;
import jakarta.persistence.*;import org.springframework.stereotype.Service;import tools.jackson.databind.json.JsonMapper;import java.util.*;import java.time.*;import java.util.function.*;
@Service public class DefaultFulfillmentService implements FulfillmentService {
 private final EntityManager em;private final ScopedTransactions tx;private final SalesService sales;private final StockService stock;private final FinanceService finance;private final JsonMapper json=JsonMapper.builder().build();
 public DefaultFulfillmentService(EntityManager em,ScopedTransactions tx,SalesService sales,StockService stock,FinanceService finance){this.em=em;this.tx=tx;this.sales=sales;this.stock=stock;this.finance=finance;}
 private Fulfillment view(FulfillmentEntity f){return new Fulfillment(f.id,f.saleId,f.customerId,f.status,f.businessDate,f.proofReference,""+f.version,details(f));}
 private static DeliveryDetails details(FulfillmentEntity task) {
  if (task.method == null && task.scheduledDate == null && task.address == null &&
      task.recipientName == null && task.recipientPhone == null && task.notes == null) return null;
  return new DeliveryDetails(task.method,task.scheduledDate,task.address,
      task.recipientName,task.recipientPhone,task.notes);
 }

 private static void applyDetails(FulfillmentEntity task, DeliveryDetails details) {
  if (details == null) return;
  if (details.method() != null && !Set.of("PICKUP","DELIVERY").contains(details.method()))
   throw new IllegalArgumentException("Invalid fulfillment method");
  if (details.scheduledDate() != null && details.scheduledDate().isBefore(task.businessDate))
   throw new IllegalArgumentException("Scheduled date precedes fulfillment business date");
  task.method = details.method();
  task.scheduledDate = details.scheduledDate();
  task.address = optionalText(details.address(),1000);
  task.recipientName = optionalText(details.recipientName(),200);
  task.recipientPhone = optionalText(details.recipientPhone(),50);
  task.notes = optionalText(details.notes(),2000);
 }

 private static String optionalText(String value, int limit) {
  if (value == null || value.isBlank()) return null;
  if (value.length() > limit) throw new IllegalArgumentException("Fulfillment field exceeds "+limit+" characters");
  return value.strip();
 }

 private FulfillmentEntity find(UUID id,LockModeType lock){var s=TenantContext.current();var f=em.find(FulfillmentEntity.class,new FulfillmentEntity.Key(s.tenantId(),s.companyId(),id),lock);if(f==null||!f.branchId.equals(s.branchId()))throw new NoSuchElementException("Fulfillment not found");return f;}
 public List<Fulfillment> list(){return tx.execute(()->em.createQuery("select f from FulfillmentEntity f order by f.createdAt desc,f.id",FulfillmentEntity.class).setMaxResults(200).getResultList().stream().map(this::view).toList());}
 @Override
 public long pendingPreparationCount() {
  return tx.execute(() -> {
   var scope = TenantContext.current();
   return em.createQuery("""
       select count(f) from FulfillmentEntity f
       where f.tenantId = :tenant and f.companyId = :company and f.branchId = :branch
         and f.status = 'PENDING'
       """, Long.class)
       .setParameter("tenant", scope.tenantId())
       .setParameter("company", scope.companyId())
       .setParameter("branch", scope.branchId())
       .getSingleResult();
  });
 }
 @Override
 public PickList pickList(UUID fulfillmentId) {
  return tx.execute(() -> {
   var task = find(fulfillmentId, LockModeType.NONE);
   var sale = sales.valuation(task.saleId);
   var lines = sale.lines().stream()
       .map(line -> new PickLine(line.productId(), Long.toString(line.quantityMilli())))
       .toList();
   return new PickList(task.id, task.saleId, sale.warehouseId(), task.status, lines);
  });
 }
 public Fulfillment get(UUID id){return tx.execute(()->view(find(id, LockModeType.NONE)));}
 public Fulfillment draft(Draft c){if(c.businessDate()==null||c.customerId()==null||c.saleId()==null)throw new IllegalArgumentException("Sale, customer and date required");text(c.reason());return tx.execute(()->once(c.operationId(),"DRAFT|"+(c.delivery()==null?c.legacyFingerprintMaterial():json.writeValueAsString(c)),()->{var sale=sales.get(c.saleId());if(!sale.status().equals("POSTED")||!sale.customerId().equals(c.customerId())||c.businessDate().isBefore(sale.businessDate()))throw new IllegalArgumentException("Completed sale for the same customer and valid date required");var s=TenantContext.current();var f=new FulfillmentEntity();f.tenantId=s.tenantId();f.companyId=s.companyId();f.branchId=s.branchId();f.id=UUID.randomUUID();f.saleId=c.saleId();f.customerId=c.customerId();f.businessDate=c.businessDate();f.createdBy=s.actorId();f.createdAt=Instant.now();f.status="PENDING";applyDetails(f,c.delivery());em.persist(f);em.flush();return f;}));}
 @Override
 public Fulfillment dispatchLines(UUID id, DispatchLines command) {
  text(command.reason());
  if (command.version() == null || !command.version().matches("0|[1-9][0-9]*"))
   throw new IllegalArgumentException("Version required");
  if (command.lines() == null || command.lines().isEmpty() || command.lines().size() > 500)
   throw new IllegalArgumentException("One to 500 dispatch lines required");
  long version = Long.parseLong(command.version());
  return tx.execute(() -> once(command.operationId(),"DISPATCH_LINES|"+id+"|"+json.writeValueAsString(command),() -> {
   var task = find(id, LockModeType.PESSIMISTIC_WRITE);
   if (task.version != version) throw new RevisionConflict("Fulfillment changed");
   if (!Set.of("PENDING","PARTIAL").contains(task.status)) throw new IllegalStateException("Invalid fulfillment state");
   finance.requireOpenPeriod(task.businessDate);
   stock.requireCustodyCollected(task.saleId);

   var remaining = new HashMap<UUID,Long>();
   for (var line : sales.valuation(task.saleId).lines())
    remaining.merge(line.productId(),line.quantityMilli(),Math::addExact);
   var previous = em.createNativeQuery("""
       select product_id, sum(quantity_milli) from fulfillment_dispatch_line
       where tenant_id=:tenant and company_id=:company and branch_id=:branch and fulfillment_id=:id
       group by product_id
       """,Object[].class)
       .setParameter("tenant",task.tenantId).setParameter("company",task.companyId)
       .setParameter("branch",task.branchId).setParameter("id",task.id).getResultList();
   for (var result : previous) {
    var row = (Object[]) result;
    remaining.compute((UUID) row[0],(product,quantity) -> Math.subtractExact(quantity,
        ((java.math.BigDecimal) row[1]).longValueExact()));
   }

   var seen = new HashSet<UUID>();
   var scope = TenantContext.current();
   for (var line : command.lines()) {
    if (line == null || line.productId() == null || !seen.add(line.productId()) ||
        line.quantityMilli() == null || !line.quantityMilli().matches("[1-9][0-9]*"))
     throw new IllegalArgumentException("Distinct products and positive dispatch quantities required");
    long quantity = Long.parseLong(line.quantityMilli());
    long available = remaining.getOrDefault(line.productId(),0L);
    if (quantity > available) throw new IllegalArgumentException("Dispatch exceeds remaining authorized quantity");
    remaining.put(line.productId(),available-quantity);
    em.createNativeQuery("""
        insert into fulfillment_dispatch_line
        (tenant_id,company_id,branch_id,fulfillment_id,operation_id,product_id,quantity_milli,dispatched_at,dispatched_by)
        values (:tenant,:company,:branch,:id,:operation,:product,:quantity,:at,:actor)
        """).setParameter("tenant",task.tenantId).setParameter("company",task.companyId)
        .setParameter("branch",task.branchId).setParameter("id",task.id)
        .setParameter("operation",command.operationId()).setParameter("product",line.productId())
        .setParameter("quantity",quantity).setParameter("at",Instant.now()).setParameter("actor",scope.actorId())
        .executeUpdate();
   }
   var nextStatus = remaining.values().stream().allMatch(quantity -> quantity == 0) ? "DISPATCHED" : "PARTIAL";
   if (nextStatus.equals(task.status)) em.lock(task, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
   task.status = nextStatus;
   em.flush();
   return task;
  }));
 }
 @Override
 public List<DispatchEntry> dispatchHistory(UUID fulfillmentId) {
  return tx.execute(() -> {
   var task = find(fulfillmentId, LockModeType.NONE);
   List<?> rows = em.createNativeQuery("""
       select operation_id, product_id, quantity_milli, cast(extract(epoch from dispatched_at)*1000000 as bigint), dispatched_by
       from fulfillment_dispatch_line
       where tenant_id=:tenant and company_id=:company and branch_id=:branch and fulfillment_id=:id
       order by dispatched_at, operation_id, product_id
       """, DispatchEntryRow.class)
       .setParameter("tenant",task.tenantId).setParameter("company",task.companyId)
       .setParameter("branch",task.branchId).setParameter("id",task.id).getResultList();
   return rows.stream().map(DispatchEntryRow.class::cast)
       .map(row -> new DispatchEntry(row.operationId(),row.productId(),Long.toString(row.quantityMilli()),
           Instant.EPOCH.plus(row.dispatchedMicros(),java.time.temporal.ChronoUnit.MICROS),row.dispatchedBy())).toList();
  });
 }

 public record DispatchEntryRow(UUID operationId,UUID productId,Long quantityMilli,
                                Long dispatchedMicros,UUID dispatchedBy) {}
 public Fulfillment dispatch(UUID id,Action c){return action(id,c,false);}
 public Fulfillment complete(UUID id,Action c){return action(id,c,true);}
 private Fulfillment action(UUID id,Action c,boolean complete){text(c.reason());if(c.version()==null||!c.version().matches("0|[1-9][0-9]*"))throw new IllegalArgumentException("Version required");long version=Long.parseLong(c.version());if(complete)text(c.proofReference());return tx.execute(()->once(c.operationId(),(complete?"COMPLETE":"DISPATCH")+"|"+id+"|"+c,()->{var f=find(id, LockModeType.PESSIMISTIC_WRITE);if(f.version!=version)throw new RevisionConflict("Fulfillment changed");if(!(complete?Set.of("PENDING","DISPATCHED").contains(f.status):f.status.equals("PENDING")))throw new IllegalStateException("Invalid fulfillment state");finance.requireOpenPeriod(f.businessDate);stock.requireCustodyCollected(f.saleId);if(complete){f.proofReference=c.proofReference();f.status="COMPLETED";}else f.status="DISPATCHED";em.flush();return f;}));}
 private Fulfillment once(UUID op,String material,Supplier<FulfillmentEntity> action){if(op==null)throw new IllegalArgumentException("Operation required");var s=TenantContext.current();String hash=DefaultSalesService.digest(material);em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:key,0))").setParameter("key",s.tenantId()+":"+op).getSingleResult();var rows=em.createNativeQuery("select request_hash,response_json::text,company_id,branch_id,actor_id from idempotency_result where tenant_id=:t and operation_id=:o").setParameter("t",s.tenantId()).setParameter("o",op).getResultList();if(!rows.isEmpty()){Object[] row=(Object[])rows.getFirst();if(!hash.equals(row[0])||!s.companyId().equals(row[2])||!s.branchId().equals(row[3])||!s.actorId().equals(row[4]))throw new IllegalStateException("Operation belongs to another request or actor");if(row[1]==null)throw new IllegalStateException("Committed result expired; reconcile original operation");return json.readValue((String)row[1],Fulfillment.class);}var f=action.get();var result=view(f);String body=json.writeValueAsString(result);em.createNativeQuery("insert into idempotency_result(tenant_id,operation_id,company_id,branch_id,request_hash,source_type,source_id,response_json)values(:t,:o,:c,:b,:h,'FULFILLMENT',:id,cast(:body as jsonb))").setParameter("t",s.tenantId()).setParameter("o",op).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("h",hash).setParameter("id",f.id).setParameter("body",body).executeUpdate();em.createNativeQuery("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload)values(:t,:event,:c,:b,:o,'FulfillmentChanged.v1','sale_fulfillment',:id,cast(:body as jsonb))").setParameter("t",s.tenantId()).setParameter("event",UUID.randomUUID()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("o",op).setParameter("id",f.id).setParameter("body",json.writeValueAsString(Map.of("fulfillmentId",result.id(),"saleId",result.saleId(),"status",result.status()))).executeUpdate();return result;}
 private static void text(String s){if(s==null||s.isBlank()||s.length()>2000)throw new IllegalArgumentException("Bounded reason/proof required");}
}
