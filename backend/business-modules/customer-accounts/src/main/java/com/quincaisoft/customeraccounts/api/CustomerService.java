package com.quincaisoft.customeraccounts.api;
import java.util.*;
public interface CustomerService {record Customer(UUID id,String name,boolean active){} void requireEligible(UUID id); List<Customer> list();}
