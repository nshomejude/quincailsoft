package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="workforce_benefit_snapshot") @IdClass(WorkforceKey.class)
public class WorkforceBenefitSnapshot {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="payroll_id") public UUID payrollId;
 @Column(name="election_id") public UUID electionId;
 @Column(name="policy_id") public UUID policyId;
 @Column(name="code") public String code;
 @Column(name="name") public String name;
 @Column(name="employee_deduction_xaf") public long employeeDeductionXaf;
 @Column(name="employer_contribution_xaf") public long employerContributionXaf;
 @Column(name="policy_reference") public String policyReference;
}
