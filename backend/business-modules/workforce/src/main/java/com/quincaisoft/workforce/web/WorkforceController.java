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
    private static void match(UUID supplied,UUID body){if(!supplied.equals(body))throw new IllegalArgumentException("Operation mismatch");}
}
