package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="workforce_reversal") @IdClass(WorkforceKey.class)
public class WorkforceReversal {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="payroll_id") public UUID payrollId;
 @Column(name="business_date") public LocalDate businessDate;
 public String status;
 public String reason;
 @Column(name="requested_by") public UUID requestedBy;
 @Column(name="approved_by") public UUID approvedBy;
 @Column(name="journal_id") public UUID journalId;
 @Version public long version;
}
