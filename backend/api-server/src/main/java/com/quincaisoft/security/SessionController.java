package com.quincaisoft.security;
import com.quincaisoft.shared.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;
@RestController
public class SessionController {
 public record SessionView(TenantScope scope,List<String> authorities) {}
 private final ScopedTransactions transactions;
 public SessionController(ScopedTransactions transactions){this.transactions=transactions;}
 @GetMapping({"/api/v1/session","/api/v1/me"}) public SessionView session(Authentication auth){return transactions.execute(()->new SessionView(TenantContext.current(),auth.getAuthorities().stream().map(Object::toString).toList()));}
}
