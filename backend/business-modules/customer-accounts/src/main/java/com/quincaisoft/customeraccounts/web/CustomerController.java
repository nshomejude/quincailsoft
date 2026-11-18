package com.quincaisoft.customeraccounts.web;
import com.quincaisoft.customeraccounts.api.CustomerService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
@RestController @RequestMapping("/api/v1/customers")
public class CustomerController {
 private final CustomerService customers;
 public CustomerController(CustomerService customers){this.customers=customers;}
 @GetMapping @PreAuthorize("hasAnyAuthority('customers:read','SCOPE_customers:read','sales:write','SCOPE_sales:write','payments:write','SCOPE_payments:write','tenders:write','SCOPE_tenders:write','quotations:write','SCOPE_quotations:write')")
 public List<CustomerService.Customer> list(){return customers.list();}
}
