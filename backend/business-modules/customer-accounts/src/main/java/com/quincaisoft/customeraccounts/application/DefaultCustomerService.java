package com.quincaisoft.customeraccounts.application;
import com.quincaisoft.customeraccounts.api.CustomerService;
import com.quincaisoft.shared.*;import com.quincaisoft.customeraccounts.infrastructure.CustomerEntity;
import jakarta.persistence.*;import java.util.*;import org.springframework.stereotype.Service;
@Service
public class DefaultCustomerService implements CustomerService {
 private final EntityManager em;private final ScopedTransactions tx;
 public DefaultCustomerService(EntityManager em,ScopedTransactions tx){this.em=em;this.tx=tx;}
 public List<Customer> list(){return tx.execute(()->{var scope=TenantContext.current();return em.createQuery("select c from CustomerEntity c where c.tenantId=:t and c.companyId=:c order by c.name,c.id",CustomerEntity.class).setParameter("t",scope.tenantId()).setParameter("c",scope.companyId()).setMaxResults(200).getResultList().stream().map(c->new Customer(c.id,c.name,c.active)).toList();});}
 public void requireEligible(UUID id){tx.execute(()->{var s=TenantContext.current();var c=em.find(CustomerEntity.class,new CustomerEntity.Key(s.tenantId(),s.companyId(),id),LockModeType.PESSIMISTIC_READ);if(c==null||!c.active)throw new IllegalArgumentException("Active customer in current company required");return null;});}
}
