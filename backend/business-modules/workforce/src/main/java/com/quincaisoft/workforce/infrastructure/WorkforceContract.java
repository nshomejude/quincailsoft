package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="workforce_contract") @IdClass(WorkforceKey.class)
public class WorkforceContract {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="employee_id") public UUID employeeId;
 @Column(name="effective_from") public LocalDate effectiveFrom;
 @Column(name="contract_type") public String contractType;
 @Column(name="monthly_gross_xaf") public long monthlyGrossXaf;
 public String reference;
 @Column(name="recorded_by") public UUID recordedBy;
}
