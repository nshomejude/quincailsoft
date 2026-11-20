package com.quincaisoft.identity;
import com.quincaisoft.identity.api.*;import com.quincaisoft.identity.application.*;import com.quincaisoft.audit.application.*;import com.quincaisoft.shared.*;
import jakarta.persistence.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;import org.springframework.jdbc.datasource.DriverManagerDataSource;import org.springframework.orm.jpa.*;import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;import org.springframework.transaction.support.TransactionTemplate;import org.springframework.security.access.AccessDeniedException;
import java.sql.*;import java.nio.file.*;import java.util.*;import java.util.function.Supplier;import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL",matches=".+")
class IdentityAuthorizationPostgresTest {
static void sql(Connection c,String q)throws Exception{try(var s=c.createStatement()){s.execute(q);}}
static long count(Connection c,String q)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(q)){r.next();return r.getLong(1);}}
@Test void staleEpochAndForeignApprovalReplayCannotAdministerMembership()throws Exception{
String adminUrl=System.getenv("QUINCAI_PG_TEST_URL"),adminUser=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent"),db="identity_"+UUID.randomUUID().toString().replace("-",""),role="identity_role_"+UUID.randomUUID().toString().replace("-","");String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
UUID t=UUID.randomUUID(),c=UUID.randomUUID(),b=UUID.randomUUID(),a=UUID.randomUUID(),checker=UUID.randomUUID(),target=UUID.randomUUID(),event=UUID.randomUUID(),source=UUID.randomUUID();
try(var admin=DriverManager.getConnection(adminUrl,adminUser,"")){
sql(admin,"DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='quincai_app') THEN CREATE ROLE quincai_app NOLOGIN NOSUPERUSER NOBYPASSRLS;END IF;END $$");sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS INHERIT");sql(admin,"GRANT quincai_app TO "+role);sql(admin,"CREATE DATABASE "+db);LocalContainerEntityManagerFactoryBean factory=null;
try(var owner=DriverManager.getConnection(url,adminUser,"")){
try (var migrations = Files.list(Path.of("../../db-migrations/src/main/resources/db/migration"))) {
    var ordered = migrations.filter(file -> file.toString().endsWith(".sql"))
        .sorted(Comparator.comparingInt(file -> Integer.parseInt(file.getFileName().toString().split("__")[0].substring(1))))
        .toList();
    for (var file : ordered) sql(owner, Files.readString(file));
}
sql(owner,"INSERT INTO tenant(id,name)VALUES('"+t+"','Synthetic');INSERT INTO company(tenant_id,id,name)VALUES('"+t+"','"+c+"','Synthetic');INSERT INTO branch(tenant_id,company_id,id,name)VALUES('"+t+"','"+c+"','"+b+"','Synthetic');INSERT INTO tenant_memberships(tenant_id,company_id,branch_id,actor_id,role_code)VALUES('"+t+"','"+c+"','"+b+"','"+a+"','owner'),('"+t+"','"+c+"','"+b+"','"+checker+"','owner'),('"+t+"','"+c+"','"+b+"','"+target+"','cashier')");
var ds=new DriverManagerDataSource(url,role,"");factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());factory.setPackagesToScan("com.quincaisoft.identity.infrastructure","com.quincaisoft.audit.infrastructure");factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();var emf=factory.getObject();var em=SharedEntityManagerCreator.createSharedEntityManager(emf);var template=new TransactionTemplate(new JpaTransactionManager(emf));
ScopedTransactions tx=new ScopedTransactions(){public <T>T execute(Supplier<T> action){return template.execute(status->{var s=TenantContext.current();em.createNativeQuery("select set_config('app.tenant_id',:t,true),set_config('app.company_id',:c,true),set_config('app.branch_id',:b,true),set_config('app.actor_id',:a,true)").setParameter("t",s.tenantId().toString()).setParameter("c",s.companyId().toString()).setParameter("b",s.branchId().toString()).setParameter("a",s.actorId().toString()).getSingleResult();return action.get();});}};
var audit=new DefaultAuditService(em,tx);var identity=new DefaultIdentityService(em,tx,audit);var makerScope=new TenantScope(t,c,b,a);var checkerScope=new TenantScope(t,c,b,checker);UUID requestOperation=UUID.randomUUID(),approvalOperation=UUID.randomUUID();var request=new IdentityService.Request(requestOperation,target,"auditor",false,1,"Synthetic access termination");
var change=TenantContext.call(makerScope,1,()->{assertEquals(3,identity.members().size());var result=identity.request(request);assertEquals(result.id(),identity.request(request).id());assertThrows(AccessDeniedException.class,()->identity.approve(result.id(),approvalOperation,result.version()));return result;});
TenantContext.call(checkerScope,1,()->{var result=identity.approve(change.id(),approvalOperation,change.version());assertEquals("APPLIED",result.status());assertEquals(result.id(),identity.approve(result.id(),approvalOperation,change.version()).id());var member=identity.members().stream().filter(x->x.actorId().equals(target)).findFirst().orElseThrow();assertEquals(2,member.authorizationEpoch());assertFalse(member.active());assertEquals("auditor",member.role());return null;});
TenantContext.call(makerScope,1,()->{assertThrows(AccessDeniedException.class,()->identity.approve(change.id(),approvalOperation,change.version()));return null;});
assertEquals(2,count(owner,"select count(*) from audit_event"));
TenantContext.call(new TenantScope(t,c,b,target),2,()->{assertThrows(AccessDeniedException.class,identity::members);return null;});
// Revocation invalidates every identity entry point, even direct module calls.
var selfRevoke=new IdentityService.Revoke(UUID.randomUUID(),a,1,"Rotate compromised sessions");
TenantContext.call(makerScope,1,()->{assertEquals(2,identity.revoke(selfRevoke).authorizationEpoch());return null;});
TenantContext.call(makerScope,1,()->{
 assertThrows(AccessDeniedException.class,identity::current);
 assertThrows(AccessDeniedException.class,identity::members);
 assertThrows(AccessDeniedException.class,identity::changes);
 assertThrows(AccessDeniedException.class,()->identity.request(request));
 assertThrows(AccessDeniedException.class,()->identity.approve(change.id(),approvalOperation,change.version()));
 assertThrows(AccessDeniedException.class,()->identity.revoke(selfRevoke));
 return null;
});
// A newly authenticated epoch can retrieve the committed revocation receipt.
TenantContext.call(makerScope,2,()->{
 assertEquals(2,identity.current().authorizationEpoch());
 assertEquals(2,identity.revoke(selfRevoke).authorizationEpoch());
 assertThrows(AccessDeniedException.class,()->identity.approve(change.id(),approvalOperation,change.version()));
 return null;
});
assertEquals(1,count(owner,"select count(*) from identity_revocation"));
assertEquals(3,count(owner,"select count(*) from audit_event"));
assertEquals(1,count(owner,"select count(*) from outbox_event where event_type='AuthorizationEpochRevoked.v1'"));
TenantContext.call(new TenantScope(t,c,UUID.randomUUID(),checker),1,()->{assertThrows(NoSuchElementException.class,identity::members);return null;});

try(var runtime=DriverManager.getConnection(url,role,"")){sql(runtime,"select set_config('app.tenant_id','"+t+"',false),set_config('app.company_id','"+c+"',false),set_config('app.branch_id','"+b+"',false),set_config('app.actor_id','"+checker+"',false)");var denied=assertThrows(SQLException.class,()->sql(runtime,"UPDATE tenant_memberships SET role_code='owner'"));assertEquals("42501",denied.getSQLState());assertThrows(SQLException.class,()->sql(runtime,"DELETE FROM audit_event"));sql(runtime,"select set_config('app.branch_id','"+UUID.randomUUID()+"',false)");assertEquals(0,count(runtime,"select count(*) from identity_change"));}
}finally{if(factory!=null)factory.destroy();sql(admin,"DROP DATABASE "+db+" WITH(FORCE)");sql(admin,"DROP ROLE "+role);}}
}
}
