# M06 Java migration slice

Typed JPA catalog read API: GET /api/v1/products (first 200 sorted SKUs) and GET /api/v1/products/{id}, with OAuth scope catalog:read and tenant/company SQL predicates plus PostgreSQL RLS. Monetary and version JSON values are strings. Sales access CatalogService through its public module API. Product metadata writes, paging, unit conversion, price approvals, variants and search remain unmigrated. Catalog writes never change stock.
