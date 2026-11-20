package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="workforce_benefit_policy") @IdClass(WorkforceKey.class)
public class WorkforceBenefitPolicy {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="code") public String code;
 @Column(name="name") public String name;
 @Column(name="effective_from") public LocalDate effectiveFrom;
 @Column(name="effective_through") public LocalDate effectiveThrough;
 @Column(name="employee_deduction_xaf") public long employeeDeductionXaf;
 @Column(name="employer_contribution_xaf") public long employerContributionXaf;
 @Column(name="policy_reference") public String policyReference;
 @Column(name="status") public String status;
 @Column(name="requested_by") public UUID requestedBy;
 @Column(name="reviewed_by") public UUID reviewedBy;
 @Version public long version;
}
