package com.quincaisoft.workforce.infrastructure;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name="workforce_payment") @IdClass(WorkforceKey.class)
public class WorkforcePayment {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="payroll_id") public UUID payrollId;
 @Column(name="business_date") public LocalDate businessDate;
 @Column(name="amount_xaf") public long amountXaf;
 @Column(name="bank_evidence_id") public UUID bankEvidenceId;
 @Column(name="journal_id") public UUID journalId;
 @Column(name="recorded_by") public UUID recordedBy;
}
