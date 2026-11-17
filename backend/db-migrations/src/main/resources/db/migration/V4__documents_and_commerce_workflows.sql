CREATE TABLE document_settings (
 tenant_id uuid NOT NULL, company_id uuid NOT NULL, branch_id uuid NOT NULL,
 issuer_name text NOT NULL, issuer_address text NOT NULL, registration_number text NOT NULL,
 fiscal_series text NOT NULL, locale varchar(2) NOT NULL CHECK(locale IN ('en','fr')), version bigint NOT NULL DEFAULT 1,
 PRIMARY KEY(tenant_id,company_id,branch_id), FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id));
CREATE TABLE document_series (
 tenant_id uuid NOT NULL, company_id uuid NOT NULL, branch_id uuid NOT NULL, kind text NOT NULL, fiscal_series text NOT NULL,
 next_number bigint NOT NULL CHECK(next_number>0), PRIMARY KEY(tenant_id,company_id,branch_id,kind,fiscal_series),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id));
CREATE TABLE document_snapshot (
 tenant_id uuid NOT NULL, company_id uuid NOT NULL, branch_id uuid NOT NULL,id uuid NOT NULL,
 source_id uuid NOT NULL,operation_id uuid NOT NULL,number text NOT NULL,kind text NOT NULL CHECK(kind IN('INVOICE','CREDIT_NOTE','QUOTATION')),
 business_date date NOT NULL,total_xaf bigint NOT NULL CHECK(total_xaf>0),sha256 varchar(64) NOT NULL,content text NOT NULL,issued_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,branch_id,kind,source_id),UNIQUE(tenant_id,branch_id,number),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id));
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['document_settings','document_series','document_snapshot'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY scope_isolation ON %I USING (tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK (tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())',t);
 END LOOP;END $$;
GRANT SELECT,INSERT,UPDATE ON document_settings,document_series TO quincai_app;
GRANT SELECT,INSERT ON document_snapshot TO quincai_app;
CREATE TRIGGER document_immutable BEFORE UPDATE OR DELETE ON document_snapshot FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
-- The sale transaction must issue its original invoice before commit.
CREATE FUNCTION require_sale_invoice() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NEW.status='POSTED' AND NOT EXISTS(SELECT 1 FROM document_snapshot d WHERE d.tenant_id=NEW.tenant_id AND d.company_id=NEW.company_id AND d.branch_id=NEW.branch_id AND d.source_id=NEW.id AND d.kind='INVOICE' AND d.total_xaf=NEW.total_xaf) THEN
 RAISE EXCEPTION 'Posted sale requires immutable numbered invoice in the same transaction' USING ERRCODE='23514';END IF;RETURN NEW;END $$;
CREATE CONSTRAINT TRIGGER sale_requires_invoice AFTER INSERT OR UPDATE ON sale DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION require_sale_invoice();

CREATE TABLE quotation(LIKE sale INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
ALTER TABLE quotation DROP COLUMN cost_xaf;
ALTER TABLE quotation ADD COLUMN valid_until date NOT NULL;
ALTER TABLE quotation ADD COLUMN converted_sale_id uuid;
ALTER TABLE quotation ADD PRIMARY KEY(tenant_id,company_id,id);
ALTER TABLE quotation ADD FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id);
CREATE TABLE quotation_line(LIKE sale_line INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
ALTER TABLE quotation_line RENAME COLUMN sale_id TO quotation_id;
ALTER TABLE quotation_line ADD PRIMARY KEY(tenant_id,company_id,id);
ALTER TABLE quotation_line ADD FOREIGN KEY(tenant_id,company_id,quotation_id) REFERENCES quotation(tenant_id,company_id,id);
ALTER TABLE quotation ENABLE ROW LEVEL SECURITY;ALTER TABLE quotation FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON quotation USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
ALTER TABLE quotation_line ENABLE ROW LEVEL SECURITY;ALTER TABLE quotation_line FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON quotation_line USING(tenant_id=app_tenant() AND company_id=app_company() AND EXISTS(SELECT 1 FROM quotation q WHERE q.tenant_id=quotation_line.tenant_id AND q.company_id=quotation_line.company_id AND q.id=quotation_line.quotation_id)) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND EXISTS(SELECT 1 FROM quotation q WHERE q.tenant_id=quotation_line.tenant_id AND q.company_id=quotation_line.company_id AND q.id=quotation_line.quotation_id));
GRANT SELECT,INSERT,UPDATE ON quotation TO quincai_app;GRANT SELECT,INSERT ON quotation_line TO quincai_app;
CREATE TRIGGER quotation_lines_immutable BEFORE UPDATE OR DELETE ON quotation_line FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
DO $$ DECLARE r record; BEGIN FOR r IN SELECT conname FROM pg_constraint WHERE conrelid='quotation'::regclass AND contype='c' AND pg_get_constraintdef(oid) LIKE '%status%' LOOP EXECUTE format('ALTER TABLE quotation DROP CONSTRAINT %I',r.conname); END LOOP; END $$;
ALTER TABLE quotation ADD CHECK(status IN('DRAFT','ISSUED','CONVERTED','CANCELLED'));
ALTER TABLE quotation ADD FOREIGN KEY(tenant_id,company_id,customer_id) REFERENCES customer(tenant_id,company_id,id);
ALTER TABLE quotation ADD FOREIGN KEY(tenant_id,company_id,branch_id,warehouse_id) REFERENCES warehouse(tenant_id,company_id,branch_id,id);
ALTER TABLE quotation_line ADD FOREIGN KEY(tenant_id,company_id,product_id) REFERENCES catalog_product(tenant_id,company_id,id);
CREATE FUNCTION protect_quotation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.status='CONVERTED' OR OLD.status='CANCELLED' OR (OLD.status='ISSUED' AND (NEW.status<>'CONVERTED' OR NEW.customer_id IS DISTINCT FROM OLD.customer_id OR NEW.warehouse_id<>OLD.warehouse_id OR NEW.total_xaf<>OLD.total_xaf OR NEW.business_date<>OLD.business_date OR NEW.valid_until<>OLD.valid_until)) THEN RAISE EXCEPTION 'Issued quotation content is immutable' USING ERRCODE='23514'; END IF;RETURN NEW;END $$;
CREATE TRIGGER quotation_immutable BEFORE UPDATE ON quotation FOR EACH ROW EXECUTE FUNCTION protect_quotation();
CREATE FUNCTION protect_quotation_line() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM quotation WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND id=NEW.quotation_id AND status='DRAFT') THEN RAISE EXCEPTION 'Lines require a draft quotation' USING ERRCODE='23514';END IF;RETURN NEW;END $$;
CREATE TRIGGER quotation_line_insert BEFORE INSERT ON quotation_line FOR EACH ROW EXECUTE FUNCTION protect_quotation_line();
CREATE TABLE customer_return(tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,sale_id uuid NOT NULL,warehouse_id uuid NOT NULL,customer_id uuid NOT NULL,created_by uuid NOT NULL,approved_by uuid,operation_id uuid NOT NULL,status text NOT NULL CHECK(status IN('DRAFT','APPROVED','POSTED')),condition_code text NOT NULL CHECK(condition_code='SELLABLE'),evidence_reference text NOT NULL,business_date date NOT NULL,version bigint NOT NULL DEFAULT 0,credit_xaf bigint NOT NULL DEFAULT 0,customer_credit_xaf bigint NOT NULL DEFAULT 0,created_at timestamptz NOT NULL,PRIMARY KEY(tenant_id,company_id,id),FOREIGN KEY(tenant_id,company_id,sale_id) REFERENCES sale(tenant_id,company_id,id),FOREIGN KEY(tenant_id,company_id,branch_id,warehouse_id) REFERENCES warehouse(tenant_id,company_id,branch_id,id));
CREATE TABLE customer_return_line(tenant_id uuid NOT NULL,company_id uuid NOT NULL,id uuid NOT NULL,return_id uuid NOT NULL,product_id uuid NOT NULL,quantity_milli bigint NOT NULL CHECK(quantity_milli>0),credit_xaf bigint NOT NULL CHECK(credit_xaf>0),cost_xaf bigint NOT NULL CHECK(cost_xaf>0),PRIMARY KEY(tenant_id,company_id,id),FOREIGN KEY(tenant_id,company_id,return_id) REFERENCES customer_return(tenant_id,company_id,id),FOREIGN KEY(tenant_id,company_id,product_id) REFERENCES catalog_product(tenant_id,company_id,id));
ALTER TABLE customer_return ENABLE ROW LEVEL SECURITY;ALTER TABLE customer_return FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON customer_return USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
ALTER TABLE customer_return_line ENABLE ROW LEVEL SECURITY;ALTER TABLE customer_return_line FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON customer_return_line USING(tenant_id=app_tenant() AND company_id=app_company() AND EXISTS(SELECT 1 FROM customer_return r WHERE r.tenant_id=customer_return_line.tenant_id AND r.company_id=customer_return_line.company_id AND r.id=customer_return_line.return_id)) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND EXISTS(SELECT 1 FROM customer_return r WHERE r.tenant_id=customer_return_line.tenant_id AND r.company_id=customer_return_line.company_id AND r.id=customer_return_line.return_id));
GRANT SELECT,INSERT,UPDATE ON customer_return TO quincai_app;GRANT SELECT,INSERT ON customer_return_line TO quincai_app;
CREATE TRIGGER return_lines_immutable BEFORE UPDATE OR DELETE ON customer_return_line FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE FUNCTION protect_return() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.status='POSTED' THEN RAISE EXCEPTION 'Posted return is immutable' USING ERRCODE='23514';END IF;
 IF OLD.status='APPROVED' AND (NEW.sale_id<>OLD.sale_id OR NEW.warehouse_id<>OLD.warehouse_id OR NEW.customer_id<>OLD.customer_id OR NEW.credit_xaf<>OLD.credit_xaf OR NEW.business_date<>OLD.business_date OR NEW.evidence_reference<>OLD.evidence_reference OR NEW.created_by<>OLD.created_by) THEN RAISE EXCEPTION 'Approved return content is immutable' USING ERRCODE='23514';END IF;RETURN NEW;END $$;
CREATE TRIGGER return_immutable BEFORE UPDATE ON customer_return FOR EACH ROW EXECUTE FUNCTION protect_return();
CREATE FUNCTION protect_return_line() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM customer_return WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND id=NEW.return_id AND status='DRAFT') THEN RAISE EXCEPTION 'Lines require a draft return' USING ERRCODE='23514';END IF;RETURN NEW;END $$;
CREATE TRIGGER return_line_insert BEFORE INSERT ON customer_return_line FOR EACH ROW EXECUTE FUNCTION protect_return_line();
