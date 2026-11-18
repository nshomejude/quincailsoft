\set ON_ERROR_STOP on
\if :{?preview_only}
\else
  \set preview_only false
\endif

\if :preview_only
\else
  DO $$ BEGIN RAISE EXCEPTION 'Run this seed only on the disposable preview cluster with -v preview_only=1'; END $$;
\endif

BEGIN;
SET LOCAL TIME ZONE 'Africa/Douala';

DO $$
BEGIN
  IF current_database() <> 'quincaisoft'
     OR current_setting('server_version_num')::integer / 10000 <> 18
     OR NOT EXISTS (SELECT FROM pg_roles WHERE rolname = current_user AND rolsuper)
     OR to_regclass('public.catalog_unit') IS NULL THEN
    RAISE EXCEPTION 'Preview seed requires a migrated PostgreSQL 18 quincaisoft database and its isolated bootstrap administrator';
  END IF;
END $$;

\set tenant 'b9cacd3e-3ffd-49ec-a7a7-76ee6279d637'
\set company '99d8c254-0152-461c-a55b-2400e13ffc85'
\set branch '9d6b5a41-2a21-4b51-9be1-df350a2a63e6'
\set category '511d98b2-b754-4212-aa1a-40f73b29ec77'

INSERT INTO tenant (id, name)
VALUES (:'tenant', 'QuincaiSoft local preview')
ON CONFLICT DO NOTHING;

INSERT INTO company (tenant_id, id, name)
VALUES (:'tenant', :'company', 'Douala Hardware Preview')
ON CONFLICT DO NOTHING;

SELECT set_config('app.tenant_id', :'tenant', true),
       set_config('app.company_id', :'company', true),
       set_config('app.branch_id', :'branch', true),
       set_config('app.actor_id', '0543fde0-65cc-4cb4-af22-6afe2d604e25', true);

INSERT INTO branch (tenant_id, company_id, id, name)
VALUES (:'tenant', :'company', :'branch', 'Douala Preview Store')
ON CONFLICT DO NOTHING;

INSERT INTO tenant_memberships (tenant_id, company_id, branch_id, actor_id, role_code)
VALUES
  (:'tenant', :'company', :'branch', '0543fde0-65cc-4cb4-af22-6afe2d604e25', 'owner'),
  (:'tenant', :'company', :'branch', '1ca68eb9-1f85-4e05-b969-d63bf95cc3f8', 'cashier'),
  (:'tenant', :'company', :'branch', '5edc2790-9131-4654-92ba-adc60c05eb8a', 'auditor')
ON CONFLICT DO NOTHING;

INSERT INTO warehouse (tenant_id, company_id, branch_id, id, name)
VALUES (:'tenant', :'company', :'branch', '87a109a4-2a14-4703-a577-0c7475f6fa30', 'Preview Main Warehouse')
ON CONFLICT DO NOTHING;

INSERT INTO financial_period (tenant_id, company_id, id, start_date, end_date, status)
VALUES (
  :'tenant', :'company', 'daa8440a-090d-4fee-a800-acc957513c36',
  make_date(extract(year FROM current_date)::integer, 1, 1),
  make_date(extract(year FROM current_date)::integer, 12, 31), 'Open'
)
ON CONFLICT DO NOTHING;

INSERT INTO customer (tenant_id, company_id, id, name, customer_type, email, credit_limit_xaf)
VALUES
  (:'tenant', :'company', 'ec8ea452-986e-4fc9-8d8b-78f34a821e91', 'Preview Walk-in', 'Individual', '', 0),
  (:'tenant', :'company', '592870e6-fd01-46f6-a1dc-eca2111a5bd4', 'Preview Contracting', 'Contractor', 'contractor@preview.example.invalid', 250000)
ON CONFLICT DO NOTHING;

INSERT INTO supplier (tenant_id, company_id, id, code, name)
VALUES (:'tenant', :'company', 'a64523bd-cfc7-4b41-bdb7-679f0e124e05', 'PREVIEW-SUPPLIER', 'Preview Materials Supplier')
ON CONFLICT DO NOTHING;

INSERT INTO catalog_category (tenant_id, company_id, id, code, name)
VALUES (:'tenant', :'company', :'category', 'PREVIEW-MATERIALS', 'Construction Materials')
ON CONFLICT DO NOTHING;

INSERT INTO catalog_unit
  (tenant_id, company_id, id, code, name, base_unit, factor_numerator, factor_denominator, decimal_precision)
VALUES
  (:'tenant', :'company', '807d06dd-e223-4c11-b5ca-ec88e96367cd', 'piece', 'Piece', 'piece', 1, 1, 0),
  (:'tenant', :'company', 'b5c8e1b4-60aa-45bb-82f6-41bd4b9c6def', 'metre', 'Metre', 'metre', 1, 1, 3)
ON CONFLICT DO NOTHING;

INSERT INTO catalog_product
  (tenant_id, company_id, id, sku, name, base_unit, selling_price_xaf, category_id, reorder_level_milli, description)
VALUES
  (:'tenant', :'company', '3b7f7f88-b1e0-4874-b342-3da71e2a89ce', 'PREVIEW-CEMENT', 'Cement 50 kg', 'piece',
   4850, :'category', 10000, 'Synthetic 50 kg bag for the local preview'),
  (:'tenant', :'company', '856daf06-41dd-483c-aecb-1ad48703e4ff', 'PREVIEW-CABLE', 'Electrical Cable', 'metre',
   950, :'category', 25000, 'Synthetic cable sold by the metre')
ON CONFLICT DO NOTHING;

COMMIT;
