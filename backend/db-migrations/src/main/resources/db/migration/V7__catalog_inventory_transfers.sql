CREATE TABLE inventory_transfer (
 tenant_id uuid NOT NULL, company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,
 origin_warehouse uuid NOT NULL,destination_warehouse uuid NOT NULL,product_id uuid NOT NULL,
 quantity_milli bigint NOT NULL CHECK(quantity_milli>0),received_milli bigint NOT NULL DEFAULT 0 CHECK(received_milli>=0 AND received_milli<=quantity_milli),
 value_xaf bigint NOT NULL DEFAULT 0 CHECK(value_xaf>=0),received_value_xaf bigint NOT NULL DEFAULT 0 CHECK(received_value_xaf>=0 AND received_value_xaf<=value_xaf),
 business_date date NOT NULL,status text NOT NULL CHECK(status IN('DRAFT','APPROVED','IN_TRANSIT','PARTIALLY_RECEIVED','RECEIVED')),
 reason text NOT NULL,created_by uuid NOT NULL,approved_by uuid,created_at timestamptz NOT NULL,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),CHECK(origin_warehouse<>destination_warehouse),CHECK(approved_by IS NULL OR approved_by<>created_by),
 CHECK(status<>'RECEIVED' OR (received_milli=quantity_milli AND received_value_xaf=value_xaf)),
 FOREIGN KEY(tenant_id,company_id,branch_id,origin_warehouse) REFERENCES warehouse(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id,destination_warehouse) REFERENCES warehouse(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,product_id) REFERENCES catalog_product(tenant_id,company_id,id)
);
ALTER TABLE inventory_transfer ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_transfer FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON inventory_transfer USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
GRANT SELECT,INSERT,UPDATE ON inventory_transfer TO quincai_app;
CREATE FUNCTION protect_transfer() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.status='RECEIVED' THEN RAISE EXCEPTION 'Received transfer is immutable'; END IF;
 IF OLD.status<>'DRAFT' AND (NEW.origin_warehouse<>OLD.origin_warehouse OR NEW.destination_warehouse<>OLD.destination_warehouse OR NEW.product_id<>OLD.product_id OR NEW.quantity_milli<>OLD.quantity_milli OR NEW.business_date<>OLD.business_date) THEN RAISE EXCEPTION 'Approved transfer terms are immutable'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER transfer_immutable BEFORE UPDATE ON inventory_transfer FOR EACH ROW EXECUTE FUNCTION protect_transfer();

CREATE TABLE inventory_job(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,warehouse_id uuid NOT NULL,source_product uuid NOT NULL,output_product uuid,
 kind text NOT NULL CHECK(kind IN('COUNT','CUT','REPACK')),status text NOT NULL CHECK(status IN('COUNTING','CAPTURED','DRAFT','APPROVED','POSTED')),
 source_quantity_milli bigint NOT NULL CHECK(source_quantity_milli>=0),output_quantity_milli bigint NOT NULL DEFAULT 0 CHECK(output_quantity_milli>=0),snapshot_version bigint NOT NULL DEFAULT 0,
 snapshot_value_xaf bigint NOT NULL DEFAULT 0 CHECK(snapshot_value_xaf>=0),posted_value_xaf bigint NOT NULL DEFAULT 0 CHECK(posted_value_xaf>=0),parent_length_mm bigint NOT NULL DEFAULT 0,output_length_mm bigint NOT NULL DEFAULT 0,
 business_date date NOT NULL,evidence_reference text NOT NULL,reason text NOT NULL,created_by uuid NOT NULL,captured_by uuid,approved_by uuid,created_at timestamptz NOT NULL,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),CHECK(approved_by IS NULL OR (approved_by<>created_by AND (captured_by IS NULL OR approved_by<>captured_by))),
 CHECK(kind='COUNT' OR (source_product<>output_product AND source_quantity_milli>0 AND output_quantity_milli>0)),
 FOREIGN KEY(tenant_id,company_id,branch_id,warehouse_id)REFERENCES warehouse(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,source_product)REFERENCES catalog_product(tenant_id,company_id,id),FOREIGN KEY(tenant_id,company_id,output_product)REFERENCES catalog_product(tenant_id,company_id,id)
);
ALTER TABLE inventory_job ENABLE ROW LEVEL SECURITY;ALTER TABLE inventory_job FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON inventory_job USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
GRANT SELECT,INSERT,UPDATE ON inventory_job TO quincai_app;
CREATE FUNCTION protect_inventory_job()RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.status='POSTED' THEN RAISE EXCEPTION 'Posted inventory job immutable';END IF;
 IF NEW.kind<>OLD.kind OR NEW.source_product<>OLD.source_product OR NEW.warehouse_id<>OLD.warehouse_id OR NEW.business_date<>OLD.business_date OR NEW.source_quantity_milli<>OLD.source_quantity_milli OR NEW.snapshot_version<>OLD.snapshot_version OR NEW.snapshot_value_xaf<>OLD.snapshot_value_xaf THEN RAISE EXCEPTION 'Job source snapshot immutable';END IF;
 IF OLD.status='APPROVED' AND (NEW.output_quantity_milli<>OLD.output_quantity_milli OR NEW.output_product IS DISTINCT FROM OLD.output_product OR NEW.evidence_reference<>OLD.evidence_reference)THEN RAISE EXCEPTION 'Approved inventory terms immutable';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER inventory_job_immutable BEFORE UPDATE ON inventory_job FOR EACH ROW EXECUTE FUNCTION protect_inventory_job();
