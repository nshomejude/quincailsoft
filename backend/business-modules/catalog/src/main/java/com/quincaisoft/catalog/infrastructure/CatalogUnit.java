package com.quincaisoft.catalog.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.io.Serializable;
@Entity @Table(name="catalog_unit") @IdClass(CatalogUnit.Key.class)
public class CatalogUnit {
 @Id @Column(name="tenant_id") public UUID tenantId; @Id @Column(name="company_id") public UUID companyId; @Id public UUID id;
 public String code;public String name;@Column(name="base_unit") public String baseUnit;@Column(name="factor_numerator") public long factorNumerator;@Column(name="factor_denominator") public long factorDenominator;
 @Column(name="decimal_precision") public int precision;public boolean active;@Version public long version;
 public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable {}
}
