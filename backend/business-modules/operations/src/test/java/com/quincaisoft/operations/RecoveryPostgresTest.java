package com.quincaisoft.operations;
import com.quincaisoft.audit.application.DefaultAuditService;
import com.quincaisoft.operations.api.RecoveryService;
import com.quincaisoft.operations.application.DefaultRecoveryService;
import com.quincaisoft.shared.*;
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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class RecoveryPostgresTest {
 static void sql(Connection c,String sql)throws SQLException{try(var s=c.createStatement()){s.execute(sql);}}
 static long count(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void auth(){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test","unused",List.of("operations:recovery:read","operations:recovery:write","operations:recovery:approve").stream().map(SimpleGrantedAuthority::new).toList()));}
 static RecoveryService.Review review(String version){return new RecoveryService.Review(UUID.randomUUID(),version,"Independent incident review");}
 @Test void retainedReceiptReviewIsScopedFencedIndependentAndNeverApplied()throws Exception{
        String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
        String suffix=UUID.randomUUID().toString().replace("-",""),db="recovery_test_"+suffix,role="recovery_role_"+suffix;
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
                factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.operations.infrastructure","com.quincaisoft.audit.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
                var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
                ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var s=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",s.tenantId().toString()).setParameter("c",s.companyId().toString()).setParameter("b",s.branchId().toString()).setParameter("a",s.actorId().toString()).getSingleResult();return action.get();});}};

                var fence=new AtomicReference<>(new TenantLifecyclePolicy.State(tenant,"ACTIVE",0,null));
                TenantLifecyclePolicy lifecycle=new TenantLifecyclePolicy(){public State current(){return fence.get();}public void requireNewMutation(){throw new UnsupportedOperationException();}public State requireSuspended(UUID decision,long epoch){var state=current();if(!state.status().equals("SUSPENDED")||state.epoch()!=epoch||!Objects.equals(state.lastDecisionId(),decision))throw new IllegalStateException("Stale fence");return state;}};
                var service=new DefaultRecoveryService(em,tx,lifecycle,new DefaultAuditService(em,tx),tools.jackson.databind.json.JsonMapper.builder().build());auth();
                UUID retained=UUID.randomUUID(),source=UUID.randomUUID(),decision=UUID.randomUUID();
                sql(owner,"INSERT INTO idempotency_result(tenant_id,company_id,branch_id,operation_id,request_hash,source_type,source_id,response_json) VALUES('"+tenant+"','"+company+"','"+branch+"','"+retained+"','"+"a".repeat(64)+"','TEST_RETAINED','"+source+"','{\"amountXaf\":\"10\"}')");
                sql(owner,"INSERT INTO idempotency_result(tenant_id,company_id,branch_id,operation_id,request_hash,source_type,response_json) VALUES('"+tenant+"','"+company+"','"+otherBranch+"','"+UUID.randomUUID()+"','"+"b".repeat(64)+"','TEST_OTHER','{}')");
                var request=new RecoveryService.Request(UUID.randomUUID(),"INCIDENT-1",Instant.parse("2024-01-01T00:00:00Z"),"isolated://candidate","Preserve source and evidence","Plan review");
                var plan=TenantContext.call(scope,1,()->{var p=service.request(request);assertEquals(p,service.request(request));assertThrows(AccessDeniedException.class,()->service.approveScope(p.id(),review(p.version())));return p;});
                var collecting=TenantContext.call(checkerScope,1,()->service.approveScope(plan.id(),review(plan.version())));
                var page=TenantContext.call(scope,1,()->service.retainedReceipts(Instant.now(),null,1));assertEquals(1,page.receipts().size());assertFalse(page.hasMore());assertEquals(retained,page.receipts().getFirst().operationId());
                var batch=new RecoveryService.Batch(UUID.randomUUID(),collecting.version(),0,page.receipts(),"Imported isolated candidate evidence");
                RecoveryService.Plan uploaded;
                try(var pool=Executors.newFixedThreadPool(2)){var start=new CountDownLatch(1);Callable<RecoveryService.Plan> run=()->{auth();try{start.await();return TenantContext.call(scope,1,()->service.append(plan.id(),batch));}finally{SecurityContextHolder.clearContext();}};var a=pool.submit(run);var b=pool.submit(run);start.countDown();uploaded=a.get(30,TimeUnit.SECONDS);assertEquals(uploaded,b.get(30,TimeUnit.SECONDS));}
                assertEquals("1",uploaded.candidateReceipts());assertEquals(1,count(owner,"select count(*) from recovery_candidate_receipt"));
                var comparison=new RecoveryService.Compare(UUID.randomUUID(),uploaded.version(),decision,1,"c".repeat(64),"isolated-egress-evidence://1","Compare preserved receipts");
                TenantContext.call(scope,1,()->{assertThrows(IllegalStateException.class,()->service.compare(plan.id(),comparison));return null;});
                assertEquals(0,count(owner,"select count(*) from idempotency_result where operation_id='"+comparison.operationId()+"'"));
                fence.set(new TenantLifecyclePolicy.State(tenant,"SUSPENDED",1,decision));
                var compared=TenantContext.call(scope,1,()->service.compare(plan.id(),comparison));assertEquals("MATCH_REVIEW_REQUIRED",compared.status());assertEquals("1",compared.matchingReceipts());
                TenantContext.call(scope,1,()->{assertThrows(AccessDeniedException.class,()->service.approveReceiptReview(plan.id(),review(compared.version())));return null;});
                var approval=review(compared.version());var reviewed=TenantContext.call(checkerScope,1,()->service.approveReceiptReview(plan.id(),approval));assertEquals("RECEIPTS_REVIEWED",reviewed.status());assertFalse(reviewed.applied());assertFalse(reviewed.productionCutoverAllowed());
                fence.set(new TenantLifecyclePolicy.State(tenant,"ACTIVE",2,UUID.randomUUID()));
                TenantContext.call(checkerScope,1,()->{assertThrows(IllegalStateException.class,()->service.approveReceiptReview(plan.id(),approval));return null;});
                TenantContext.call(scope,2,()->{assertThrows(AccessDeniedException.class,()->service.get(plan.id()));return null;});
                TenantContext.call(new TenantScope(tenant,company,branch,cashier),1,()->{assertThrows(AccessDeniedException.class,()->service.get(plan.id()));return null;});
                try(var runtime=DriverManager.getConnection(url,role,"")){assertEquals(0,count(runtime,"select count(*) from recovery_plan"));}
                assertThrows(SQLException.class,()->sql(owner,"UPDATE recovery_candidate_receipt SET request_hash='"+"d".repeat(64)+"'"));
                assertThrows(SQLException.class,()->sql(owner,"DELETE FROM recovery_plan"));
                var second=TenantContext.call(scope,1,()->service.request(new RecoveryService.Request(UUID.randomUUID(),"INCIDENT-2",request.targetPoint(),"isolated://different","Preserve","Compare mismatch")));
                var secondCollect=TenantContext.call(checkerScope,1,()->service.approveScope(second.id(),review(second.version())));
                var badReceipt=new RecoveryService.Receipt(retained,"d".repeat(64),"TEST_RETAINED",source,page.receipts().getFirst().responseDigest());
                var badUpload=TenantContext.call(scope,1,()->service.append(second.id(),new RecoveryService.Batch(UUID.randomUUID(),secondCollect.version(),0,List.of(badReceipt),"Conflict evidence")));
                fence.set(new TenantLifecyclePolicy.State(tenant,"SUSPENDED",3,decision));
                var quarantined=TenantContext.call(scope,1,()->service.compare(second.id(),new RecoveryService.Compare(UUID.randomUUID(),badUpload.version(),decision,3,"e".repeat(64),"egress://2","Compare conflict")));
                assertEquals("DIFFERENCES_QUARANTINED",quarantined.status());
                TenantContext.call(checkerScope,1,()->{assertEquals("CONTENT_CONFLICT",service.differences(second.id(),null,10).getFirst().classification());assertThrows(IllegalStateException.class,()->service.approveReceiptReview(second.id(),review(quarantined.version())));return null;});
                assertEquals(0,count(owner,"select count(*) from journal_entry"));
            }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH (FORCE)");sql(admin,"DROP ROLE "+role);}
        }
    }
}
