package com.quincaisoft.catalog.application;
import com.quincaisoft.catalog.api.CatalogService;
import com.quincaisoft.catalog.infrastructure.*;
import com.quincaisoft.shared.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class DefaultCatalogService implements CatalogService {
 private final EntityManager em; private final ScopedTransactions transactions;
 public DefaultCatalogService(EntityManager em, ScopedTransactions transactions){this.em=em;this.transactions=transactions;}
 public List<Product> list(){return transactions.execute(()->{var s=TenantContext.current();return em.createQuery("select p from CatalogProduct p where p.tenantId=:t and p.companyId=:c order by p.sku",CatalogProduct.class).setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setMaxResults(200).getResultList().stream().map(CatalogProduct::view).toList();});}
 public Product get(UUID id){return transactions.execute(()->{var s=TenantContext.current();return em.createQuery("select p from CatalogProduct p where p.tenantId=:t and p.companyId=:c and p.id=:id",CatalogProduct.class).setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("id",Objects.requireNonNull(id)).getResultStream().findFirst().orElseThrow(()->new NoSuchElementException("Product not found in company scope")).view();});}
}
