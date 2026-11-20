package com.quincaisoft.company.infrastructure;
import jakarta.persistence.*;import java.util.UUID;
@Entity @Table(name="company_business_defaults") @IdClass(CompanyDefaults.Key.class)
public class CompanyDefaults {
 public record Key(UUID tenantId,UUID companyId) implements java.io.Serializable{}
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Column(name="price_list_name") public String priceListName;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) @Column(columnDefinition="char(3)") public String currency;public String locale;
 @Column(name="fiscal_review_status") public String fiscalReviewStatus;
 @Column(name="tax_configuration_status") public String taxConfigurationStatus;
}
