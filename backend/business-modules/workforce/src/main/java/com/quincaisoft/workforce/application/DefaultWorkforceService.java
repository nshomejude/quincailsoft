package com.quincaisoft.workforce.application;

import com.quincaisoft.audit.api.AuditService;
import com.quincaisoft.finance.api.FinanceService;
import com.quincaisoft.tenders.api.BankService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final BankService bank;
    private final JsonMapper json=JsonMapper.builder().build();
    public DefaultWorkforceService(EntityManager em, ScopedTransactions tx, FinanceService finance, AuditService audit) {
        this(em,tx,finance,audit,null);
    }
    @Autowired public DefaultWorkforceService(EntityManager em, ScopedTransactions tx, FinanceService finance, AuditService audit, BankService bank) {
        this.em=em; this.tx=tx; this.finance=finance; this.audit=audit; this.bank=bank;
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
                if(!view(e).monthlyGrossXaf().equals(input.monthlyGrossXaf())||!view(e).contractType().equals(input.contractType()))throw new IllegalArgumentException("Use effective-dated contract change for salary or contract changes");
                // Existing payroll/attendance evidence must stay inside the employment period.
                Number outside=(Number)em.createNativeQuery("select (select count(*) from workforce_attendance where employee_id=:id and (business_date<:start or (cast(:finish as date) is not null and business_date>cast(:finish as date)))) + (select count(*) from workforce_payroll where employee_id=:id and (period_end<:start or (cast(:finish as date) is not null and period_start>cast(:finish as date))))")
                    .setParameter("id",id).setParameter("start",input.hiredOn()).setParameter("finish",input.leftOn()==null?null:input.leftOn().toString()).getSingleResult();
                if(outside.longValue()!=0)throw new IllegalStateException("Employment dates would invalidate recorded evidence");
            }
            e.employeeNumber=input.employeeNumber();e.name=input.name();e.email=input.email();e.department=input.department();e.jobTitle=input.jobTitle();e.contractType=input.contractType();e.hiredOn=input.hiredOn();e.leftOn=input.leftOn();e.monthlyGrossXaf=gross;
            if(id==null){em.persist(e);em.flush();persistContract(e,input.hiredOn(),input.contractType(),gross,"INITIAL_EMPLOYMENT_RECORD");}em.flush();return view(e);
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
        Objects.requireNonNull(input,"Payroll required");
        long total=number(input.deductionXaf());
        var lines=total==0?List.<Deduction>of():List.of(new Deduction("REVIEWED_AGGREGATE","Reviewed aggregate deduction",input.deductionXaf(),input.calculationPolicy()));
        return draftDetailedInternal(new DetailedPayroll(input,lines,null),input);
    }
    public Payslip draftDetailedPayroll(DetailedPayroll detailed){ return draftDetailedInternal(detailed,detailed); }
    private Payslip draftDetailedInternal(DetailedPayroll detailed,Object material){
        Objects.requireNonNull(detailed,"Payroll detail required");var input=Objects.requireNonNull(detailed.payroll(),"Payroll required");
        if(detailed.deductions()==null||detailed.deductions().size()>50)throw new IllegalArgumentException("At most 50 deduction lines required");
        long total=0;var codes=new HashSet<String>();for(var line:detailed.deductions()){if(line==null)throw new IllegalArgumentException("Deduction required");text(line.code(),80);text(line.label(),240);text(line.policyReference(),240);long amount=number(line.amountXaf());if(amount==0||!codes.add(line.code()))throw new IllegalArgumentException("Positive unique deduction lines required");total=Math.addExact(total,amount);}
        if(total!=number(input.deductionXaf()))throw new IllegalArgumentException("Deduction lines must match total");
        Objects.requireNonNull(input,"Payroll required");range(input.periodStart(),input.periodEnd());text(input.calculationPolicy(),240);text(input.reason(),2000);
        if(input.employeeId()==null||input.businessDate()==null)throw new IllegalArgumentException("Employee and business date required");
        long gross=number(input.grossXaf()),deduction=number(input.deductionXaf());if(gross==0||deduction>gross)throw new IllegalArgumentException("Payroll gross must be positive and deductions cannot exceed gross");
        return tx.execute(()->{authorize("workforce:payroll:write");return once(input.operationId(),"PayrollDrafted",input.employeeId(),material,Payslip.class,input.reason(),()->{
            var e=findEmployee(input.employeeId(),LockModeType.PESSIMISTIC_WRITE);
            if(input.periodEnd().isBefore(e.hiredOn)||e.leftOn!=null&&input.periodStart().isAfter(e.leftOn))throw new IllegalArgumentException("Payroll period does not overlap employment");
            Long overlaps=em.createQuery("select count(p) from WorkforcePayroll p where p.employeeId=:e and p.periodStart<=:end and p.periodEnd>=:start and p.status not in ('CANCELLED','REVERSED')",Long.class).setParameter("e",e.id).setParameter("start",input.periodStart()).setParameter("end",input.periodEnd()).getSingleResult();
            if(overlaps!=0)throw new IllegalStateException("Payroll already exists for overlapping period");
            if(detailed.replacesPayslipId()!=null){var old=findPayroll(detailed.replacesPayslipId(),LockModeType.PESSIMISTIC_WRITE);if(!old.employeeId.equals(e.id)||!Set.of("CANCELLED","REVERSED").contains(old.status)||!old.periodStart.equals(input.periodStart())||!old.periodEnd.equals(input.periodEnd()))throw new IllegalArgumentException("Replacement must reference canceled/reversed payroll for same employee and period");}
            var s=TenantContext.current();var p=new WorkforcePayroll();p.tenantId=s.tenantId();p.companyId=s.companyId();p.branchId=s.branchId();p.id=UUID.randomUUID();p.employeeId=e.id;p.employeeName=e.name;p.employeeNumber=e.employeeNumber;p.periodStart=input.periodStart();p.periodEnd=input.periodEnd();p.businessDate=input.businessDate();p.grossXaf=gross;p.deductionXaf=deduction;p.netXaf=Math.subtractExact(gross,deduction);p.calculationPolicy=input.calculationPolicy();p.status="DRAFT";p.preparedBy=s.actorId();p.replacesPayrollId=detailed.replacesPayslipId();em.persist(p);em.flush();
            for(var d:detailed.deductions()){var row=new WorkforceDeduction();row.tenantId=s.tenantId();row.companyId=s.companyId();row.branchId=s.branchId();row.id=UUID.randomUUID();row.payrollId=p.id;row.code=d.code();row.label=d.label();row.amountXaf=number(d.amountXaf());row.policyReference=d.policyReference();em.persist(row);}em.flush();return view(p);
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
    public Contract changeContract(UUID employeeId,ContractChange input){
        Objects.requireNonNull(input,"Contract change required");text(input.reference(),240);text(input.reason(),2000);long salary=number(input.monthlyGrossXaf()),version=number(input.employeeVersion());
        if(input.effectiveFrom()==null||input.contractType()==null||!Set.of("PERMANENT","FIXED_TERM","CASUAL").contains(input.contractType()))throw new IllegalArgumentException("Effective date and contract type required");
        return tx.execute(()->{authorize("workforce:employees:write");return once(input.operationId(),"EmploymentContractChanged",employeeId,input,Contract.class,input.reason(),()->{
            var e=findEmployee(employeeId,LockModeType.PESSIMISTIC_WRITE);if(e.version!=version)throw new RevisionConflict("Employee changed");employed(e,input.effectiveFrom());
            var latest=em.createQuery("select c.effectiveFrom from WorkforceContract c where c.employeeId=:e order by c.effectiveFrom desc",LocalDate.class).setParameter("e",employeeId).setMaxResults(1).getResultList();
            if(!latest.isEmpty()&&!input.effectiveFrom().isAfter(latest.getFirst()))throw new IllegalArgumentException("Contract changes must advance effective history");
            long posted=em.createQuery("select count(p) from WorkforcePayroll p where p.employeeId=:e and p.periodEnd>=:d and p.status in ('ACCRUED_UNPAID','PAID_RECORDED')",Long.class).setParameter("e",employeeId).setParameter("d",input.effectiveFrom()).getSingleResult();
            if(posted!=0)throw new IllegalStateException("Reverse affected payroll before changing historical contract terms");
            var row=persistContract(e,input.effectiveFrom(),input.contractType(),salary,input.reference());em.lock(e,LockModeType.PESSIMISTIC_FORCE_INCREMENT);em.flush();return view(row);
        },Contract::id);});
    }
    private WorkforceContract persistContract(WorkforceEmployee e,LocalDate date,String type,long salary,String reference){
        var s=TenantContext.current();var c=new WorkforceContract();c.tenantId=s.tenantId();c.companyId=s.companyId();c.branchId=s.branchId();c.id=UUID.randomUUID();c.employeeId=e.id;c.effectiveFrom=date;c.contractType=type;c.monthlyGrossXaf=salary;c.reference=reference;c.recordedBy=s.actorId();em.persist(c);return c;
    }
    public List<Contract> contracts(UUID employeeId,int offset,int limit){bounds(offset,limit);return tx.execute(()->{authorize("workforce:employees:read");findEmployee(employeeId,LockModeType.NONE);return em.createQuery("select c from WorkforceContract c where c.employeeId=:e order by c.effectiveFrom,c.id",WorkforceContract.class).setParameter("e",employeeId).setFirstResult(offset).setMaxResults(limit).getResultList().stream().map(this::view).toList();});}
    public Contract contractAt(UUID employeeId,LocalDate date){Objects.requireNonNull(date,"Date required");return tx.execute(()->{authorize("workforce:employees:read");var e=findEmployee(employeeId,LockModeType.NONE);employed(e,date);var rows=em.createQuery("select c from WorkforceContract c where c.employeeId=:e and c.effectiveFrom<=:d order by c.effectiveFrom desc",WorkforceContract.class).setParameter("e",employeeId).setParameter("d",date).setMaxResults(1).getResultList();if(rows.isEmpty())throw new NoSuchElementException("No contract history recorded for date");return view(rows.getFirst());});}
    private Contract view(WorkforceContract c){return new Contract(c.id,c.employeeId,c.effectiveFrom,c.contractType,Long.toString(c.monthlyGrossXaf),c.reference,c.recordedBy);}
    public List<Deduction> deductions(UUID id){return tx.execute(()->{authorize("workforce:payroll:read");findPayroll(id,LockModeType.NONE);return deductionRows(id);});}
    private List<Deduction> deductionRows(UUID id){return em.createQuery("select d from WorkforceDeduction d where d.payrollId=:id order by d.code,d.id",WorkforceDeduction.class).setParameter("id",id).getResultList().stream().map(d->new Deduction(d.code,d.label,Long.toString(d.amountXaf),d.policyReference)).toList();}
    public Payslip cancelPayroll(UUID id,Approval input){Objects.requireNonNull(input,"Cancellation required");text(input.reason(),2000);long version=number(input.version());return tx.execute(()->{authorize("workforce:payroll:write");return once(input.operationId(),"PayrollCancelled",id,input,Payslip.class,input.reason(),()->{var p=findPayroll(id,LockModeType.PESSIMISTIC_WRITE);if(p.version!=version||!p.status.equals("DRAFT"))throw new RevisionConflict("Only a current draft can be canceled");p.status="CANCELLED";em.flush();return view(p);},Payslip::id);});}
    public Reversal requestReversal(UUID id,ReversalRequest input){Objects.requireNonNull(input,"Reversal request required");text(input.reason(),2000);long version=number(input.version());if(input.businessDate()==null)throw new IllegalArgumentException("Reversal business date required");return tx.execute(()->{authorize("workforce:payroll:write");return once(input.operationId(),"PayrollReversalRequested",id,input,Reversal.class,input.reason(),()->{
        var p=findPayroll(id,LockModeType.PESSIMISTIC_WRITE);if(!p.status.equals("ACCRUED_UNPAID")||p.version!=version)throw new RevisionConflict("Only current unpaid accrual can be reversed");if(input.businessDate().isBefore(p.businessDate))throw new IllegalArgumentException("Reversal cannot predate accrual");finance.requireOpenPeriod(input.businessDate());
        var s=TenantContext.current();var r=new WorkforceReversal();r.tenantId=s.tenantId();r.companyId=s.companyId();r.branchId=s.branchId();r.id=UUID.randomUUID();r.payrollId=id;r.businessDate=input.businessDate();r.status="REQUESTED";r.reason=input.reason();r.requestedBy=s.actorId();em.persist(r);em.flush();return view(r);
    },Reversal::id);});}
    public Reversal approveReversal(UUID id,Approval input){Objects.requireNonNull(input,"Reversal approval required");text(input.reason(),2000);long version=number(input.version());return tx.execute(()->{authorize("workforce:payroll:approve");return once(input.operationId(),"PayrollReversed",id,input,Reversal.class,input.reason(),()->{
        var s=TenantContext.current();var r=em.find(WorkforceReversal.class,new WorkforceKey(s.tenantId(),s.companyId(),id),LockModeType.PESSIMISTIC_WRITE);if(r==null)throw new NoSuchElementException("Reversal not found");if(r.version!=version||!r.status.equals("REQUESTED"))throw new RevisionConflict("Reversal changed");if(r.requestedBy.equals(s.actorId()))throw new AccessDeniedException("Independent reversal reviewer required");
        var p=findPayroll(r.payrollId,LockModeType.PESSIMISTIC_WRITE);if(!p.status.equals("ACCRUED_UNPAID"))throw new IllegalStateException("Payroll is no longer an unpaid accrual");
        var entries=new ArrayList<FinanceService.Entry>();entries.add(new FinanceService.Entry("PAYROLL_EXPENSE",0,p.grossXaf));if(p.deductionXaf>0)entries.add(new FinanceService.Entry("PAYROLL_WITHHOLDING",p.deductionXaf,0));if(p.netXaf>0)entries.add(new FinanceService.Entry("PAYROLL_PAYABLE",p.netXaf,0));
        r.journalId=finance.post(r.id,input.operationId(),r.businessDate,"PAYROLL_REVERSAL",entries);r.approvedBy=s.actorId();r.status="APPLIED";em.flush();p.status="REVERSED";em.flush();return view(r);
    },Reversal::id);});}
    private Reversal view(WorkforceReversal r){return new Reversal(r.id,r.payrollId,r.businessDate,r.status,r.requestedBy,r.approvedBy,r.journalId,Long.toString(r.version));}
    public Payment payBank(UUID id,BankPayment input){Objects.requireNonNull(input,"Payment required");text(input.reason(),2000);long version=number(input.version());if(input.businessDate()==null||input.bankEvidenceId()==null)throw new IllegalArgumentException("Date and approved bank evidence required");return tx.execute(()->{authorize("workforce:payroll:pay");return once(input.operationId(),"PayrollBankPaymentRecorded",id,input,Payment.class,input.reason(),()->{
        if(bank==null)throw new IllegalStateException("Bank evidence service unavailable");var p=findPayroll(id,LockModeType.PESSIMISTIC_WRITE);if(!p.status.equals("ACCRUED_UNPAID")||p.version!=version||p.netXaf<=0)throw new RevisionConflict("Current positive unpaid net payroll required");if(input.businessDate().isBefore(p.businessDate))throw new IllegalArgumentException("Payment cannot predate accrual");
        long pending=em.createQuery("select count(r) from WorkforceReversal r where r.payrollId=:id and r.status='REQUESTED'",Long.class).setParameter("id",id).getSingleResult();if(pending!=0)throw new IllegalStateException("Resolve payroll reversal before payment");
        var s=TenantContext.current();var payment=new WorkforcePayment();payment.tenantId=s.tenantId();payment.companyId=s.companyId();payment.branchId=s.branchId();payment.id=UUID.randomUUID();payment.payrollId=id;payment.businessDate=input.businessDate();payment.amountXaf=p.netXaf;payment.bankEvidenceId=input.bankEvidenceId();payment.recordedBy=s.actorId();
        bank.consumePayroll(input.bankEvidenceId(),payment.id,input.businessDate(),p.netXaf);payment.journalId=finance.post(payment.id,input.operationId(),input.businessDate(),"PAYROLL_BANK_PAYMENT",List.of(new FinanceService.Entry("PAYROLL_PAYABLE",p.netXaf,0),new FinanceService.Entry("BANK_CONTROL",0,p.netXaf)));em.persist(payment);em.flush();p.status="PAID_RECORDED";em.flush();return new Payment(payment.id,id,payment.businessDate,Long.toString(payment.amountXaf),payment.bankEvidenceId,payment.journalId,"PAID_RECORDED");
    },Payment::id);});}
    public List<Reversal> reversals(UUID id){return tx.execute(()->{authorize("workforce:payroll:read");findPayroll(id,LockModeType.NONE);return em.createQuery("select r from WorkforceReversal r where r.payrollId=:id order by r.id",WorkforceReversal.class).setParameter("id",id).getResultList().stream().map(this::view).toList();});}
    public List<Payment> payments(UUID id){return tx.execute(()->{authorize("workforce:payroll:read");findPayroll(id,LockModeType.NONE);return em.createQuery("select p from WorkforcePayment p where p.payrollId=:id order by p.id",WorkforcePayment.class).setParameter("id",id).getResultList().stream().map(p->new Payment(p.id,p.payrollId,p.businessDate,Long.toString(p.amountXaf),p.bankEvidenceId,p.journalId,"PAID_RECORDED")).toList();});}
    public String payslipHtml(UUID id){return tx.execute(()->{authorize("workforce:payroll:read");return WorkforceRendering.html(view(findPayroll(id,LockModeType.NONE)),deductionRows(id));});}
    public String payrollCsv(LocalDate from,LocalDate through,int offset,int limit){return WorkforceRendering.csv(payroll(from,through,offset,limit));}

    private WorkforceEmployee findEmployee(UUID id,LockModeType lock){var s=TenantContext.current();var row=em.find(WorkforceEmployee.class,new WorkforceKey(s.tenantId(),s.companyId(),Objects.requireNonNull(id)),lock);if(row==null||!s.branchId().equals(row.branchId))throw new NoSuchElementException("Employee not found");return row;}
    private WorkforcePayroll findPayroll(UUID id,LockModeType lock){var s=TenantContext.current();var row=em.find(WorkforcePayroll.class,new WorkforceKey(s.tenantId(),s.companyId(),Objects.requireNonNull(id)),lock);if(row==null||!s.branchId().equals(row.branchId))throw new NoSuchElementException("Payslip not found");return row;}
    private Employee view(WorkforceEmployee e){
        var terms=em.createQuery("select c from WorkforceContract c where c.employeeId=:e and c.effectiveFrom<=:d order by c.effectiveFrom desc",WorkforceContract.class).setParameter("e",e.id).setParameter("d",LocalDate.now(java.time.ZoneId.of("Africa/Douala"))).setMaxResults(1).getResultList();
        String type=terms.isEmpty()?e.contractType:terms.getFirst().contractType;long salary=terms.isEmpty()?e.monthlyGrossXaf:terms.getFirst().monthlyGrossXaf;
        return new Employee(e.id,e.employeeNumber,e.name,e.email,e.department,e.jobTitle,type,e.hiredOn,e.leftOn,Long.toString(salary),Long.toString(e.version));
    }
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
