package com.quincaisoft.finance;

import com.quincaisoft.company.application.DefaultCompanyService;
import com.quincaisoft.finance.api.*;
import com.quincaisoft.finance.application.*;
import com.quincaisoft.shared.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class FinanceReplayPostgresTest {
    static void sql(Connection c,String text)throws SQLException{try(var s=c.createStatement()){s.execute(text);}}
    static long count(Connection c,String text)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(text)){r.next();return r.getLong(1);}}
    static void historicalPeriodReceipt(Connection c,TenantScope scope,UUID operation,UUID source,String material,Object response,JsonMapper json)throws Exception {
        String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(material.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        try(var p=c.prepareStatement("insert into idempotency_result(tenant_id,operation_id,company_id,branch_id,request_hash,source_type,source_id,response_json)values(?,?,?,?,?,'PERIOD_COMMAND',?,cast(? as jsonb))")){
            p.setObject(1,scope.tenantId());p.setObject(2,operation);p.setObject(3,scope.companyId());p.setObject(4,scope.branchId());p.setString(5,hash);p.setObject(6,source);p.setString(7,json.writeValueAsString(response));p.executeUpdate();
        }
        try(var p=c.prepareStatement("insert into outbox_event(tenant_id,id,company_id,branch_id,operation_id,event_type,aggregate_type,aggregate_id,payload)values(?,?,?,?,?,'PeriodChanged.v1','period',?,cast(? as jsonb))")){
            p.setObject(1,scope.tenantId());p.setObject(2,UUID.randomUUID());p.setObject(3,scope.companyId());p.setObject(4,scope.branchId());p.setObject(5,operation);p.setObject(6,source);p.setString(7,json.writeValueAsString(response));p.executeUpdate();
        }
    }
    static ManualJournalService.Command command(long version){return new ManualJournalService.Command(UUID.randomUUID(),version,"Reviewed correction");}
    static List<ManualJournalService.Line> lines(){return List.of(new ManualJournalService.Line("CASH", "9007199254740993", "0"),new ManualJournalService.Line("CONTROL", "0", "9007199254740993"));}

    @Test void durableFinanceReplayBindsActorAndKeepsJournalInvariants()throws Exception {
        String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
        String suffix=UUID.randomUUID().toString().replace("-",""),db="finance_replay_"+suffix,role="finance_replay_"+suffix;
        String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
        UUID tenant=UUID.randomUUID(),company=UUID.randomUUID(),branch=UUID.randomUUID(),maker=UUID.randomUUID(),checker=UUID.randomUUID();
        var ownerScope=new TenantScope(tenant,company,branch,maker);var reviewerScope=new TenantScope(tenant,company,branch,checker);
        try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
            sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
            sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);
            LocalContainerEntityManagerFactoryBean factory=null;
            try(var owner=DriverManager.getConnection(url,adminUser,"")){
                try(var paths=Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))){for(var p:paths.filter(p->p.toString().endsWith(".sql")).sorted(Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1)))).toList())sql(owner,Files.readString(p));}
                sql(owner,"INSERT INTO tenant(id,name)VALUES('"+tenant+"','Synthetic');INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+company+"','Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+tenant+"','"+company+"','"+branch+"','Synthetic')");
                sql(owner,"INSERT INTO financial_period(tenant_id,company_id,id,start_date,end_date,status)VALUES('"+tenant+"','"+company+"','"+UUID.randomUUID()+"','2024-01-01','2024-12-31','Open'),('"+tenant+"','"+company+"','"+UUID.randomUUID()+"','2025-01-01','2025-12-31','Closed')");
                factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.finance.infrastructure","com.quincaisoft.company.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
                var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
                ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var s=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",s.tenantId().toString()).setParameter("c",s.companyId().toString()).setParameter("b",s.branchId().toString()).setParameter("a",s.actorId().toString()).getSingleResult();return action.get();});}};
                var json=JsonMapper.builder().build();var finance=new DefaultFinanceService(em,tx);
                var journals=new DefaultManualJournalService(em,tx,finance,json);
                var periods=new DefaultPeriodService(em,tx,new DefaultCompanyService(em,tx),List.of(new ManualJournalPeriodGuard(em)),json);
                var draftCommand=new ManualJournalService.Draft(UUID.randomUUID(),LocalDate.of(2024,6,1),"Synthetic correction",lines());
                ManualJournalService.Journal draft;
                try(var pool=Executors.newFixedThreadPool(2)){
                    var start=new CountDownLatch(1);Callable<ManualJournalService.Journal> call=()->{start.await();return TenantContext.call(ownerScope,1,()->journals.draft(draftCommand));};
                    var a=pool.submit(call);var b=pool.submit(call);start.countDown();draft=a.get(15,TimeUnit.SECONDS);assertEquals(draft,b.get(15,TimeUnit.SECONDS));
                }
                TenantContext.call(reviewerScope,1,()->{assertThrows(IllegalStateException.class,()->journals.draft(draftCommand));return null;});
                TenantContext.call(ownerScope,1,()->{assertThrows(IllegalStateException.class,()->journals.draft(new ManualJournalService.Draft(draftCommand.operationId(),draftCommand.businessDate(),"Changed command",lines())));return null;});
                var submitted=TenantContext.call(ownerScope,1,()->journals.command(draft.id(),"submit",command(draft.version())));
                TenantContext.call(ownerScope,1,()->{assertThrows(IllegalArgumentException.class,()->journals.command(draft.id(),"approve",command(submitted.version())));return null;});
                var approveCommand=command(submitted.version());var posted=TenantContext.call(reviewerScope,1,()->journals.command(draft.id(),"approve",approveCommand));
                assertEquals("POSTED",posted.status());
                TenantContext.call(reviewerScope,1,()->{assertEquals(posted,journals.command(draft.id(),"approve",approveCommand));return null;});
                TenantContext.call(ownerScope,1,()->{assertThrows(IllegalStateException.class,()->journals.command(draft.id(),"approve",approveCommand));return null;});
                var reversalCommand=new ManualJournalService.Reversal(UUID.randomUUID(),posted.version(),LocalDate.of(2024,6,2),"Reverse synthetic entry");
                var reversal=TenantContext.call(ownerScope,1,()->journals.reversal(posted.id(),reversalCommand));
                TenantContext.call(ownerScope,1,()->{assertEquals(reversal,journals.reversal(posted.id(),reversalCommand));assertThrows(RuntimeException.class,()->journals.reversal(posted.id(),new ManualJournalService.Reversal(UUID.randomUUID(),posted.version(),LocalDate.of(2024,6,2),"Duplicate reversal")));return null;});
                var reversalSubmitted=TenantContext.call(ownerScope,1,()->journals.command(reversal.id(),"submit",command(reversal.version())));
                TenantContext.call(reviewerScope,1,()->journals.command(reversal.id(),"approve",command(reversalSubmitted.version())));
                assertEquals(2,count(owner,"select count(*) from journal_entry"));
                assertEquals(0,count(owner,"select count(*) from (select account_code from journal_line group by account_code having sum(debit_xaf)<>sum(credit_xaf)) q"));
                assertThrows(SQLException.class,()->sql(owner,"UPDATE journal_line SET debit_xaf=1 WHERE debit_xaf>0"));
                var closedCommand=new ManualJournalService.Draft(UUID.randomUUID(),LocalDate.of(2025,6,1),"Closed period draft",lines());
                TenantContext.call(ownerScope,1,()->{assertThrows(IllegalStateException.class,()->journals.draft(closedCommand));return null;});
                assertEquals(0,count(owner,"select count(*) from idempotency_result where operation_id='"+closedCommand.operationId()+"'"));
                assertEquals(2,count(owner,"select count(*) from manual_journal"));
                TenantContext.call(new TenantScope(tenant,company,UUID.randomUUID(),maker),1,()->{assertThrows(NoSuchElementException.class,()->journals.get(posted.id()));return null;});
                TenantContext.call(new TenantScope(UUID.randomUUID(),company,branch,maker),1,()->{assertThrows(NoSuchElementException.class,()->journals.get(posted.id()));return null;});
                var createCommand=new PeriodService.Create(UUID.randomUUID(),LocalDate.of(2026,1,1),LocalDate.of(2026,12,31),"New reporting period");
                var period=TenantContext.call(ownerScope,1,()->periods.create(createCommand));
                TenantContext.call(ownerScope,1,()->{assertEquals(period,periods.create(createCommand));return null;});
                TenantContext.call(reviewerScope,1,()->{assertThrows(IllegalStateException.class,()->periods.create(createCommand));return null;});
                var requestCommand=new PeriodService.Command(UUID.randomUUID(),period.version(),"Review close request");
                var request=TenantContext.call(ownerScope,1,()->periods.request(period.id(),"CLOSE",requestCommand));
                TenantContext.call(reviewerScope,1,()->{assertThrows(IllegalStateException.class,()->periods.request(period.id(),"CLOSE",requestCommand));return null;});
                var oldRequest=new PeriodService.Command(UUID.randomUUID(),period.version(),"Historical close request");
                historicalPeriodReceipt(owner,ownerScope,oldRequest.operationId(),request.id(),"CLOSE|"+period.id()+"|"+oldRequest,request,json);
                TenantContext.call(ownerScope,1,()->{assertEquals(request,periods.request(period.id(),"CLOSE",oldRequest));return null;});
                TenantContext.call(reviewerScope,1,()->{assertThrows(IllegalStateException.class,()->periods.request(period.id(),"CLOSE",oldRequest));return null;});
                var oldCreate=new PeriodService.Create(UUID.randomUUID(),createCommand.startDate(),createCommand.endDate(),"Historical create without actor");
                historicalPeriodReceipt(owner,ownerScope,oldCreate.operationId(),period.id(),"CREATE|"+oldCreate,period,json);
                TenantContext.call(ownerScope,1,()->{assertThrows(IllegalStateException.class,()->periods.create(oldCreate));return null;});
                var rejectCommand=new PeriodService.Command(UUID.randomUUID(),request.version(),"Reject pending close");
                var rejected=TenantContext.call(reviewerScope,1,()->periods.decide(request.id(),false,rejectCommand));
                TenantContext.call(reviewerScope,1,()->{assertEquals(rejected,periods.decide(request.id(),false,rejectCommand));return null;});
                TenantContext.call(ownerScope,1,()->{assertThrows(IllegalStateException.class,()->periods.decide(request.id(),false,rejectCommand));return null;});
            }finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH(FORCE)");sql(admin,"DROP ROLE "+role);}
        }
    }
}
