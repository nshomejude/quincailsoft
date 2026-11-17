package com.quincaisoft.subscriptions.application;
import com.quincaisoft.subscriptions.api.EntitlementService;
import com.quincaisoft.shared.ScopedTransactions;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
@Service public class DefaultEntitlementService implements EntitlementService {
 private final EntityManager em;private final ScopedTransactions tx;
 public DefaultEntitlementService(EntityManager em,ScopedTransactions tx){this.em=em;this.tx=tx;}
 public void requireCommerce(){tx.execute(()->{Number count=(Number)em.createNativeQuery("select count(*) from subscription_entitlement where feature='COMMERCE' and enabled and expires_at>now()").getSingleResult();if(count.longValue()!=1)throw new AccessDeniedException("Active commerce entitlement required");return null;});}
}
