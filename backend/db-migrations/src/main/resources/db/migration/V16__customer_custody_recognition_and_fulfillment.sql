ALTER TABLE sale ADD COLUMN recognition_policy text NOT NULL DEFAULT 'HANDOVER' CHECK(recognition_policy IN('HANDOVER','SALE_WITH_CUSTOMER_CUSTODY'));
CREATE TABLE inventory_recognition_policy(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,
 mode text NOT NULL CHECK(mode IN('HANDOVER','SALE_WITH_CUSTOMER_CUSTODY')),effective_from date NOT NULL,contract_reference text NOT NULL,
 status text NOT NULL CHECK(status IN('DRAFT','APPROVED')),created_by uuid NOT NULL,approved_by uuid,created_at timestamptz NOT NULL,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),CHECK(approved_by IS NULL OR approved_by<>created_by),
 FOREIGN KEY(tenant_id,company_id,branch_id)REFERENCES branch(tenant_id,company_id,id));
CREATE UNIQUE INDEX recognition_policy_effective ON inventory_recognition_policy(tenant_id,company_id,effective_from) WHERE status='APPROVED';
CREATE TABLE customer_custody(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,sale_id uuid NOT NULL,customer_id uuid NOT NULL,
 warehouse_id uuid NOT NULL,product_id uuid NOT NULL,quantity_milli bigint NOT NULL CHECK(quantity_milli>0),collected_milli bigint NOT NULL DEFAULT 0 CHECK(collected_milli>=0 AND collected_milli<=quantity_milli),
 frozen_cost_xaf bigint NOT NULL CHECK(frozen_cost_xaf>=0),status text NOT NULL CHECK(status IN('PENDING','PARTIALLY_COLLECTED','COLLECTED')),operation_id uuid NOT NULL,created_at timestamptz NOT NULL,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,sale_id,product_id),CHECK((status='COLLECTED')=(collected_milli=quantity_milli)),
 FOREIGN KEY(tenant_id,company_id,sale_id)REFERENCES sale(tenant_id,company_id,id),FOREIGN KEY(tenant_id,company_id,customer_id)REFERENCES customer(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id,warehouse_id)REFERENCES warehouse(tenant_id,company_id,branch_id,id),FOREIGN KEY(tenant_id,company_id,product_id)REFERENCES catalog_product(tenant_id,company_id,id));
CREATE TABLE custody_collection(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,custody_id uuid NOT NULL,operation_id uuid NOT NULL,
 quantity_milli bigint NOT NULL CHECK(quantity_milli>0),proof_reference text NOT NULL CHECK(length(trim(proof_reference))>0),actor_id uuid NOT NULL,collected_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,operation_id),FOREIGN KEY(tenant_id,company_id,custody_id)REFERENCES customer_custody(tenant_id,company_id,id));
CREATE TABLE sale_fulfillment(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,sale_id uuid NOT NULL,customer_id uuid NOT NULL,
 status text NOT NULL CHECK(status IN('PENDING','DISPATCHED','COMPLETED')),proof_reference text,business_date date NOT NULL,created_by uuid NOT NULL,created_at timestamptz NOT NULL,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,sale_id),FOREIGN KEY(tenant_id,company_id,sale_id)REFERENCES sale(tenant_id,company_id,id),FOREIGN KEY(tenant_id,company_id,customer_id)REFERENCES customer(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id)REFERENCES branch(tenant_id,company_id,id));
ALTER TABLE inventory_recognition_policy ENABLE ROW LEVEL SECURITY;ALTER TABLE inventory_recognition_policy FORCE ROW LEVEL SECURITY;
CREATE POLICY company_isolation ON inventory_recognition_policy USING(tenant_id=app_tenant() AND company_id=app_company())WITH CHECK(tenant_id=app_tenant() AND company_id=app_company());
GRANT SELECT,INSERT,UPDATE ON inventory_recognition_policy TO quincai_app;
DO $$ DECLARE t text;BEGIN FOREACH t IN ARRAY ARRAY['customer_custody','custody_collection','sale_fulfillment']LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY scope_isolation ON %I USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())',t);
 EXECUTE format('GRANT SELECT,INSERT ON %I TO quincai_app',t);
END LOOP;END $$;
GRANT UPDATE ON customer_custody,sale_fulfillment TO quincai_app;
CREATE FUNCTION protect_custody_terms()RETURNS trigger LANGUAGE plpgsql AS $$BEGIN
 IF OLD.status='COLLECTED' OR NEW.sale_id<>OLD.sale_id OR NEW.customer_id<>OLD.customer_id OR NEW.product_id<>OLD.product_id OR NEW.warehouse_id<>OLD.warehouse_id OR NEW.quantity_milli<>OLD.quantity_milli OR NEW.frozen_cost_xaf<>OLD.frozen_cost_xaf OR NEW.collected_milli<OLD.collected_milli THEN RAISE EXCEPTION 'Recognized custody facts immutable';END IF;RETURN NEW;END $$;
CREATE TRIGGER custody_immutable BEFORE UPDATE ON customer_custody FOR EACH ROW EXECUTE FUNCTION protect_custody_terms();
CREATE FUNCTION protect_recognition_policy()RETURNS trigger LANGUAGE plpgsql AS $$BEGIN
 IF OLD.status='APPROVED' OR NEW.mode<>OLD.mode OR NEW.effective_from<>OLD.effective_from OR NEW.contract_reference<>OLD.contract_reference THEN RAISE EXCEPTION 'Recognition policy terms immutable';END IF;RETURN NEW;END $$;
CREATE TRIGGER recognition_policy_immutable BEFORE UPDATE ON inventory_recognition_policy FOR EACH ROW EXECUTE FUNCTION protect_recognition_policy();
CREATE FUNCTION protect_fulfillment()RETURNS trigger LANGUAGE plpgsql AS $$BEGIN
 IF OLD.status='COMPLETED' OR NEW.sale_id<>OLD.sale_id OR NEW.customer_id<>OLD.customer_id OR NEW.business_date<>OLD.business_date THEN RAISE EXCEPTION 'Fulfillment source immutable';END IF;RETURN NEW;END $$;
CREATE TRIGGER fulfillment_immutable BEFORE UPDATE ON sale_fulfillment FOR EACH ROW EXECUTE FUNCTION protect_fulfillment();
