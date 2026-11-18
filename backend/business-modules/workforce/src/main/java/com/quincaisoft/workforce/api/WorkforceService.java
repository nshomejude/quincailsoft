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
