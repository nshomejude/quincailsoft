package com.quincaisoft.finance.infrastructure;
import jakarta.persistence.*;import java.util.UUID;
@Entity @Table(name="finance_bootstrap_mapping") @IdClass(BootstrapChartMapping.Key.class)
public class BootstrapChartMapping {
 public record Key(UUID tenantId,UUID companyId,String operationalCode) implements java.io.Serializable{}
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="operational_code") public String operationalCode;
 @Column(name="statutory_account_code") public String statutoryAccountCode;
 @Column(name="review_status") public String reviewStatus;
}
