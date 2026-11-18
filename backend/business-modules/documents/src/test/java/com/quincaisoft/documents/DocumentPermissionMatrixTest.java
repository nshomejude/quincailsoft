package com.quincaisoft.documents;

import com.quincaisoft.documents.api.DocumentService;
import com.quincaisoft.documents.application.DocumentSourceReader;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Source read/list gates, not HTTP, capture replay, membership or PostgreSQL acceptance. */
class DocumentPermissionMatrixTest {
 @TestFactory List<DynamicTest> everyTemplateDeniesUnrelatedCapabilitiesBeforeReadingSources() {
  var templates = new DocumentSourceReader(null, null, null).templates();
  var authorities = new TreeSet<String>(List.of("documents:read", "documents:write", "documents:print", "reports:read", "ROLE_ADMIN"));
  templates.forEach(t -> authorities.add(t.permission()));
  var tests = new ArrayList<DynamicTest>();
  for (var template : templates) {
   var candidates = new ArrayList<String>();
   candidates.add(null);
   candidates.add("");
   for (String authority : authorities) {
    candidates.add(authority);
    candidates.add("SCOPE_" + authority);
   }
   for (String authority : candidates) tests.add(DynamicTest.dynamicTest(
       template.id() + " with " + authority, () -> {
    var em = mock(EntityManager.class);
    var originals = mock(DocumentService.class);
    var reader = new DocumentSourceReader(em, originals, null);
    SecurityContextHolder.clearContext();
    try {
     if (authority != null) SecurityContextHolder.getContext().setAuthentication(
         UsernamePasswordAuthenticationToken.authenticated("reviewer", null,
             authority.isEmpty() ? List.of() : List.of(new SimpleGrantedAuthority(authority))));
     boolean allowed = Objects.equals(authority, template.permission())
         || Objects.equals(authority, "SCOPE_" + template.permission());
     if (allowed) assertDoesNotThrow(() -> reader.authorize(template.id()));
     else {
      assertThrows(AccessDeniedException.class, () -> reader.authorize(template.id()));
      assertThrows(AccessDeniedException.class, () -> reader.sources(template.id()));
      assertThrows(AccessDeniedException.class, () -> reader.read(template.id(), UUID.randomUUID()));
     }
     verifyNoInteractions(em, originals);
    } finally { SecurityContextHolder.clearContext(); }
   }));
  }
  assertFalse(tests.isEmpty(), "Document permission suite must not silently become empty");
  return tests;
 }
}
