ALTER TABLE stock_balance ADD COLUMN quarantined_milli bigint NOT NULL DEFAULT 0 CHECK(quarantined_milli>=0);
ALTER TABLE stock_balance ADD CONSTRAINT protected_stock_within_custody CHECK(reserved_milli::numeric+allocated_milli::numeric+quarantined_milli::numeric<=quantity_milli);
CREATE TABLE inventory_lot(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,
 warehouse_id uuid NOT NULL,product_id uuid NOT NULL,lot_number text NOT NULL,source_reference text NOT NULL,
 quantity_milli bigint NOT NULL CHECK(quantity_milli>=0),quarantined_milli bigint NOT NULL DEFAULT 0 CHECK(quarantined_milli>=0 AND quarantined_milli<=quantity_milli),
 expires_on date,created_by uuid NOT NULL,created_at timestamptz NOT NULL,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,product_id,lot_number),
 FOREIGN KEY(tenant_id,company_id,branch_id,warehouse_id)REFERENCES warehouse(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,product_id)REFERENCES catalog_product(tenant_id,company_id,id));
CREATE TABLE inventory_recall(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,lot_id uuid NOT NULL,
 quantity_milli bigint NOT NULL CHECK(quantity_milli>0),status text NOT NULL CHECK(status IN('DRAFT','APPROVED','QUARANTINED','RECOVERED','DISPOSED')),
 business_date date NOT NULL,reason text NOT NULL,evidence_reference text,created_by uuid NOT NULL,approved_by uuid,created_at timestamptz NOT NULL,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),CHECK(approved_by IS NULL OR approved_by<>created_by),
 FOREIGN KEY(tenant_id,company_id,lot_id)REFERENCES inventory_lot(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id)REFERENCES branch(tenant_id,company_id,id));
CREATE TABLE inventory_reservation(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,warehouse_id uuid NOT NULL,product_id uuid NOT NULL,
 quantity_milli bigint NOT NULL CHECK(quantity_milli>0),released_milli bigint NOT NULL DEFAULT 0 CHECK(released_milli>=0 AND released_milli<=quantity_milli),
 status text NOT NULL CHECK(status IN('ACTIVE','RELEASED')),source_reference text NOT NULL,reason text NOT NULL,created_by uuid NOT NULL,created_at timestamptz NOT NULL,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),CHECK((status='RELEASED')=(released_milli=quantity_milli)),
 FOREIGN KEY(tenant_id,company_id,branch_id,warehouse_id)REFERENCES warehouse(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,product_id)REFERENCES catalog_product(tenant_id,company_id,id));
DO $$ DECLARE t text;BEGIN FOREACH t IN ARRAY ARRAY['inventory_lot','inventory_recall','inventory_reservation'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY scope_isolation ON %I USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())',t);
 EXECUTE format('GRANT SELECT,INSERT,UPDATE ON %I TO quincai_app',t);
END LOOP;END $$;
CREATE FUNCTION protect_inventory_recall()RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.status IN('RECOVERED','DISPOSED')THEN RAISE EXCEPTION 'Final recall immutable';END IF;
 IF NEW.lot_id<>OLD.lot_id OR NEW.quantity_milli<>OLD.quantity_milli OR NEW.business_date<>OLD.business_date OR NEW.created_by<>OLD.created_by THEN RAISE EXCEPTION 'Recall source immutable';END IF;RETURN NEW;END $$;
CREATE TRIGGER recall_immutable BEFORE UPDATE ON inventory_recall FOR EACH ROW EXECUTE FUNCTION protect_inventory_recall();
