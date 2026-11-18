package com.quincaisoft.security;
import com.quincaisoft.shared.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.io.IOException;
import java.util.UUID;
public final class TenantScopeFilter extends OncePerRequestFilter {
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  var authentication=SecurityContextHolder.getContext().getAuthentication();
  if(!(authentication instanceof JwtAuthenticationToken token)){chain.doFilter(request,response);return;}
  TenantScope scope;long epoch;
  try {var jwt=token.getToken();scope=new TenantScope(UUID.fromString(jwt.getClaimAsString("tenant_id")),UUID.fromString(jwt.getClaimAsString("company_id")),UUID.fromString(jwt.getClaimAsString("branch_id")),UUID.fromString(jwt.getSubject()));Object epochClaim=jwt.getClaim("authorization_epoch");epoch=Long.parseLong(String.valueOf(epochClaim));if(epoch<0)throw new IllegalArgumentException();}
  catch(RuntimeException error){ApiErrors.write(request,response,403,"INVALID_SCOPE","Valid tenant, company, branch, subject and authorization epoch required");return;}
  try{TenantContext.call(scope,epoch,()->{chain.doFilter(request,response);return null;});}
  catch(IOException|ServletException error){throw error;}
  catch(Exception error){throw new ServletException(error);}
 }
}
