# Finance migration boundary

The finance API recognizes a zero-tax immediate credit sale in one balanced journal: receivable/revenue and cost-of-goods/inventory. Integral XAF totals are overflow checked. It locks and requires exactly one open company financial period. Hibernate/JPA is the sole aggregate writer; PostgreSQL adds deferred balance constraints and immutable fact protection.

Accounts are explicit operational controls, not certified SYSCOHADA mappings. Payments, deposits, customer credit spending, supplier payables, return credits, refunds, statutory periods and fiscal/document issuance are not yet migrated. Unit tests cover balanced/unbalanced entries, invalid two-sided lines and arithmetic overflow; the sales PostgreSQL integration test exercises actual JPA journal posting and rollback.
