package com.quincaisoft.workforce.web;
import com.quincaisoft.workforce.api.WorkforceService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

/** All service paths enforce capability and fresh branch membership before reads or idempotent replay. */
@RestController @RequestMapping("/api/v1/workforce")
public class WorkforceController {
    private final WorkforceService service;
    public WorkforceController(WorkforceService service){this.service=service;}
    @GetMapping("/employees") public List<WorkforceService.Employee> employees(@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="25")int limit){return service.employees(offset,limit);}
    @GetMapping("/employees/{id}") public WorkforceService.Employee employee(@PathVariable UUID id){return service.employee(id);}
    @PostMapping("/employees") public WorkforceService.Employee create(@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.EmployeeSave body){match(operation,body.operationId());return service.saveEmployee(null,body);}
    @PutMapping("/employees/{id}") public WorkforceService.Employee update(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.EmployeeSave body){match(operation,body.operationId());return service.saveEmployee(id,body);}
    @PostMapping("/attendance") public WorkforceService.Attendance record(@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.AttendanceSave body){match(operation,body.operationId());return service.recordAttendance(body);}
    @GetMapping("/employees/{id}/attendance") public List<WorkforceService.Attendance> attendance(@PathVariable UUID id,@RequestParam LocalDate from,@RequestParam LocalDate through){return service.attendance(id,from,through);}
    @PostMapping("/payroll") public WorkforceService.Payslip draft(@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.PayrollDraft body){match(operation,body.operationId());return service.draftPayroll(body);}
    @PostMapping("/payroll/{id}/approve") public WorkforceService.Payslip approve(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID operation,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.Approval body){match(operation,body.operationId());return service.approvePayroll(id,body);}
    @GetMapping("/payroll/{id}") public WorkforceService.Payslip get(@PathVariable UUID id){return service.payslip(id);}
    @GetMapping("/payroll") public List<WorkforceService.Payslip> payroll(@RequestParam LocalDate from,@RequestParam LocalDate through,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="25")int limit){return service.payroll(from,through,offset,limit);}
    @PostMapping("/employees/{id}/contracts") public WorkforceService.Contract changeContract(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.ContractChange body){match(op,body.operationId());return service.changeContract(id,body);}
    @GetMapping("/employees/{id}/contracts") public List<WorkforceService.Contract> contracts(@PathVariable UUID id,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="25")int limit){return service.contracts(id,offset,limit);}
    @GetMapping("/employees/{id}/contract-at") public WorkforceService.Contract contractAt(@PathVariable UUID id,@RequestParam LocalDate date){return service.contractAt(id,date);}
    @PostMapping("/payroll/detailed") public WorkforceService.Payslip detailed(@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.DetailedPayroll body){match(op,body.payroll().operationId());return service.draftDetailedPayroll(body);}
    @GetMapping("/payroll/{id}/deductions") public List<WorkforceService.Deduction> deductions(@PathVariable UUID id){return service.deductions(id);}
    @PostMapping("/payroll/{id}/cancel") public WorkforceService.Payslip cancel(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.Approval body){match(op,body.operationId());return service.cancelPayroll(id,body);}
    @PostMapping("/payroll/{id}/reversals") public WorkforceService.Reversal requestReversal(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.ReversalRequest body){match(op,body.operationId());return service.requestReversal(id,body);}
    @PostMapping("/payroll/reversals/{id}/approve") public WorkforceService.Reversal approveReversal(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.Approval body){match(op,body.operationId());return service.approveReversal(id,body);}
    @PostMapping("/payroll/{id}/bank-payment") public WorkforceService.Payment payBank(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID op,@RequestHeader("X-Request-ID")UUID request,@RequestBody WorkforceService.BankPayment body){match(op,body.operationId());return service.payBank(id,body);}
    @GetMapping(value="/payroll/{id}/print",produces="text/html;charset=UTF-8") public org.springframework.http.ResponseEntity<String> print(@PathVariable UUID id){return org.springframework.http.ResponseEntity.ok().header("Cache-Control","no-store").header("Content-Security-Policy","default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'").header("X-Content-Type-Options","nosniff").body(service.payslipHtml(id));}
    @GetMapping(value="/payroll/export.csv",produces="text/csv;charset=UTF-8") public org.springframework.http.ResponseEntity<String> export(@RequestParam LocalDate from,@RequestParam LocalDate through,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="100")int limit){return org.springframework.http.ResponseEntity.ok().header("Cache-Control","no-store").header("Content-Disposition","attachment; filename=payroll-page.csv").body(service.payrollCsv(from,through,offset,limit));}
    @GetMapping("/payroll/{id}/reversals") public List<WorkforceService.Reversal> reversals(@PathVariable UUID id){return service.reversals(id);}
    @GetMapping("/payroll/{id}/payments") public List<WorkforceService.Payment> payments(@PathVariable UUID id){return service.payments(id);}
    private static void match(UUID supplied,UUID body){if(!supplied.equals(body))throw new IllegalArgumentException("Operation mismatch");}
}
