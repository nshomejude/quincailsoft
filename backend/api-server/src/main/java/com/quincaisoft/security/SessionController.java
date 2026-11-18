package com.quincaisoft.security;
import com.quincaisoft.shared.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Map;
@RestController
public class SessionController {
 private final ScopedTransactions transactions;
 public SessionController(ScopedTransactions transactions){this.transactions=transactions;}
 @GetMapping({"/api/v1/session","/api/v1/me"}) public Map<String,Object> session(Authentication auth){return transactions.execute(()->Map.of("scope",TenantContext.current(),"authorities",auth.getAuthorities().stream().map(Object::toString).toList()));}
}
