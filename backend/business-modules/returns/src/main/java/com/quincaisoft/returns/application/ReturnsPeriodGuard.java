package com.quincaisoft.returns.application;

import com.quincaisoft.finance.api.PeriodCloseGuard;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/** Approved stock/credit obligations must post before the company period closes. */
@Component
public class ReturnsPeriodGuard implements PeriodCloseGuard {
 private final EntityManager em;
 public ReturnsPeriodGuard(EntityManager em){this.em=em;}
 public String domain(){return "returns";}
 public void verifyCanClose(LocalDate start,LocalDate end){
  long count=em.createQuery("select count(r) from CustomerReturnEntity r where r.status='APPROVED' and r.businessDate between :start and :end",Long.class).setParameter("start",start).setParameter("end",end).getSingleResult();
  if(count>0)throw new IllegalStateException("Approved unposted customer returns prevent period closure");
 }
}
