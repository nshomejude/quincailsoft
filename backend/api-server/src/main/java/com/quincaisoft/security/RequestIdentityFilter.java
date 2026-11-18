package com.quincaisoft.security;
import jakarta.servlet.*;import jakarta.servlet.http.*;
import java.io.IOException;import java.util.UUID;
import org.springframework.web.filter.OncePerRequestFilter;
public final class RequestIdentityFilter extends OncePerRequestFilter {
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
  String supplied=request.getHeader("X-Request-ID");String id=UUID.randomUUID().toString();
  if(supplied!=null){try{id=UUID.fromString(supplied).toString();}catch(IllegalArgumentException e){request.setAttribute("correlationId",id);ApiErrors.write(request,response,400,"INVALID_REQUEST_ID","X-Request-ID must be a UUID");return;}}
  request.setAttribute("correlationId",id);response.setHeader("X-Request-ID",id);
  if(request.getRequestURI().startsWith("/api/")&&!java.util.Set.of("GET","HEAD","OPTIONS").contains(request.getMethod())&&supplied==null){ApiErrors.write(request,response,400,"REQUEST_ID_REQUIRED","X-Request-ID is required for commands");return;}
  chain.doFilter(request,response);
 }
}
