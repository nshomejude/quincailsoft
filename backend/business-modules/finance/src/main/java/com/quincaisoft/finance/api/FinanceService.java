package com.quincaisoft.finance.api;
import java.util.*;import java.time.*;
public interface FinanceService {
 public record Entry(String account,long debit,long credit) {
  public Entry {if(account==null||account.isBlank()||debit<0||credit<0||(debit==0)==(credit==0)) throw new IllegalArgumentException("Exactly one positive debit or credit is required");}
 }
 public static void balanced(List<Entry> entries){
  if(entries==null||entries.size()<2)throw new IllegalArgumentException("At least two journal lines required");
  long debit=0,credit=0;for(var e:entries){debit=Math.addExact(debit,e.debit());credit=Math.addExact(credit,e.credit());}
  if(debit==0||debit!=credit)throw new IllegalArgumentException("Unbalanced journal");
 }
 record CreditSplit(long appliedToReceivable,long customerCredit) {}
 @com.quincaisoft.shared.TenantMutation UUID post(UUID sourceId,UUID operationId,LocalDate date,String sourceType,List<Entry> entries);
 void requireOpenPeriod(LocalDate date);
 long outstandingSale(UUID saleId);
 long pendingCollectionCount();
 @com.quincaisoft.shared.TenantMutation void applyReceipt(UUID saleId,long amount);
 @com.quincaisoft.shared.TenantMutation CreditSplit applyCredit(UUID saleId,long amount);
 @com.quincaisoft.shared.TenantMutation void recognizeSale(UUID saleId,UUID operationId,LocalDate date,long revenue,long cost);
}
