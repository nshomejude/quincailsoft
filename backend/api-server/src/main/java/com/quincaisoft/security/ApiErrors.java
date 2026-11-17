package com.quincaisoft.security;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
public final class ApiErrors {
 private ApiErrors(){}
 static Map<String,Object> body(HttpServletRequest request,String code,String message,boolean retryable){
  var data=new LinkedHashMap<String,Object>();data.put("code",code);data.put("message",message);data.put("correlationId",request.getAttribute("correlationId"));data.put("fields",List.of());data.put("retryable",retryable);data.put("currentVersion",null);data.put("helpKey",code.toLowerCase(Locale.ROOT));return data;
 }
 static void write(HttpServletRequest request,HttpServletResponse response,int status,String code,String message)throws IOException {
  response.setStatus(status);response.setContentType("application/json");response.getWriter().write(JsonMapper.builder().build().writeValueAsString(body(request,code,message,false)));
 }
}
