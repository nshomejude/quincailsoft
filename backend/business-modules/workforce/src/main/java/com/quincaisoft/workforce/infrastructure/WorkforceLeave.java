package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="workforce_leave") @IdClass(WorkforceKey.class)
public class WorkforceLeave {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="employee_id") public UUID employeeId;
 @Column(name="from_date") public LocalDate fromDate;
 @Column(name="through_date") public LocalDate throughDate;
 @Column(name="leave_type") public String leaveType;
 @Column(name="requested_minutes") public int requestedMinutes;
 @Column(name="status") public String status;
 @Column(name="requested_by") public UUID requestedBy;
 @Column(name="reviewed_by") public UUID reviewedBy;
 @Column(name="reason") public String reason;
 @Version public long version;
}
