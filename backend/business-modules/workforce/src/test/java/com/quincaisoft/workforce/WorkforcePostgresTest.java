package com.quincaisoft.workforce;

import com.quincaisoft.audit.application.DefaultAuditService;
import com.quincaisoft.finance.application.DefaultFinanceService;
import com.quincaisoft.shared.*;
import com.quincaisoft.workforce.api.WorkforceService;
import com.quincaisoft.workforce.application.DefaultWorkforceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class WorkforcePostgresTest {
    private static final List<String> PERMISSIONS=List.of("workforce:employees:write","workforce:employees:read","workforce:attendance:write","workforce:payroll:write","workforce:payroll:approve","workforce:payroll:read");
    private static final LocalDate START=LocalDate.of(2024,1,1),END=LocalDate.of(2024,1,31);
    static void sql(Connection c,String sql)throws SQLException {try(var s=c.createStatement()){s.execute(sql);}}
    static long count(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    static void auth(boolean permitted){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test","unused",permitted?PERMISSIONS.stream().map(SimpleGrantedAuthority::new).toList():List.of()));}
    static WorkforceService.EmployeeSave employee(UUID op,String name){return new WorkforceService.EmployeeSave(op,"EMP-001",name,"employee@example.invalid","Sales","Sales clerk","PERMANENT",START,null,"9007199254740993","0","Synthetic HR acceptance");}
    @Test void scopedPersonnelAttendanceAndIndependentPayrollAreAtomicAndReplaySafe()throws Exception {
        String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
        String suffix=UUID.randomUUID().toString().replace("-",""),db="workforce_test_"+suffix,role="workforce_role_"+suffix;
        String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
        UUID tenant=UUID.randomUUID(),company=UUID.randomUUID(),branch=UUID.randomUUID(),otherBranch=UUID.randomUUID(),maker=UUID.randomUUID(),checker=UUID.randomUUID(),cashier=UUID.randomUUID();
        var scope=new TenantScope(tenant,company,branch,maker);var checkerScope=new TenantScope(tenant,company,branch,checker);
        try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
            sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
            sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
            LocalContainerEntityManagerFactoryBean factory=null;
            try(var owner=DriverManager.getConnection(url,adminUser,"")){
                try(var files=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var path:files.filter(p->p.toString().endsWith(".sql")).sorted(Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(path));}
                sql(owner,"INSERT INTO tenant(id,name) VALUES('"+tenant+"','Test'); INSERT INTO company(tenant_id,id,name) VALUES('"+tenant+"','"+company+"','Test'); INSERT INTO branch(tenant_id,company_id,id,name) VALUES('"+tenant+"','"+company+"','"+branch+"','A'),('"+tenant+"','"+company+"','"+otherBranch+"','B')");
                for(var actor:List.of(maker,checker,cashier))sql(owner,"INSERT INTO tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code) VALUES('"+tenant+"','"+company+"','"+branch+"','"+actor+"','"+(actor.equals(cashier)?"cashier":"owner")+"')");
                sql(owner,"INSERT INTO financial_period(tenant_id,company_id,id,start_date,end_date,status) VALUES('"+tenant+"','"+company+"','"+UUID.randomUUID()+"','2024-01-01','2024-12-31','Open')");
                factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.workforce.infrastructure","com.quincaisoft.finance.infrastructure","com.quincaisoft.audit.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
                var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
                ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var s=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",s.tenantId().toString()).setParameter("c",s.companyId().toString()).setParameter("b",s.branchId().toString()).setParameter("a",s.actorId().toString()).getSingleResult();return action.get();});}};
                var service=new DefaultWorkforceService(em,tx,new DefaultFinanceService(em,tx),new DefaultAuditService(em,tx));
                var command=employee(UUID.randomUUID(),"Synthetic employee");WorkforceService.Employee created;
                try(var pool=Executors.newFixedThreadPool(2)){
                    var start=new CountDownLatch(1);Callable<WorkforceService.Employee> run=()->{auth(true);try{start.await();return TenantContext.call(scope,1,()->service.saveEmployee(null,command));}finally{SecurityContextHolder.clearContext();}};
                    var a=pool.submit(run);var b=pool.submit(run);start.countDown();created=a.get(30,TimeUnit.SECONDS);assertEquals(created,b.get(30,TimeUnit.SECONDS));
                }
                auth(true);var entry=created;
                var draft=TenantContext.call(scope,1,()->{
                    assertEquals("9007199254740993",entry.monthlyGrossXaf());assertEquals(1,service.employees(0,25).size());assertEquals(entry,service.employee(entry.id()));
                    assertThrows(IllegalStateException.class,()->service.saveEmployee(null,employee(command.operationId(),"Conflicting employee")));
                    var attendance=new WorkforceService.AttendanceSave(UUID.randomUUID(),entry.id(),START,"PRESENT",480,"Synthetic attendance");var recorded=service.recordAttendance(attendance);assertEquals(recorded,service.recordAttendance(attendance));
                    assertEquals(1,service.attendance(entry.id(),START,END).size());
                    assertThrows(IllegalArgumentException.class,()->service.recordAttendance(new WorkforceService.AttendanceSave(UUID.randomUUID(),entry.id(),START.minusDays(1),"PRESENT",480,"Before hire")));
                    assertThrows(IllegalArgumentException.class,()->service.recordAttendance(new WorkforceService.AttendanceSave(UUID.randomUUID(),entry.id(),START.plusDays(1),"ABSENT",480,"Invalid minutes")));
                    assertThrows(RuntimeException.class,()->service.recordAttendance(new WorkforceService.AttendanceSave(UUID.randomUUID(),entry.id(),START,"ABSENT",0,"Duplicate day")));
                    var pay=new WorkforceService.PayrollDraft(UUID.randomUUID(),entry.id(),START,END,END,"9007199254740993","450359962737049","REVIEWED_SYNTHETIC_NOT_STATUTORY","Payroll fixture");
                    var p=service.draftPayroll(pay);assertEquals("8556839292003944",p.netXaf());assertEquals(p,service.draftPayroll(pay));
                    assertThrows(IllegalStateException.class,()->service.draftPayroll(new WorkforceService.PayrollDraft(UUID.randomUUID(),entry.id(),START,END,END,"10","1","SYNTHETIC","Duplicate period")));
                    assertThrows(AccessDeniedException.class,()->service.approvePayroll(p.id(),new WorkforceService.Approval(UUID.randomUUID(),p.version(),"Self approve denied")));
                    return p;
                });
                assertEquals(0,count(owner,"SELECT count(*) FROM journal_entry"));
                TenantContext.call(new TenantScope(tenant,company,branch,cashier),1,()->{assertThrows(AccessDeniedException.class,()->service.employee(entry.id()));return null;});
                auth(false);TenantContext.call(scope,1,()->{assertThrows(AccessDeniedException.class,()->service.employee(entry.id()));return null;});auth(true);
                TenantContext.call(new TenantScope(tenant,company,otherBranch,maker),1,()->{assertThrows(AccessDeniedException.class,()->service.employee(entry.id()));return null;});
                TenantContext.call(new TenantScope(UUID.randomUUID(),company,branch,maker),1,()->{assertThrows(AccessDeniedException.class,()->service.employee(entry.id()));return null;});
                var approval=new WorkforceService.Approval(UUID.randomUUID(),draft.version(),"Independent payroll review");
                var accrued=TenantContext.call(checkerScope,1,()->{var p=service.approvePayroll(draft.id(),approval);assertEquals("ACCRUED_UNPAID",p.status());assertNotNull(p.journalId());assertEquals(p,service.approvePayroll(draft.id(),approval));assertEquals(p,service.payslip(p.id()));assertEquals(1,service.payroll(START,END,0,20).size());return p;});
                assertEquals(1,count(owner,"SELECT count(*) FROM journal_entry WHERE source_type='PAYROLL_ACCRUAL'"));
                assertEquals(9007199254740993L,count(owner,"SELECT sum(debit_xaf) FROM journal_line"));assertEquals(9007199254740993L,count(owner,"SELECT sum(credit_xaf) FROM journal_line"));
                assertEquals(1,count(owner,"SELECT count(*) FROM outbox_event WHERE event_type='PayrollAccrued.v1'"));
                // The absent financial period must roll back approval, journal, replay and audit atomically.
                var blocked=TenantContext.call(scope,1,()->service.draftPayroll(new WorkforceService.PayrollDraft(UUID.randomUUID(),entry.id(),LocalDate.of(2024,2,1),LocalDate.of(2024,2,29),LocalDate.of(2025,2,28),"1","0","SYNTHETIC","Future posting period missing")));
                var blockedApproval=new WorkforceService.Approval(UUID.randomUUID(),blocked.version(),"Missing period should roll back");
                TenantContext.call(checkerScope,1,()->{assertThrows(IllegalStateException.class,()->service.approvePayroll(blocked.id(),blockedApproval));assertEquals("DRAFT",service.payslip(blocked.id()).status());return null;});
                assertEquals(1,count(owner,"SELECT count(*) FROM journal_entry"));
                assertEquals(0,count(owner,"SELECT count(*) FROM idempotency_result WHERE operation_id='"+blockedApproval.operationId()+"'"));
                TenantContext.call(checkerScope,2,()->{assertThrows(AccessDeniedException.class,()->service.approvePayroll(draft.id(),approval));return null;});
                TenantContext.call(scope,1,()->{assertThrows(IllegalStateException.class,()->service.approvePayroll(draft.id(),approval));return null;});
                try(var runtime=DriverManager.getConnection(url,role,"")){
                    assertEquals(0,count(runtime,"SELECT count(*) FROM workforce_payroll"));
                    sql(runtime,"BEGIN; SELECT set_config('app.tenant_id','"+tenant+"',true),set_config('app.company_id','"+company+"',true),set_config('app.branch_id','"+branch+"',true)");
                    assertThrows(SQLException.class,()->sql(runtime,"UPDATE workforce_payroll SET gross_xaf=gross_xaf+1 WHERE id='"+accrued.id()+"'"));sql(runtime,"ROLLBACK");
                }
                assertEquals(2,count(owner,"SELECT count(*) FROM workforce_payroll"));
            }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH (FORCE)");sql(admin,"DROP ROLE "+role);}
        }
    }
}
