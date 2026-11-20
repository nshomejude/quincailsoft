DO $$ DECLARE r record; BEGIN
 FOR r IN SELECT conname FROM pg_constraint WHERE conrelid='quotation'::regclass AND contype='c' AND pg_get_constraintdef(oid) LIKE '%status%'
 LOOP EXECUTE format('ALTER TABLE quotation DROP CONSTRAINT %I',r.conname); END LOOP;
END $$;
ALTER TABLE quotation ADD CONSTRAINT quotation_status CHECK(status IN('DRAFT','ISSUED','ACCEPTED','REJECTED','CONVERTED','CANCELLED'));
ALTER TABLE quotation ADD CONSTRAINT quotation_branch_identity UNIQUE(tenant_id,company_id,branch_id,id);
CREATE TABLE quotation_decision(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,
 quotation_id uuid NOT NULL,operation_id uuid NOT NULL,expected_version bigint NOT NULL CHECK(expected_version>=0),
 decision text NOT NULL CHECK(decision IN('ACCEPTED','REJECTED')),actor_id uuid NOT NULL,
 reason text NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 2000),
 evidence_reference text NOT NULL CHECK(length(trim(evidence_reference)) BETWEEN 1 AND 200),
 recorded_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,operation_id),UNIQUE(tenant_id,company_id,quotation_id,expected_version),
 FOREIGN KEY(tenant_id,company_id,branch_id,quotation_id) REFERENCES quotation(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id));
ALTER TABLE quotation_decision ENABLE ROW LEVEL SECURITY;
ALTER TABLE quotation_decision FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON quotation_decision USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())
 WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
GRANT SELECT,INSERT ON quotation_decision TO quincai_app;
CREATE TRIGGER quotation_decision_immutable BEFORE UPDATE OR DELETE ON quotation_decision FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE OR REPLACE FUNCTION protect_quotation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.status='DRAFT' AND NEW.status NOT IN('DRAFT','ISSUED','CANCELLED') THEN
  RAISE EXCEPTION 'Quotation must be issued before a customer decision' USING ERRCODE='23514';
 END IF;
 IF OLD.status IN('CONVERTED','CANCELLED','REJECTED') THEN
  RAISE EXCEPTION 'Final quotation is immutable' USING ERRCODE='23514';
 END IF;
 IF OLD.status IN('ISSUED','ACCEPTED') THEN
  IF (to_jsonb(NEW)-ARRAY['status','version','converted_sale_id']) IS DISTINCT FROM
     (to_jsonb(OLD)-ARRAY['status','version','converted_sale_id']) OR NEW.version<>OLD.version+1 THEN
   RAISE EXCEPTION 'Issued quotation content is immutable' USING ERRCODE='23514';
  END IF;
  IF OLD.status='ISSUED' THEN
   IF NEW.status NOT IN('ACCEPTED','REJECTED') OR NEW.converted_sale_id IS NOT NULL OR NOT EXISTS(
    SELECT 1 FROM quotation_decision d WHERE d.tenant_id=OLD.tenant_id AND d.company_id=OLD.company_id
      AND d.branch_id=OLD.branch_id AND d.quotation_id=OLD.id AND d.expected_version=OLD.version
      AND d.decision=NEW.status AND d.actor_id=nullif(current_setting('app.actor_id',true),'')::uuid
   ) THEN RAISE EXCEPTION 'Customer decision evidence required' USING ERRCODE='23514'; END IF;
  ELSIF NEW.status<>'CONVERTED' OR NEW.converted_sale_id IS NULL THEN
   RAISE EXCEPTION 'Accepted quotation may only stage its sale' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
