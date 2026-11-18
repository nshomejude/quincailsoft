package com.quincaisoft.workforce.application;

import com.quincaisoft.audit.api.AuditService;
import com.quincaisoft.finance.api.FinanceService;
import com.quincaisoft.shared.*;
import com.quincaisoft.workforce.api.WorkforceService;
import com.quincaisoft.workforce.infrastructure.*;
import jakarta.persistence.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.*;

@Service
public class DefaultWorkforceService implements WorkforceService {
    private final EntityManager em;
    private final ScopedTransactions tx;
    private final FinanceService finance;
    private final AuditService audit;
    private final JsonMapper json=JsonMapper.builder().build();
    public DefaultWorkforceService(EntityManager em, ScopedTransactions tx, FinanceService finance, AuditService audit) {
        this.em=em; this.tx=tx; this.finance=finance; this.audit=audit;
    }
    private void authorize(String capability) {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals(capability)||a.getAuthority().equals("SCOPE_"+capability)))
            throw new AccessDeniedException("Workforce capability required");
        var s=TenantContext.current();
        var roles=em.createNativeQuery("select role_code from tenant_memberships where tenant_id=:t and company_id=:c and branch_id=:b and actor_id=:a and active and authorization_epoch=:e")
            .setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("a",s.actorId()).setParameter("e",TenantContext.authorizationEpoch()).getResultList();
        if(roles.size()!=1 || !Set.of("owner","finance").contains(roles.getFirst())) throw new AccessDeniedException("Current owner or finance membership required");
    }
    public Employee saveEmployee(UUID id, EmployeeSave input) {
        Objects.requireNonNull(input,"Employee required"); text(input.employeeNumber(),80); text(input.name(),240); text(input.department(),120);text(input.jobTitle(),120);text(input.reason(),2000);
        if(input.email()==null || input.email().length()>254 || (!input.email().isEmpty()&&!input.email().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))throw new IllegalArgumentException("Valid email or empty string required");
        if(input.contractType()==null||!Set.of("PERMANENT","FIXED_TERM","CASUAL").contains(input.contractType()))throw new IllegalArgumentException("Contract type required");
        if(input.hiredOn()==null||input.leftOn()!=null&&input.leftOn().isBefore(input.hiredOn()))throw new IllegalArgumentException("Employment dates invalid");
        long gross=number(input.monthlyGrossXaf()),expected=number(input.version());
        return tx.execute(()->{authorize("workforce:employees:write");return once(input.operationId(),"EmployeeSaved",id,input,Employee.class,input.reason(),()->{
            var s=TenantContext.current(); WorkforceEmployee e;
            if(id==null){if(expected!=0)throw new RevisionConflict("New employee version must be zero");e=new WorkforceEmployee();e.tenantId=s.tenantId();e.companyId=s.companyId();e.branchId=s.branchId();e.id=UUID.randomUUID();}
            else {e=findEmployee(id,LockModeType.PESSIMISTIC_WRITE);if(e.version!=expected)throw new RevisionConflict("Employee changed");
                // Existing payroll/attendance evidence must stay inside the employment period.
                Number outside=(Number)em.createNativeQuery("select (select count(*) from workforce_attendance where employee_id=:id and (business_date<:start or (cast(:finish as date) is not null and business_date>cast(:finish as date)))) + (select count(*) from workforce_payroll where employee_id=:id and (period_end<:start or (cast(:finish as date) is not null and period_start>cast(:finish as date))))")
                    .setParameter("id",id).setParameter("start",input.hiredOn()).setParameter("finish",input.leftOn()==null?null:input.leftOn().toString()).getSingleResult();
                if(outside.longValue()!=0)throw new IllegalStateException("Employment dates would invalidate recorded evidence");
            }
            e.employeeNumber=input.employeeNumber();e.name=input.name();e.email=input.email();e.department=input.department();e.jobTitle=input.jobTitle();e.contractType=input.contractType();e.hiredOn=input.hiredOn();e.leftOn=input.leftOn();e.monthlyGrossXaf=gross;
            if(id==null)em.persist(e);em.flush();return view(e);
        },Employee::id);});
    }
    public List<Employee> employees(int offset,int limit){bounds(offset,limit);return tx.execute(()->{authorize("workforce:employees:read");return em.createQuery("select e from WorkforceEmployee e order by e.employeeNumber,e.id",WorkforceEmployee.class).setFirstResult(offset).setMaxResults(limit).getResultList().stream().map(this::view).toList();});}
    public Employee employee(UUID id){return tx.execute(()->{authorize("workforce:employees:read");return view(findEmployee(id,LockModeType.NONE));});}
    public Attendance recordAttendance(AttendanceSave input){
        Objects.requireNonNull(input,"Attendance required");text(input.reason(),2000);
        if(input.date()==null||input.employeeId()==null||input.status()==null||!Set.of("PRESENT","ABSENT","APPROVED_LEAVE").contains(input.status())||input.workedMinutes()<0||input.workedMinutes()>1440||!input.status().equals("PRESENT")&&input.workedMinutes()!=0)throw new IllegalArgumentException("Invalid attendance");
        return tx.execute(()->{authorize("workforce:attendance:write");return once(input.operationId(),"AttendanceRecorded",input.employeeId(),input,Attendance.class,input.reason(),()->{
            var employee=findEmployee(input.employeeId(),LockModeType.PESSIMISTIC_WRITE);employed(employee,input.date());var s=TenantContext.current();var row=new WorkforceAttendance();row.tenantId=s.tenantId();row.companyId=s.companyId();row.branchId=s.branchId();row.id=UUID.randomUUID();row.employeeId=employee.id;row.date=input.date();row.status=input.status();row.workedMinutes=input.workedMinutes();em.persist(row);em.flush();return view(row);
        },Attendance::id);});
    }
    public List<Attendance> attendance(UUID employeeId,LocalDate from,LocalDate through){range(from,through);return tx.execute(()->{authorize("workforce:employees:read");findEmployee(employeeId,LockModeType.NONE);return em.createQuery("select a from WorkforceAttendance a where a.employeeId=:id and a.date between :f and :t order by a.date,a.id",WorkforceAttendance.class).setParameter("id",employeeId).setParameter("f",from).setParameter("t",through).getResultList().stream().map(this::view).toList();});}
    public Payslip draftPayroll(PayrollDraft input){
        Objects.requireNonNull(input,"Payroll required");range(input.periodStart(),input.periodEnd());text(input.calculationPolicy(),240);text(input.reason(),2000);
        if(input.employeeId()==null||input.businessDate()==null)throw new IllegalArgumentException("Employee and business date required");
        long gross=number(input.grossXaf()),deduction=number(input.deductionXaf());if(gross==0||deduction>gross)throw new IllegalArgumentException("Payroll gross must be positive and deductions cannot exceed gross");
        return tx.execute(()->{authorize("workforce:payroll:write");return once(input.operationId(),"PayrollDrafted",input.employeeId(),input,Payslip.class,input.reason(),()->{
            var e=findEmployee(input.employeeId(),LockModeType.PESSIMISTIC_WRITE);
            if(input.periodEnd().isBefore(e.hiredOn)||e.leftOn!=null&&input.periodStart().isAfter(e.leftOn))throw new IllegalArgumentException("Payroll period does not overlap employment");
            Long overlaps=em.createQuery("select count(p) from WorkforcePayroll p where p.employeeId=:e and p.periodStart<=:end and p.periodEnd>=:start",Long.class).setParameter("e",e.id).setParameter("start",input.periodStart()).setParameter("end",input.periodEnd()).getSingleResult();
            if(overlaps!=0)throw new IllegalStateException("Payroll already exists for overlapping period");
            var s=TenantContext.current();var p=new WorkforcePayroll();p.tenantId=s.tenantId();p.companyId=s.companyId();p.branchId=s.branchId();p.id=UUID.randomUUID();p.employeeId=e.id;p.employeeName=e.name;p.employeeNumber=e.employeeNumber;p.periodStart=input.periodStart();p.periodEnd=input.periodEnd();p.businessDate=input.businessDate();p.grossXaf=gross;p.deductionXaf=deduction;p.netXaf=Math.subtractExact(gross,deduction);p.calculationPolicy=input.calculationPolicy();p.status="DRAFT";p.preparedBy=s.actorId();em.persist(p);em.flush();return view(p);
        },Payslip::id);});
    }
    public Payslip approvePayroll(UUID id,Approval input){
        Objects.requireNonNull(input,"Approval required");text(input.reason(),2000);long version=number(input.version());
        return tx.execute(()->{authorize("workforce:payroll:approve");return once(input.operationId(),"PayrollAccrued",id,input,Payslip.class,input.reason(),()->{
            var p=findPayroll(id,LockModeType.PESSIMISTIC_WRITE);if(p.version!=version||!p.status.equals("DRAFT"))throw new RevisionConflict("Payroll draft changed");
            if(p.preparedBy.equals(TenantContext.current().actorId()))throw new AccessDeniedException("Independent payroll reviewer required");
            var entries=new ArrayList<FinanceService.Entry>();entries.add(new FinanceService.Entry("PAYROLL_EXPENSE",p.grossXaf,0));if(p.deductionXaf>0)entries.add(new FinanceService.Entry("PAYROLL_WITHHOLDING",0,p.deductionXaf));if(p.netXaf>0)entries.add(new FinanceService.Entry("PAYROLL_PAYABLE",0,p.netXaf));
            p.journalId=finance.post(p.id,input.operationId(),p.businessDate,"PAYROLL_ACCRUAL",entries);p.status="ACCRUED_UNPAID";p.approvedBy=TenantContext.current().actorId();em.flush();return view(p);
        },Payslip::id);});
    }
    public Payslip payslip(UUID id){return tx.execute(()->{authorize("workforce:payroll:read");return view(findPayroll(id,LockModeType.NONE));});}
    public List<Payslip> payroll(LocalDate from,LocalDate through,int offset,int limit){range(from,through);bounds(offset,limit);return tx.execute(()->{authorize("workforce:payroll:read");return em.createQuery("select p from WorkforcePayroll p where p.businessDate between :f and :t order by p.businessDate,p.id",WorkforcePayroll.class).setParameter("f",from).setParameter("t",through).setFirstResult(offset).setMaxResults(limit).getResultList().stream().map(this::view).toList();});}
    private WorkforceEmployee findEmployee(UUID id,LockModeType lock){var s=TenantContext.current();var row=em.find(WorkforceEmployee.class,new WorkforceKey(s.tenantId(),s.companyId(),Objects.requireNonNull(id)),lock);if(row==null||!s.branchId().equals(row.branchId))throw new NoSuchElementException("Employee not found");return row;}
    private WorkforcePayroll findPayroll(UUID id,LockModeType lock){var s=TenantContext.current();var row=em.find(WorkforcePayroll.class,new WorkforceKey(s.tenantId(),s.companyId(),Objects.requireNonNull(id)),lock);if(row==null||!s.branchId().equals(row.branchId))throw new NoSuchElementException("Payslip not found");return row;}
    private Employee view(WorkforceEmployee e){return new Employee(e.id,e.employeeNumber,e.name,e.email,e.department,e.jobTitle,e.contractType,e.hiredOn,e.leftOn,Long.toString(e.monthlyGrossXaf),Long.toString(e.version));}
    private Attendance view(WorkforceAttendance a){return new Attendance(a.id,a.employeeId,a.date,a.status,a.workedMinutes);}
    private Payslip view(WorkforcePayroll p){return new Payslip(p.id,p.employeeId,p.employeeName,p.employeeNumber,p.periodStart,p.periodEnd,p.businessDate,Long.toString(p.grossXaf),Long.toString(p.deductionXaf),Long.toString(p.netXaf),p.calculationPolicy,p.status,p.preparedBy,p.approvedBy,p.journalId,Long.toString(p.version));}
    private <T>T once(UUID operation,String event,UUID target,Object command,Class<T> type,String reason,Supplier<T> action,Function<T,UUID> id){
        if(operation==null)throw new IllegalArgumentException("Operation required");var s=TenantContext.current();String hash=sha(event+"|"+target+"|"+s+"|"+json.writeValueAsString(command));
        em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k",s.tenantId()+":"+operation).getSingleResult();
        var rows=em.createNativeQuery("select request_hash,response_json::text from idempotency_result where tenant_id=:t and operation_id=:o").setParameter("t",s.tenantId()).setParameter("o",operation).getResultList();
        if(!rows.isEmpty()){var row=(Object[])rows.getFirst();if(!hash.equals(row[0])||row[1]==null)throw new IllegalStateException("Operation reused or result unavailable");return json.readValue((String)row[1],type);}
        T result=action.get();em.flush();audit.append(operation,event,"workforce",id.apply(result),reason);
        em.createNativeQuery("insert into idempotency_result(tenant_id,company_id,branch_id,operation_id,request_hash,source_type,source_id,response_json) values(:t,:c,:b,:o,:h,'WORKFORCE',:id,cast(:r as jsonb))").setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("o",operation).setParameter("h",hash).setParameter("id",id.apply(result)).setParameter("r",json.writeValueAsString(result)).executeUpdate();
        em.createNativeQuery("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload) values(:t,:id,:c,:b,:o,:e,'workforce',:a,cast(:p as jsonb))").setParameter("t",s.tenantId()).setParameter("id",UUID.randomUUID()).setParameter("c",s.companyId()).setParameter("b",s.branchId()).setParameter("o",operation).setParameter("e",event+".v1").setParameter("a",id.apply(result)).setParameter("p",json.writeValueAsString(Map.of("id",id.apply(result),"actorId",s.actorId()))).executeUpdate();
        return result;
    }
    private static void employed(WorkforceEmployee e,LocalDate date){if(date.isBefore(e.hiredOn)||e.leftOn!=null&&date.isAfter(e.leftOn))throw new IllegalArgumentException("Date outside employment");}
    private static void text(String s,int length){if(s==null||s.isBlank()||s.length()>length||s.indexOf('\0')>=0)throw new IllegalArgumentException("Required text outside bounds");}
    private static long number(String s){if(s==null||!s.matches("0|[1-9][0-9]*"))throw new IllegalArgumentException("Nonnegative exact integer string required");return Long.parseLong(s);}
    private static void range(LocalDate from,LocalDate through){if(from==null||through==null||through.isBefore(from)||through.isAfter(from.plusYears(1)))throw new IllegalArgumentException("Date range must be ordered and at most one year");}
    private static void bounds(int offset,int limit){if(offset<0||offset>100000||limit<1||limit>100)throw new IllegalArgumentException("Invalid pagination");}
    private static String sha(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
