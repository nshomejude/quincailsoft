package com.quincaisoft.customeraccounts.api;

import java.util.List;
import java.util.UUID;

/** Scoped customer master data; balances remain owned by finance. */
public interface CustomerDirectory {
 record Save(UUID operationId,String name,String type,String phone,String email,String address,String taxReference,
             String creditLimitXaf,int paymentTermsDays,boolean consent,boolean active,String version,String reason) {}
 record Entry(UUID id,String name,String type,String phone,String email,String address,String taxReference,
              String creditLimitXaf,int paymentTermsDays,boolean consent,boolean active,String version) {}
 record Page(List<Entry> items,long total,int offset,int limit) {}
 Entry create(Save command);
 Entry update(UUID id,Save command);
 Entry get(UUID id);
 Page page(String query,int offset,int limit);
}
