package com.quincaisoft.operations;
import com.quincaisoft.audit.application.DefaultAuditService;
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
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class ExportRetentionPostgresTest {
 static void sql(Connection c,String sql)throws SQLException{try(var s=c.createStatement()){s.execute(sql);}}
 static long count(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void auth(){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test","unused",List.of("operations:retention:read","operations:retention:write","operations:retention:approve").stream().map(SimpleGrantedAuthority::new).toList()));}
 @Test void expiryPreservesEvidenceRequiresReviewAndSerializesLegalHold()throws Exception{
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


                var suspended=new java.util.concurrent.atomic.AtomicBoolean();
                TenantLifecyclePolicy lifecycle=new TenantLifecyclePolicy(){public State current(){return new State(tenant,"ACTIVE",0,null);}public void requireNewMutation(){if(suspended.get())throw new AccessDeniedException("Suspended");}public State requireSuspended(UUID id,long epoch){throw new UnsupportedOperationException();}};
                var service=new com.quincaisoft.operations.application.DefaultExportRetentionService(em,tx,lifecycle,new DefaultAuditService(em,tx),tools.jackson.databind.json.JsonMapper.builder().build());auth();
                UUID export=UUID.randomUUID(),heldExport=UUID.randomUUID(),future=UUID.randomUUID();
                for(UUID id:List.of(export,heldExport,future))sql(owner,"INSERT INTO operation_export_job(tenant_id,company_id,branch_id,id,actor_id,entity,status,row_count,digest,content,created_at,expires_at,authorization_epoch,failure)VALUES('"+tenant+"','"+company+"','"+branch+"','"+id+"','"+maker+"','PRODUCT','READY',1,'"+"a".repeat(64)+"','sensitive payload',now()-interval '2 days',now()"+(id.equals(future)?"+interval '1 day'":"-interval '1 day'")+",1,'')");
                var request=new com.quincaisoft.operations.api.ExportRetentionService.Request(UUID.randomUUID(),export,"Expired operational payload");
                var pending=TenantContext.call(scope,1,()->{var result=service.request(request);assertEquals(result,service.request(request));assertThrows(AccessDeniedException.class,()->service.approve(result.id(),cmd(result.version())));assertThrows(IllegalStateException.class,()->service.request(new com.quincaisoft.operations.api.ExportRetentionService.Request(UUID.randomUUID(),future,"Not expired")));return result;});
                TenantContext.call(new TenantScope(tenant,company,otherBranch,maker),1,()->{assertThrows(AccessDeniedException.class,()->service.get(pending.id()));return null;});
                TenantContext.call(scope,1,()->{
                  assertThrows(AccessDeniedException.class,()->service.execute(pending.id(),cmd(pending.version())));
                  assertThrows(IllegalStateException.class,()->service.request(new com.quincaisoft.operations.api.ExportRetentionService.Request(request.operationId(),export,"Conflicting reason")));
                  return null;
                });
                TenantContext.call(new TenantScope(tenant,company,branch,cashier),1,()->{assertThrows(AccessDeniedException.class,()->service.get(pending.id()));return null;});
                TenantContext.call(scope,2,()->{assertThrows(AccessDeniedException.class,()->service.get(pending.id()));return null;});
                var approved=TenantContext.call(checkerScope,1,()->service.approve(pending.id(),cmd(pending.version())));
                TenantContext.call(checkerScope,1,()->{assertThrows(AccessDeniedException.class,()->service.execute(approved.id(),cmd(approved.version())));return null;});
                TenantContext.call(scope,1,()->{assertThrows(IllegalStateException.class,()->service.execute(approved.id(),cmd("99")));return null;});
                suspended.set(true);TenantContext.call(scope,1,()->{assertThrows(AccessDeniedException.class,()->service.execute(approved.id(),cmd(approved.version())));return null;});suspended.set(false);
                var execute=cmd(approved.version());var purged=TenantContext.call(scope,1,()->{var r=service.execute(approved.id(),execute);assertEquals(r,service.execute(approved.id(),execute));return r;});assertEquals("PURGED",purged.status());
                assertEquals(1,count(owner,"select count(*) from operation_export_job where id='"+export+"' and status='EXPIRED' and content='' and digest='"+"a".repeat(64)+"' and row_count=1"));
                assertThrows(SQLException.class,()->sql(owner,"UPDATE operation_export_job SET content='resurrected' WHERE id='"+export+"'"));
                assertThrows(SQLException.class,()->sql(owner,"DELETE FROM operation_export_retention"));
                var held=TenantContext.call(scope,1,()->service.request(new com.quincaisoft.operations.api.ExportRetentionService.Request(UUID.randomUUID(),heldExport,"Second expired payload")));
                var heldApproved=TenantContext.call(checkerScope,1,()->service.approve(held.id(),cmd(held.version())));
                UUID hold=UUID.randomUUID();sql(owner,"INSERT INTO platform_operator(actor_id,active)VALUES('"+maker+"',true),('"+checker+"',true)");
                owner.setAutoCommit(false);sql(owner,"SELECT pg_advisory_xact_lock(hashtextextended('tenant-lifecycle:"+tenant+"',0))");
                sql(owner,"SELECT set_config('app.platform_actor','"+maker+"',true)");
                sql(owner,"INSERT INTO platform_governance_request(id,tenant_id,kind,status,requested_by,expected_target_version,reference,reason,created_at)VALUES('"+hold+"','"+tenant+"','PLACE_LEGAL_HOLD','PENDING','"+maker+"',0,'CASE','Preserve evidence',now())");
                sql(owner,"SELECT set_config('app.platform_actor','"+checker+"',true)");
                sql(owner,"UPDATE platform_governance_request SET status='APPLIED',approved_by='"+checker+"',version=1 WHERE id='"+hold+"'");
                sql(owner,"INSERT INTO platform_legal_hold(id,tenant_id,reference,reason,status,placed_by,created_at)VALUES('"+hold+"','"+tenant+"','CASE','Preserve evidence','ACTIVE','"+checker+"',now())");
                var pool=Executors.newSingleThreadExecutor();try{
                  var started=new CountDownLatch(1);var futurePurge=pool.submit(()->{auth();try{started.countDown();return TenantContext.call(scope,1,()->service.execute(heldApproved.id(),cmd(heldApproved.version())));}finally{SecurityContextHolder.clearContext();}});
                  assertTrue(started.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->futurePurge.get(200,TimeUnit.MILLISECONDS));owner.commit();owner.setAutoCommit(true);
                  var denied=assertThrows(ExecutionException.class,()->futurePurge.get(10,TimeUnit.SECONDS));assertInstanceOf(IllegalStateException.class,denied.getCause());
                }finally{if(!owner.getAutoCommit()){owner.rollback();owner.setAutoCommit(true);}pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
                assertEquals(1,count(owner,"select count(*) from operation_export_job where id='"+heldExport+"' and content='sensitive payload'"));
                assertEquals(1,count(owner,"select count(*) from outbox_event where event_type='ExportPayloadPurged.v1'"));
            }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH (FORCE)");sql(admin,"DROP ROLE "+role);}
        }
 }
 static com.quincaisoft.operations.api.ExportRetentionService.Command cmd(String version){return new com.quincaisoft.operations.api.ExportRetentionService.Command(UUID.randomUUID(),version,"Independent retention review");}
}
