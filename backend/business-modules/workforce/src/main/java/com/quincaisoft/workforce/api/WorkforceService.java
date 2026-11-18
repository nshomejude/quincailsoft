package com.quincaisoft.workforce.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Personnel and explicit, reviewed payroll accruals. No statutory tax calculator or payment certification. */
public interface WorkforceService {
    record EmployeeSave(UUID operationId, String employeeNumber, String name, String email,
                        String department, String jobTitle, String contractType, LocalDate hiredOn,
                        LocalDate leftOn, String monthlyGrossXaf, String version, String reason) {}
    record Employee(UUID id, String employeeNumber, String name, String email, String department,
                    String jobTitle, String contractType, LocalDate hiredOn, LocalDate leftOn,
                    String monthlyGrossXaf, String version) {}
    record AttendanceSave(UUID operationId, UUID employeeId, LocalDate date, String status,
                          int workedMinutes, String reason) {}
    record Attendance(UUID id, UUID employeeId, LocalDate date, String status, int workedMinutes) {}
    record PayrollDraft(UUID operationId, UUID employeeId, LocalDate periodStart, LocalDate periodEnd,
                        LocalDate businessDate, String grossXaf, String deductionXaf,
                        String calculationPolicy, String reason) {}
    record Approval(UUID operationId, String version, String reason) {}
    record Payslip(UUID id, UUID employeeId, String employeeName, String employeeNumber,
                   LocalDate periodStart, LocalDate periodEnd, LocalDate businessDate,
                   String grossXaf, String deductionXaf, String netXaf, String calculationPolicy,
                   String status, UUID preparedBy, UUID approvedBy, UUID journalId, String version) {}
    record ContractChange(UUID operationId, String employeeVersion, LocalDate effectiveFrom, String contractType,
                          String monthlyGrossXaf, String reference, String reason) {}
    record Contract(UUID id, UUID employeeId, LocalDate effectiveFrom, String contractType,
                    String monthlyGrossXaf, String reference, UUID recordedBy) {}
    record Deduction(String code, String label, String amountXaf, String policyReference) {}
    record DetailedPayroll(PayrollDraft payroll, List<Deduction> deductions, UUID replacesPayslipId) {}
    record ReversalRequest(UUID operationId, String version, LocalDate businessDate, String reason) {}
    record Reversal(UUID id, UUID payslipId, LocalDate businessDate, String status, UUID requestedBy,
                    UUID approvedBy, UUID journalId, String version) {}
    record BankPayment(UUID operationId, String version, LocalDate businessDate, UUID bankEvidenceId, String reason) {}
    record Payment(UUID id, UUID payslipId, LocalDate businessDate, String amountXaf, UUID bankEvidenceId,
                   UUID journalId, String status) {}
    @com.quincaisoft.shared.TenantMutation Contract changeContract(UUID employeeId, ContractChange input);
    List<Contract> contracts(UUID employeeId, int offset, int limit);
    Contract contractAt(UUID employeeId, LocalDate date);
    @com.quincaisoft.shared.TenantMutation Payslip draftDetailedPayroll(DetailedPayroll input);
    List<Deduction> deductions(UUID payslipId);
    Payslip cancelPayroll(UUID id, Approval input);
    Reversal requestReversal(UUID payslipId, ReversalRequest input);
    Reversal approveReversal(UUID id, Approval input);
    @com.quincaisoft.shared.TenantMutation Payment payBank(UUID payslipId, BankPayment input);
    List<Reversal> reversals(UUID payslipId);
    List<Payment> payments(UUID payslipId);
    String payslipHtml(UUID id);
    String payrollCsv(LocalDate from, LocalDate through, int offset, int limit);
    record LeaveRequest(UUID operationId, UUID employeeId, LocalDate from, LocalDate through,
                        String leaveType, int requestedMinutes, String reason) {}
    record Leave(UUID id, UUID employeeId, LocalDate from, LocalDate through, String leaveType,
                 int requestedMinutes, String status, UUID requestedBy, UUID reviewedBy, String reason, String version) {}
    record BenefitPolicyRequest(UUID operationId, String code, String name, LocalDate effectiveFrom, LocalDate effectiveThrough,
                                String employeeDeductionXaf, String employerContributionXaf, String policyReference, String reason) {}
    record BenefitPolicy(UUID id, String code, String name, LocalDate effectiveFrom, LocalDate effectiveThrough,
                         String employeeDeductionXaf, String employerContributionXaf, String policyReference,
                         String status, UUID requestedBy, UUID reviewedBy, String version) {}
    record BenefitElectionRequest(UUID operationId, UUID employeeId, UUID policyId, LocalDate from, LocalDate through, String reason) {}
    record BenefitElection(UUID id, UUID employeeId, UUID policyId, String policyCode, LocalDate from, LocalDate through,
                           String status, UUID requestedBy, UUID reviewedBy, String version) {}
    record Benefit(UUID electionId, UUID policyId, String code, String name, String employeeDeductionXaf,
                   String employerContributionXaf, String policyReference) {}
    @com.quincaisoft.shared.TenantMutation Leave requestLeave(LeaveRequest input);
    Leave leave(UUID id);
    List<Leave> leaves(UUID employeeId, String status, LocalDate from, LocalDate through, int offset, int limit);
    @com.quincaisoft.shared.TenantMutation Leave decideLeave(UUID id, String action, Approval input);
    @com.quincaisoft.shared.TenantMutation BenefitPolicy requestBenefitPolicy(BenefitPolicyRequest input);
    BenefitPolicy benefitPolicy(UUID id);
    List<BenefitPolicy> benefitPolicies(String status, int offset, int limit);
    @com.quincaisoft.shared.TenantMutation BenefitPolicy decideBenefitPolicy(UUID id, String action, Approval input);
    @com.quincaisoft.shared.TenantMutation BenefitElection requestBenefitElection(BenefitElectionRequest input);
    BenefitElection benefitElection(UUID id);
    List<BenefitElection> benefitElections(UUID employeeId, String status, int offset, int limit);
    @com.quincaisoft.shared.TenantMutation BenefitElection decideBenefitElection(UUID id, String action, Approval input);
    List<Benefit> benefitPreview(UUID employeeId, LocalDate from, LocalDate through);
    List<Benefit> payrollBenefits(UUID payrollId);
    @com.quincaisoft.shared.TenantMutation Employee saveEmployee(UUID id, EmployeeSave input);
    List<Employee> employees(int offset, int limit);
    Employee employee(UUID id);
    @com.quincaisoft.shared.TenantMutation Attendance recordAttendance(AttendanceSave input);
    List<Attendance> attendance(UUID employeeId, LocalDate from, LocalDate through);
    @com.quincaisoft.shared.TenantMutation Payslip draftPayroll(PayrollDraft input);
    @com.quincaisoft.shared.TenantMutation Payslip approvePayroll(UUID id, Approval input);
    Payslip payslip(UUID id);
    List<Payslip> payroll(LocalDate from, LocalDate through, int offset, int limit);
}
