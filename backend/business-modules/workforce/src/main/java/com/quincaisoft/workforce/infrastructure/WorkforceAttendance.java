package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="workforce_attendance") @IdClass(WorkforceKey.class)
public class WorkforceAttendance {
    @Id @Column(name="tenant_id") public UUID tenantId;
    @Id @Column(name="company_id") public UUID companyId;
    @Id public UUID id;
    @Column(name="branch_id") public UUID branchId;
    @Column(name="employee_id") public UUID employeeId;
    @Column(name="business_date") public LocalDate date;
    public String status;
    @Column(name="worked_minutes") public int workedMinutes;
}
