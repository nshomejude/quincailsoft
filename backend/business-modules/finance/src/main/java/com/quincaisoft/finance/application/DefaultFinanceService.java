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
  tx.execute(()->{var s=TenantContext.current();
   // Lock period rows so concurrent closure cannot race this posting.
   var periods=em.createNativeQuery("select status from financial_period where tenant_id=:t and company_id=:c and start_date<=:d and end_date>=:d for share")
    .setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("d",date).getResultList();
   if(periods.size()!=1||!"Open".equals(periods.getFirst()))throw new IllegalStateException("Exactly one open financial period is required");
   var j=new JournalEntryEntity();j.tenantId=s.tenantId();j.companyId=s.companyId();j.branchId=s.branchId();j.id=UUID.randomUUID();j.operationId=operationId;j.sourceType="SALE";j.sourceId=saleId;j.businessDate=date;j.currency="XAF";j.createdAt=Instant.now();em.persist(j);em.flush();
   for(var e:entries){var l=new JournalLineEntity();l.tenantId=s.tenantId();l.companyId=s.companyId();l.entryId=j.id;l.id=UUID.randomUUID();l.accountCode=e.account();l.debitXaf=e.debit();l.creditXaf=e.credit();em.persist(l);}em.flush();return null;});
 }
}
