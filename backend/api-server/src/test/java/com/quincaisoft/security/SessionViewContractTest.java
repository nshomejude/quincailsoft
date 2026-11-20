package com.quincaisoft.security;

import com.quincaisoft.shared.TenantScope;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class SessionViewContractTest {
 @Test void preservesSessionScopeAndAuthorityWireShape(){
  var tenant=UUID.randomUUID();var company=UUID.randomUUID();var branch=UUID.randomUUID();var actor=UUID.randomUUID();
  var scope=new TenantScope(tenant,company,branch,actor);
  var mapper=JsonMapper.builder().build();
  var json=mapper.readTree(mapper.writeValueAsString(new SessionController.SessionView(scope,List.of("SCOPE_support:read","SCOPE_sales:read"))));
  assertEquals(2,json.size());assertEquals(4,json.required("scope").size());
  assertEquals(tenant.toString(),json.required("scope").required("tenantId").asText());
  assertEquals(company.toString(),json.required("scope").required("companyId").asText());
  assertEquals(branch.toString(),json.required("scope").required("branchId").asText());
  assertEquals(actor.toString(),json.required("scope").required("actorId").asText());
  assertEquals(2,json.required("authorities").size());
  assertEquals("SCOPE_support:read",json.required("authorities").get(0).asText());
  assertEquals("SCOPE_sales:read",json.required("authorities").get(1).asText());
 }
}
