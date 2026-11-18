package com.quincaisoft.security;
import com.quincaisoft.shared.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
class SecurityBoundaryTest {
 @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
 @Test void tokenScopeIsLexicalAndHeadersCannotOverride()throws Exception{
  var tenant=UUID.randomUUID();var company=UUID.randomUUID();var branch=UUID.randomUUID();var actor=UUID.randomUUID();
  var jwt=Jwt.withTokenValue("fixture").header("alg","RS256").subject(actor.toString()).claim("tenant_id",tenant.toString()).claim("company_id",company.toString()).claim("branch_id",branch.toString()).claim("authorization_epoch",3).build();
  SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  var request=new MockHttpServletRequest();request.addHeader("X-Tenant-ID",UUID.randomUUID().toString());
  new TenantScopeFilter().doFilter(request,new MockHttpServletResponse(),(req,res)->{assertEquals(tenant,TenantContext.current().tenantId());assertEquals(3,TenantContext.authorizationEpoch());});
  assertThrows(IllegalStateException.class,TenantContext::current);
 }
 @Test void missingSignedScopeFailsClosed()throws Exception{
  SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("fixture").header("alg","RS256").subject(UUID.randomUUID().toString()).build()));
  var response=new MockHttpServletResponse();new TenantScopeFilter().doFilter(new MockHttpServletRequest(),response,(q,s)->fail("Must not reach domain"));assertEquals(403,response.getStatus());assertTrue(response.getContentAsString().contains("INVALID_SCOPE"));
 }
 @Test void writeRequiresRequestIdentity()throws Exception{
  var request=new MockHttpServletRequest("POST","/api/v1/sales");var response=new MockHttpServletResponse();new RequestIdentityFilter().doFilter(request,response,(q,s)->fail("Missing request ID"));assertEquals(400,response.getStatus());assertNotNull(response.getHeader("X-Request-ID"));
 }
 @Test void databaseRevocationIsCheckedBeforeDomain()throws Exception{
  final List<String> sqlCalls=new ArrayList<>();final List<Object[]> arguments=new ArrayList<>();
  var jdbc=new JdbcTemplate(){
   @Override public <T>T queryForObject(String sql,Class<T> type,Object... args){
    sqlCalls.add(sql);arguments.add(args);
    if(sql.startsWith("select set_config"))return type.cast(args[0]);
    if(sql.startsWith("select count(*) from tenant_memberships"))return type.cast(Integer.valueOf(0));
    throw new AssertionError("Unexpected SQL: "+sql);
   }
  };
  final boolean[] rolledBack={false};
  var manager=new PlatformTransactionManager(){
   public TransactionStatus getTransaction(TransactionDefinition definition){return new SimpleTransactionStatus();}
   public void commit(TransactionStatus status){fail("Revoked identity must never commit");}
   public void rollback(TransactionStatus status){rolledBack[0]=true;}
  };
  var service=new PostgresScopedTransactions(jdbc,manager);var scope=new TenantScope(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());
  TenantContext.call(scope,7,()->{assertThrows(AccessDeniedException.class,()->service.execute(()->{fail("Revoked member must not execute");return null;}));return null;});
  assertTrue(rolledBack[0]);assertEquals(5,sqlCalls.size());
  assertArrayEquals(new Object[]{scope.tenantId(),scope.companyId(),scope.branchId(),scope.actorId(),7L},arguments.get(4));
  assertTrue(sqlCalls.subList(0,4).stream().allMatch(sql->sql.contains(", true)")));

 }
}
