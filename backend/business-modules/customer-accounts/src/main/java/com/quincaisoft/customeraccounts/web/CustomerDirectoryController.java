package com.quincaisoft.customeraccounts.web;
import com.quincaisoft.customeraccounts.api.CustomerDirectory;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/customers")
public class CustomerDirectoryController {
 private final CustomerDirectory directory;
 public CustomerDirectoryController(CustomerDirectory directory){this.directory=directory;}
 @GetMapping("/page") @PreAuthorize("hasAnyAuthority('customers:read','SCOPE_customers:read')")
 public CustomerDirectory.Page page(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="25") int limit){return directory.page(q,offset,limit);}
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('customers:read','SCOPE_customers:read')")
 public CustomerDirectory.Entry get(@PathVariable UUID id){return directory.get(id);}
 @PostMapping @PreAuthorize("hasAnyAuthority('customers:write','SCOPE_customers:write')")
 public CustomerDirectory.Entry create(@RequestHeader("Idempotency-Key") UUID key,@RequestBody CustomerDirectory.Save body){match(key,body);return directory.create(body);}
 @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('customers:write','SCOPE_customers:write')")
 public CustomerDirectory.Entry update(@PathVariable UUID id,@RequestHeader("Idempotency-Key") UUID key,@RequestBody CustomerDirectory.Save body){match(key,body);return directory.update(id,body);}
 private void match(UUID key,CustomerDirectory.Save body){if(body==null||!key.equals(body.operationId()))throw new IllegalArgumentException("Operation ID mismatch");}
}
