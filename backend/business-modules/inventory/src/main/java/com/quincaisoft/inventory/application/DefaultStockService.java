package com.quincaisoft.inventory.application;
import com.quincaisoft.inventory.api.StockService;
import com.quincaisoft.inventory.infrastructure.*;
import com.quincaisoft.shared.*;
import com.quincaisoft.catalog.api.CatalogService;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
@Service
public class DefaultStockService implements StockService {
 private final EntityManager em;private final ScopedTransactions transactions;private final CatalogService catalog;
 public DefaultStockService(EntityManager em,ScopedTransactions transactions,CatalogService catalog){this.em=em;this.transactions=transactions;this.catalog=catalog;}
 public void requireWarehouse(UUID warehouseId){transactions.execute(()->{var scope=TenantContext.current();var result=em.createNativeQuery("SELECT id FROM warehouse WHERE tenant_id=:t AND company_id=:c AND branch_id=:b AND id=:w AND active").setParameter("t",scope.tenantId()).setParameter("c",scope.companyId()).setParameter("b",scope.branchId()).setParameter("w",Objects.requireNonNull(warehouseId)).getResultList();if(result.isEmpty())throw new IllegalArgumentException("Warehouse not active in branch scope");return null;});}
 public StockResult issue(UUID warehouseId,UUID productId,long quantityMilli,UUID operationId,String reason){return mutate(warehouseId,productId,quantityMilli,0,operationId,reason,false);}
 public StockResult receive(UUID warehouseId,UUID productId,long quantityMilli,long valueXaf,UUID operationId,String reason){return mutate(warehouseId,productId,quantityMilli,valueXaf,operationId,reason,true);}
 private StockResult mutate(UUID warehouseId,UUID productId,long quantity,long value,UUID operationId,String reason,boolean receipt){
  Objects.requireNonNull(warehouseId);Objects.requireNonNull(productId);Objects.requireNonNull(operationId);
  if(quantity<=0||value<0||reason==null||reason.isBlank()||reason.length()>2000)throw new IllegalArgumentException("Valid quantity, value and reason required");
  return transactions.execute(()->{
   var scope=TenantContext.current();
   // Lock the scoped warehouse first: serializes initialization/replays, then product balance.
   var warehouses=em.createNativeQuery("SELECT id FROM warehouse WHERE tenant_id=:t AND company_id=:c AND branch_id=:b AND id=:w AND active FOR UPDATE")
    .setParameter("t",scope.tenantId()).setParameter("c",scope.companyId()).setParameter("b",scope.branchId()).setParameter("w",warehouseId).getResultList();
   if(warehouses.isEmpty())throw new IllegalArgumentException("Warehouse not active in branch scope");
   if(!catalog.get(productId).active())throw new IllegalArgumentException("Product not active in company scope");
   var prior=em.createQuery("select m from StockMovement m where m.tenantId=:t and m.companyId=:c and m.operationId=:o and m.warehouseId=:w and m.productId=:p",StockMovement.class)
    .setParameter("t",scope.tenantId()).setParameter("c",scope.companyId()).setParameter("o",operationId).setParameter("w",warehouseId).setParameter("p",productId).getResultStream().findFirst();
   if(prior.isPresent()){
    var m=prior.get();if(m.quantityMilli!=(receipt?quantity:-quantity)||!m.reason.equals(reason)||(receipt&&m.valueXaf!=value))throw new IllegalArgumentException("Operation identifier reused with different stock command");
    return new StockResult(m.id,Math.abs(m.quantityMilli),Math.abs(m.valueXaf));
   }
   var key=new StockBalance.Key(scope.tenantId(),scope.companyId(),warehouseId,productId);
   var balance=em.find(StockBalance.class,key,LockModeType.PESSIMISTIC_WRITE);
   if(balance==null){
    if(!receipt)throw new IllegalArgumentException("No merchant stock at warehouse");
    balance=new StockBalance();balance.tenantId=scope.tenantId();balance.companyId=scope.companyId();balance.branchId=scope.branchId();balance.warehouseId=warehouseId;balance.productId=productId;em.persist(balance);
   }
   if(!balance.branchId.equals(scope.branchId()))throw new IllegalArgumentException("Stock balance outside branch scope");
   long cost;if(receipt){balance.receive(quantity,value);cost=value;}else cost=balance.issue(quantity);
   var movement=new StockMovement();movement.id=UUID.randomUUID();movement.tenantId=scope.tenantId();movement.companyId=scope.companyId();movement.branchId=scope.branchId();movement.warehouseId=warehouseId;movement.productId=productId;movement.quantityMilli=receipt?quantity:-quantity;movement.valueXaf=receipt?cost:-cost;movement.operationId=operationId;movement.actorId=scope.actorId();movement.reason=reason;movement.occurredAt=Instant.now();em.persist(movement);em.flush();
   return new StockResult(movement.id,quantity,cost);
  });
 }
}
