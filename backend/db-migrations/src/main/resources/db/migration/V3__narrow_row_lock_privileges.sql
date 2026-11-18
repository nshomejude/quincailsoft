-- PostgreSQL row locks require UPDATE privilege on at least one column.
-- Grant only metadata rather than permitting runtime changes to eligibility or periods.
ALTER TABLE customer ADD COLUMN lock_version bigint NOT NULL DEFAULT 0 CHECK(lock_version>=0);
ALTER TABLE financial_period ADD COLUMN lock_version bigint NOT NULL DEFAULT 0 CHECK(lock_version>=0);
ALTER TABLE warehouse ADD COLUMN lock_version bigint NOT NULL DEFAULT 0 CHECK(lock_version>=0);
GRANT UPDATE(lock_version) ON customer,financial_period,warehouse TO quincai_app;
