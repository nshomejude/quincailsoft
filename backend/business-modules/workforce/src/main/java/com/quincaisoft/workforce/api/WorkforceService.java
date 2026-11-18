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
    Contract changeContract(UUID employeeId, ContractChange input);
    List<Contract> contracts(UUID employeeId, int offset, int limit);
    Contract contractAt(UUID employeeId, LocalDate date);
    Payslip draftDetailedPayroll(DetailedPayroll input);
    List<Deduction> deductions(UUID payslipId);
    Payslip cancelPayroll(UUID id, Approval input);
    Reversal requestReversal(UUID payslipId, ReversalRequest input);
    Reversal approveReversal(UUID id, Approval input);
    Payment payBank(UUID payslipId, BankPayment input);
    List<Reversal> reversals(UUID payslipId);
    List<Payment> payments(UUID payslipId);
    String payslipHtml(UUID id);
    String payrollCsv(LocalDate from, LocalDate through, int offset, int limit);
    Employee saveEmployee(UUID id, EmployeeSave input);
    List<Employee> employees(int offset, int limit);
    Employee employee(UUID id);
    Attendance recordAttendance(AttendanceSave input);
    List<Attendance> attendance(UUID employeeId, LocalDate from, LocalDate through);
    Payslip draftPayroll(PayrollDraft input);
    Payslip approvePayroll(UUID id, Approval input);
    Payslip payslip(UUID id);
    List<Payslip> payroll(LocalDate from, LocalDate through, int offset, int limit);
}
