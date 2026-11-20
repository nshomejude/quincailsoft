package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="workforce_employee") @IdClass(WorkforceKey.class)
public class WorkforceEmployee {
    @Id @Column(name="tenant_id") public UUID tenantId;
    @Id @Column(name="company_id") public UUID companyId;
    @Id public UUID id;
    @Column(name="branch_id") public UUID branchId;
    @Column(name="employee_number") public String employeeNumber;
    public String name;
    public String email;
    public String department;
    @Column(name="job_title") public String jobTitle;
    @Column(name="contract_type") public String contractType;
    @Column(name="hired_on") public LocalDate hiredOn;
    @Column(name="left_on") public LocalDate leftOn;
    @Column(name="monthly_gross_xaf") public long monthlyGrossXaf;
    @Version public long version;
}
