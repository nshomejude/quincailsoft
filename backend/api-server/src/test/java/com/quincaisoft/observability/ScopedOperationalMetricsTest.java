package com.quincaisoft.observability;

import com.quincaisoft.shared.ScopedTransactions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScopedOperationalMetricsTest {
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void refusesUnauthenticatedAndUnprivilegedBeforeOpeningDatabaseTransaction(){
  var jdbc=mock(JdbcTemplate.class);var tx=mock(ScopedTransactions.class);var collector=new ScopedOperationalMetrics(jdbc,tx,1000);
  assertThrows(AccessDeniedException.class,collector::metrics);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("actor","",List.of(new SimpleGrantedAuthority("catalog:read"))));
  assertThrows(AccessDeniedException.class,collector::metrics);
  verifyNoInteractions(jdbc,tx);
 }
 @Test void boundsDatabaseSample(){
  var jdbc=mock(JdbcTemplate.class);var tx=mock(ScopedTransactions.class);
  for(int limit:new int[]{-1,0,10001,Integer.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->new ScopedOperationalMetrics(jdbc,tx,limit));
  assertDoesNotThrow(()->new ScopedOperationalMetrics(jdbc,tx,1));
  assertDoesNotThrow(()->new ScopedOperationalMetrics(jdbc,tx,10000));
 }
}
