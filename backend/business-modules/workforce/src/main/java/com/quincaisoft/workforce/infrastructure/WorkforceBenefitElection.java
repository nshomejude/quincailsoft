package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="workforce_benefit_election") @IdClass(WorkforceKey.class)
public class WorkforceBenefitElection {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="employee_id") public UUID employeeId;
 @Column(name="policy_id") public UUID policyId;
 @Column(name="policy_code") public String policyCode;
 @Column(name="from_date") public LocalDate fromDate;
 @Column(name="through_date") public LocalDate throughDate;
 @Column(name="status") public String status;
 @Column(name="requested_by") public UUID requestedBy;
 @Column(name="reviewed_by") public UUID reviewedBy;
 @Version public long version;
}
