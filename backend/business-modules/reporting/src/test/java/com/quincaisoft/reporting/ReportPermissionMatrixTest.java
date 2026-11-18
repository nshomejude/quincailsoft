package com.quincaisoft.reporting;

import com.quincaisoft.reporting.application.DefaultReportingService;
import com.quincaisoft.shared.ScopedTransactions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Service source-capability gate only; not HTTP, membership, RLS or database acceptance. */
class ReportPermissionMatrixTest {
 @TestFactory List<DynamicTest> everyReportRequiresItsOwnSourceCapability() {
  var catalog = new DefaultReportingService(null, null).catalog();
  var authorities = new TreeSet<String>(List.of("reports:read", "reports:export", "documents:read", "ROLE_ADMIN"));
  catalog.forEach(d -> authorities.add(d.permission()));
  var tests = new ArrayList<DynamicTest>();
  for (var definition : catalog) {
   var candidates = new ArrayList<String>();
   candidates.add(null); // absent authentication
   candidates.add(""); // authenticated, no authorities
   for (String authority : authorities) {
    candidates.add(authority);
    candidates.add("SCOPE_" + authority);
   }
   for (String authority : candidates) tests.add(DynamicTest.dynamicTest(
       definition.code() + " with " + authority, () -> {
    var tx = mock(ScopedTransactions.class);
    var reachedTransaction = new IllegalStateException("test transaction boundary");
    when(tx.execute(any())).thenThrow(reachedTransaction);
    var service = new DefaultReportingService(null, tx);
    SecurityContextHolder.clearContext();
    try {
     if (authority != null) SecurityContextHolder.getContext().setAuthentication(
         UsernamePasswordAuthenticationToken.authenticated("reviewer", null,
             authority.isEmpty() ? List.of() : List.of(new SimpleGrantedAuthority(authority))));
     LocalDate day = definition.requiresDateRange() ? LocalDate.of(2026, 11, 18) : null;
     boolean allowed = Objects.equals(authority, definition.permission())
         || Objects.equals(authority, "SCOPE_" + definition.permission());
     if (allowed) {
      assertSame(reachedTransaction, assertThrows(IllegalStateException.class,
          () -> service.run(definition.code(), day, day, 0, 1)));
      verify(tx).execute(any());
     } else {
      assertThrows(AccessDeniedException.class, () -> service.run(definition.code(), day, day, 0, 1));
      verifyNoInteractions(tx);
     }
    } finally { SecurityContextHolder.clearContext(); }
   }));
  }
  assertFalse(tests.isEmpty(), "Report permission suite must not silently become empty");
  return tests;
 }
}
