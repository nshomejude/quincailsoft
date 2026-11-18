package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="workforce_deduction") @IdClass(WorkforceKey.class)
public class WorkforceDeduction {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="payroll_id") public UUID payrollId;
 public String code;
 public String label;
 @Column(name="amount_xaf") public long amountXaf;
 @Column(name="policy_reference") public String policyReference;
}
