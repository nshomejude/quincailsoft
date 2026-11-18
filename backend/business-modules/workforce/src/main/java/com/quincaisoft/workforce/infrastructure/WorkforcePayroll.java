package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="workforce_payroll") @IdClass(WorkforceKey.class)
public class WorkforcePayroll {
    @Id @Column(name="tenant_id") public UUID tenantId;
    @Id @Column(name="company_id") public UUID companyId;
    @Id public UUID id;
    @Column(name="branch_id") public UUID branchId;
    @Column(name="employee_id") public UUID employeeId;
    @Column(name="employee_name") public String employeeName;
    @Column(name="employee_number") public String employeeNumber;
    @Column(name="period_start") public LocalDate periodStart;
    @Column(name="period_end") public LocalDate periodEnd;
    @Column(name="business_date") public LocalDate businessDate;
    @Column(name="gross_xaf") public long grossXaf;
    @Column(name="deduction_xaf") public long deductionXaf;
    @Column(name="net_xaf") public long netXaf;
    @Column(name="calculation_policy") public String calculationPolicy;
    public String status;
    @Column(name="prepared_by") public UUID preparedBy;
    @Column(name="approved_by") public UUID approvedBy;
    @Column(name="journal_id") public UUID journalId;
    @Version public long version;
}
