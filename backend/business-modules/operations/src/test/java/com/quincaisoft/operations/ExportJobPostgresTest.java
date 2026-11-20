package com.quincaisoft.operations;
import com.quincaisoft.audit.application.DefaultAuditService;
import com.quincaisoft.operations.api.OperationsService;
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
class ExportJobPostgresTest {
 static void sql(Connection c,String sql)throws SQLException{try(var s=c.createStatement()){s.execute(sql);}}
 static long count(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void auth(String... permissions){var values=permissions.length==0?List.of("operations:export","catalog:read","suppliers:read"):List.of(permissions);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test","unused",values.stream().map(SimpleGrantedAuthority::new).toList()));}

 @Test void producerDownloadRevalidatePermissionsAndPreserveBoundedIsolation()throws Exception{
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
                factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(new DriverManagerDataSource(url,role,""));factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.operations.infrastructure","com.quincaisoft.audit.infrastructure","com.quincaisoft.identity.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
                var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
                ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var s=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",s.tenantId().toString()).setParameter("c",s.companyId().toString()).setParameter("b",s.branchId().toString()).setParameter("a",s.actorId().toString()).getSingleResult();return action.get();});}};


                TenantLifecyclePolicy lifecycle=new TenantLifecyclePolicy(){public State current(){return new State(tenant,"ACTIVE",0,null);}public void requireNewMutation(){}public State requireSuspended(UUID id,long epoch){throw new UnsupportedOperationException();}};
                var json=tools.jackson.databind.json.JsonMapper.builder().build();
                var service=new com.quincaisoft.operations.application.DefaultExportService(em,tx,lifecycle,new com.quincaisoft.operations.application.OperationalReads(em,json),new DefaultAuditService(em,tx),json);auth();
                UUID category=UUID.randomUUID(),otherCompany=UUID.randomUUID();
                sql(owner,"INSERT INTO catalog_category(tenant_id,company_id,id,code,name)VALUES('"+tenant+"','"+company+"','"+category+"','FORMULA','=SUM(1,2)')");
                sql(owner,"INSERT INTO company(tenant_id,id,name)VALUES('"+tenant+"','"+otherCompany+"','Other company'); INSERT INTO catalog_category(tenant_id,company_id,id,code,name)VALUES('"+tenant+"','"+otherCompany+"','"+UUID.randomUUID()+"','SECRET','OTHER COMPANY SECRET')");
                var request=new OperationsService.ExportRequest(UUID.randomUUID(),OperationsService.Entity.CATEGORY,"Authorized category export");
                var queued=TenantContext.call(scope,1,()->{var q=service.request(request);assertEquals(q,service.request(request));assertThrows(IllegalStateException.class,()->service.request(new OperationsService.ExportRequest(request.operationId(),OperationsService.Entity.UNIT,"Changed request")));assertThrows(IllegalStateException.class,()->service.download(q.id()));return q;});
                TenantContext.call(checkerScope,1,()->{assertThrows(NoSuchElementException.class,()->service.get(queued.id()));return null;});
                TenantContext.call(new TenantScope(tenant,company,branch,cashier),1,()->{assertThrows(AccessDeniedException.class,()->service.get(queued.id()));return null;});
                TenantContext.call(new TenantScope(tenant,company,otherBranch,maker),1,()->{assertThrows(AccessDeniedException.class,()->service.get(queued.id()));return null;});
                TenantContext.call(scope,1,()->{assertThrows(AccessDeniedException.class,()->service.request(new OperationsService.ExportRequest(UUID.randomUUID(),OperationsService.Entity.CUSTOMER,"Customer export denied without step-up")));assertThrows(jakarta.persistence.PersistenceException.class,()->service.request(new OperationsService.ExportRequest(UUID.randomUUID(),OperationsService.Entity.UNIT,"Second queued export")));return null;});
                auth("operations:export");TenantContext.call(scope,1,()->{assertThrows(AccessDeniedException.class,()->service.run(queued.id(),cmd(queued.version())));return null;});auth();
                var run=cmd(queued.version());OperationsService.Export ready;
                try(var pool=Executors.newFixedThreadPool(2)){
                  var start=new CountDownLatch(1);Callable<OperationsService.Export> work=()->{auth();try{assertTrue(start.await(5,TimeUnit.SECONDS));return TenantContext.call(scope,1,()->service.run(queued.id(),run));}finally{SecurityContextHolder.clearContext();}};
                  var first=pool.submit(work);var second=pool.submit(work);start.countDown();ready=first.get(10,TimeUnit.SECONDS);assertEquals(ready,second.get(10,TimeUnit.SECONDS));
                }
                assertEquals("READY",ready.status());assertEquals(1,ready.rowCount());
                var content=TenantContext.call(scope,1,()->service.download(ready.id()));assertTrue(content.content().contains("'=SUM(1,2)"));assertFalse(content.content().contains("OTHER COMPANY SECRET"));assertEquals(ready.digest(),com.quincaisoft.operations.infrastructure.OperationReceipts.digest(content.content()));
                TenantContext.call(scope,1,()->{assertEquals(ready,service.get(ready.id()));assertEquals(1,service.list().size());assertThrows(IllegalStateException.class,()->service.cancel(ready.id(),cmd(ready.version())));return null;});
                auth("operations:export");TenantContext.call(scope,1,()->{assertThrows(AccessDeniedException.class,()->service.download(ready.id()));return null;});auth();
                var identity=new com.quincaisoft.identity.application.DefaultIdentityService(em,tx,new DefaultAuditService(em,tx));
                TenantContext.call(scope,1,()->identity.revoke(new com.quincaisoft.identity.api.IdentityService.Revoke(UUID.randomUUID(),maker,1,"Revoke export authorization")));
                TenantContext.call(scope,1,()->{assertThrows(AccessDeniedException.class,()->service.download(ready.id()));return null;});
                TenantContext.call(scope,2,()->{assertThrows(AccessDeniedException.class,()->service.download(ready.id()));return null;});
                var cancelQueue=TenantContext.call(scope,2,()->service.request(new OperationsService.ExportRequest(UUID.randomUUID(),OperationsService.Entity.UNIT,"Cancel test")));
                TenantContext.call(scope,2,()->{var cancel=cmd(cancelQueue.version());var cancelled=service.cancel(cancelQueue.id(),cancel);assertEquals("CANCELLED",cancelled.status());assertEquals(cancelled,service.cancel(cancelQueue.id(),cancel));return null;});
                sql(owner,"INSERT INTO catalog_category(tenant_id,company_id,id,code,name) SELECT '"+tenant+"','"+company+"',gen_random_uuid(),'BULK-'||g,'Bounded' FROM generate_series(1,5000)g");
                var tooLarge=TenantContext.call(scope,2,()->service.request(new OperationsService.ExportRequest(UUID.randomUUID(),OperationsService.Entity.CATEGORY,"Bounded test")));
                TenantContext.call(scope,2,()->{var failed=service.run(tooLarge.id(),cmd(tooLarge.version()));assertEquals("FAILED",failed.status());assertEquals(0,failed.rowCount());assertThrows(IllegalStateException.class,()->service.download(failed.id()));return null;});
                sql(owner,"DELETE FROM catalog_category WHERE company_id='"+company+"' AND code LIKE 'BULK-%'; INSERT INTO catalog_category(tenant_id,company_id,id,code,name) SELECT '"+tenant+"','"+company+"',gen_random_uuid(),'UTF-'||g,repeat('é',4000) FROM generate_series(1,300)g");
                var utfLimit=TenantContext.call(scope,2,()->service.request(new OperationsService.ExportRequest(UUID.randomUUID(),OperationsService.Entity.CATEGORY,"UTF-8 byte bound test")));
                TenantContext.call(scope,2,()->{assertEquals("FAILED",service.run(utfLimit.id(),cmd(utfLimit.version())).status());return null;});
                UUID expired=UUID.randomUUID();sql(owner,"INSERT INTO operation_export_job(tenant_id,company_id,branch_id,id,actor_id,entity,status,row_count,digest,content,created_at,expires_at,authorization_epoch,failure)VALUES('"+tenant+"','"+company+"','"+branch+"','"+expired+"','"+maker+"','UNIT','READY',0,'digest','private',now()-interval '2 hours',now()-interval '1 hour',2,'')");
                TenantContext.call(scope,2,()->{assertThrows(IllegalStateException.class,()->service.download(expired));return null;});
                assertEquals(3,count(owner,"select count(*) from outbox_event where event_type='ExportCompleted.v1'"));
                assertEquals(0,count(owner,"select count(*) from operation_export_job where status='FAILED' and (content<>'' or row_count<>0)"));
                assertEquals(0,count(owner,"select count(*) from operation_export_job where status='QUEUED'"));
            }finally{SecurityContextHolder.clearContext();if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH (FORCE)");sql(admin,"DROP ROLE "+role);}
        }
 }
 static OperationsService.Command cmd(String version){return new OperationsService.Command(UUID.randomUUID(),version,"Export execution review");}
}
