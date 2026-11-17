package com.quincaisoft.finance.application;
import com.quincaisoft.finance.api.FinanceService;
import com.quincaisoft.shared.*;
import com.quincaisoft.finance.infrastructure.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import java.util.*; import java.time.*;
@Service
public class DefaultFinanceService implements FinanceService {
 private final EntityManager em; private final ScopedTransactions tx;
 public DefaultFinanceService(EntityManager em,ScopedTransactions tx){this.em=em;this.tx=tx;}
 public void recognizeSale(UUID saleId,UUID operationId,LocalDate date,long revenue,long cost){
  if(revenue<=0||cost<=0)throw new IllegalArgumentException("Positive sale revenue and cost required");
  var entries=List.of(new Entry("ACCOUNTS_RECEIVABLE",revenue,0),new Entry("SALES_REVENUE",0,revenue),new Entry("COST_OF_GOODS_SOLD",cost,0),new Entry("MERCHANT_INVENTORY",0,cost));FinanceService.balanced(entries);
  tx.execute(()->{post(saleId,operationId,date,"SALE",entries);var s=TenantContext.current();var balance=new ReceivableBalanceEntity();balance.tenantId=s.tenantId();balance.companyId=s.companyId();balance.branchId=s.branchId();balance.saleId=saleId;balance.outstandingXaf=revenue;em.persist(balance);em.flush();return null;});
 }
 public void requireOpenPeriod(LocalDate date){
  if(date==null)throw new IllegalArgumentException("Business date required");
  tx.execute(()->{var s=TenantContext.current();var periods=em.createNativeQuery("select status from financial_period where tenant_id=:t and company_id=:c and start_date<=:d and end_date>=:d for share").setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("d",date).getResultList();
   if(periods.size()!=1||!"Open".equals(periods.getFirst()))throw new IllegalStateException("Exactly one open financial period is required");return null;});
 }
 public UUID post(UUID sourceId,UUID operationId,LocalDate date,String sourceType,List<Entry> entries){
  FinanceService.balanced(entries);if(sourceId==null||operationId==null||sourceType==null||sourceType.isBlank())throw new IllegalArgumentException("Journal source required");
  return tx.execute(()->{requireOpenPeriod(date);var s=TenantContext.current();
   var j=new JournalEntryEntity();j.tenantId=s.tenantId();j.companyId=s.companyId();j.branchId=s.branchId();j.id=UUID.randomUUID();j.operationId=operationId;j.sourceType=sourceType;j.sourceId=sourceId;j.businessDate=date;j.currency="XAF";j.createdAt=Instant.now();em.persist(j);em.flush();
   for(var e:entries){var l=new JournalLineEntity();l.tenantId=s.tenantId();l.companyId=s.companyId();l.entryId=j.id;l.id=UUID.randomUUID();l.accountCode=e.account();l.debitXaf=e.debit();l.creditXaf=e.credit();em.persist(l);}em.flush();return j.id;});
 }
 private ReceivableBalanceEntity balance(UUID id){var s=TenantContext.current();var r=em.find(ReceivableBalanceEntity.class,new ReceivableBalanceEntity.Key(s.tenantId(),s.companyId(),id),jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);if(r==null||!r.branchId.equals(s.branchId()))throw new NoSuchElementException("Scoped receivable not found");return r;}
 public long outstandingSale(UUID id){return tx.execute(()->balance(id).outstandingXaf);}
 public void applyReceipt(UUID id,long amount){if(amount<=0)throw new IllegalArgumentException("Positive receipt required");tx.execute(()->{var r=balance(id);if(amount>r.outstandingXaf)throw new IllegalArgumentException("Payment exceeds outstanding debt; record surplus as a deposit");r.outstandingXaf=Math.subtractExact(r.outstandingXaf,amount);em.flush();return null;});}
 public CreditSplit applyCredit(UUID id,long amount){if(amount<=0)throw new IllegalArgumentException("Positive credit required");return tx.execute(()->{var r=balance(id);long applied=Math.min(amount,r.outstandingXaf);r.outstandingXaf=Math.subtractExact(r.outstandingXaf,applied);em.flush();return new CreditSplit(applied,Math.subtractExact(amount,applied));});}
}
