package com.quincaisoft.finance.infrastructure;import jakarta.persistence.*;import java.time.*;import java.util.*;import java.io.Serializable;
@org.hibernate.annotations.DynamicUpdate @Entity @Table(name="financial_period")@IdClass(FinancialPeriodEntity.Key.class)public class FinancialPeriodEntity {
 @Id @Column(name="tenant_id")public UUID tenantId;@Id @Column(name="company_id")public UUID companyId;@Id public UUID id;@Column(name="start_date")public LocalDate startDate;@Column(name="end_date")public LocalDate endDate;public String status;@Version public long version;public record Key(UUID tenantId,UUID companyId,UUID id)implements Serializable{}
}
