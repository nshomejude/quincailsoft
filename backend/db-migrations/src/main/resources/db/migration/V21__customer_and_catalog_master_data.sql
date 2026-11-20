ALTER TABLE customer ADD COLUMN customer_type text NOT NULL DEFAULT 'Individual' CHECK(customer_type IN ('Individual','Contractor','Company')),
 ADD COLUMN phone text NOT NULL DEFAULT '', ADD COLUMN email text NOT NULL DEFAULT '', ADD COLUMN address text NOT NULL DEFAULT '',
 ADD COLUMN tax_reference text NOT NULL DEFAULT '', ADD COLUMN credit_limit_xaf bigint NOT NULL DEFAULT 0 CHECK(credit_limit_xaf>=0),
 ADD COLUMN payment_terms_days integer NOT NULL DEFAULT 0 CHECK(payment_terms_days BETWEEN 0 AND 3650),
 ADD COLUMN consent boolean NOT NULL DEFAULT false, ADD COLUMN version bigint NOT NULL DEFAULT 0;
GRANT INSERT,UPDATE ON customer TO quincai_app;
CREATE FUNCTION protect_customer_master_update() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.tenant_id<>OLD.tenant_id OR NEW.company_id<>OLD.company_id OR NEW.id<>OLD.id THEN RAISE EXCEPTION 'Customer ownership is immutable' USING ERRCODE='23514'; END IF;
 IF nullif(current_setting('app.customer_operation',true),'') IS NULL AND
    (to_jsonb(NEW)-'lock_version') IS DISTINCT FROM (to_jsonb(OLD)-'lock_version') THEN
  RAISE EXCEPTION 'Customer changes require a scoped master-data command' USING ERRCODE='42501';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER customer_master_update BEFORE UPDATE ON customer FOR EACH ROW EXECUTE FUNCTION protect_customer_master_update();

CREATE TABLE catalog_category(tenant_id uuid NOT NULL,company_id uuid NOT NULL,id uuid NOT NULL,code text NOT NULL,name text NOT NULL,parent_id uuid,brand text NOT NULL DEFAULT '',description text NOT NULL DEFAULT '',active boolean NOT NULL DEFAULT true,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,code),CHECK(parent_id IS DISTINCT FROM id),FOREIGN KEY(tenant_id,company_id) REFERENCES company(tenant_id,id),FOREIGN KEY(tenant_id,company_id,parent_id) REFERENCES catalog_category(tenant_id,company_id,id));
CREATE TABLE catalog_unit(tenant_id uuid NOT NULL,company_id uuid NOT NULL,id uuid NOT NULL,code text NOT NULL,name text NOT NULL,base_unit text NOT NULL,factor_numerator bigint NOT NULL CHECK(factor_numerator>0),factor_denominator bigint NOT NULL CHECK(factor_denominator>0),decimal_precision integer NOT NULL CHECK(decimal_precision BETWEEN 0 AND 6),active boolean NOT NULL DEFAULT true,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,code),FOREIGN KEY(tenant_id,company_id) REFERENCES company(tenant_id,id));
ALTER TABLE catalog_category ENABLE ROW LEVEL SECURITY;ALTER TABLE catalog_category FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON catalog_category USING(tenant_id=app_tenant() AND company_id=app_company()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company());
ALTER TABLE catalog_unit ENABLE ROW LEVEL SECURITY;ALTER TABLE catalog_unit FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON catalog_unit USING(tenant_id=app_tenant() AND company_id=app_company()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company());
GRANT SELECT,INSERT,UPDATE ON catalog_category,catalog_unit TO quincai_app;
ALTER TABLE catalog_product ADD COLUMN category_id uuid,ADD COLUMN brand text NOT NULL DEFAULT '',ADD COLUMN barcode text NOT NULL DEFAULT '',
 ADD COLUMN pack_size_milli bigint NOT NULL DEFAULT 1000 CHECK(pack_size_milli>0),ADD COLUMN reorder_level_milli bigint NOT NULL DEFAULT 0 CHECK(reorder_level_milli>=0),
 ADD COLUMN track_stock boolean NOT NULL DEFAULT true,ADD COLUMN track_serial boolean NOT NULL DEFAULT false,ADD COLUMN track_expiry boolean NOT NULL DEFAULT false,ADD COLUMN description text NOT NULL DEFAULT '',
 ADD FOREIGN KEY(tenant_id,company_id,category_id) REFERENCES catalog_category(tenant_id,company_id,id);
CREATE UNIQUE INDEX product_barcode_unique ON catalog_product(tenant_id,company_id,barcode) WHERE barcode<>'';
