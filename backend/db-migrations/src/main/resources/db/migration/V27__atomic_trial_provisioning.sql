-- M01 §§5.1/8.1: persisted intent + atomic bootstrap via public owning-module APIs.
CREATE TABLE platform_provisioning (
 id uuid PRIMARY KEY, request_id uuid NOT NULL UNIQUE REFERENCES platform_onboarding_configuration(request_id),
 operation_id uuid NOT NULL UNIQUE, correlation_id uuid NOT NULL, actor_id uuid NOT NULL REFERENCES platform_operator(actor_id),
 request_hash text NOT NULL CHECK(length(request_hash)=64),
 tenant_id uuid NOT NULL UNIQUE, company_id uuid NOT NULL, branch_id uuid NOT NULL, warehouse_id uuid NOT NULL, period_id uuid NOT NULL,
 owner_subject uuid NOT NULL, branch_name text NOT NULL CHECK(length(trim(branch_name)) BETWEEN 1 AND 120),
 warehouse_name text NOT NULL CHECK(length(trim(warehouse_name)) BETWEEN 1 AND 120), business_date date NOT NULL,
 storage_prefix text NOT NULL UNIQUE, trial_ends_at timestamptz NOT NULL, created_at timestamptz NOT NULL, activated_at timestamptz,
 failure_code text, response_text text, status text NOT NULL CHECK(status IN('PENDING','PROVISIONING','FAILED','ACTIVE')),
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0), CHECK(trial_ends_at>created_at),
 CHECK((status='ACTIVE')=(activated_at IS NOT NULL))
);
CREATE TABLE platform_provisioning_step (
 provisioning_id uuid NOT NULL REFERENCES platform_provisioning(id), step_code text NOT NULL CHECK(step_code IN('TENANT','COMPANY','BRANCH','STOCK_LOCATION','DOCUMENTS','FINANCE','TRIAL_ENTITLEMENTS','OWNER_MEMBERSHIP','VALIDATED')),
 completed_at timestamptz NOT NULL, PRIMARY KEY(provisioning_id,step_code)
);
CREATE TABLE platform_provisioning_event (
 id uuid PRIMARY KEY, provisioning_id uuid NOT NULL REFERENCES platform_provisioning(id),
 actor_id uuid NOT NULL REFERENCES platform_operator(actor_id),correlation_id uuid NOT NULL,
 event_type text NOT NULL CHECK(event_type IN('TrialProvisioningRequested.v1','TrialProvisioningFailed.v1','TenantTrialActivated.v1')),
 created_at timestamptz NOT NULL
);
DO $$ DECLARE t text;BEGIN
 FOREACH t IN ARRAY ARRAY['platform_provisioning','platform_provisioning_step','platform_provisioning_event'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY platform_only ON %I USING(EXISTS(SELECT 1 FROM platform_operator o WHERE o.actor_id=nullif(current_setting(''app.platform_actor'',true),'''')::uuid AND o.active)) WITH CHECK(EXISTS(SELECT 1 FROM platform_operator o WHERE o.actor_id=nullif(current_setting(''app.platform_actor'',true),'''')::uuid AND o.active))',t);
 END LOOP;
END $$;
GRANT SELECT,INSERT,UPDATE ON platform_provisioning TO quincai_app;
GRANT SELECT,INSERT ON platform_provisioning_step,platform_provisioning_event TO quincai_app;
CREATE TRIGGER provisioning_step_immutable BEFORE UPDATE OR DELETE ON platform_provisioning_step FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TRIGGER provisioning_event_immutable BEFORE UPDATE OR DELETE ON platform_provisioning_event FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();

CREATE FUNCTION app_bootstrap_tenant() RETURNS uuid LANGUAGE sql STABLE AS $$
 SELECT p.tenant_id FROM platform_provisioning p
 WHERE p.id=nullif(current_setting('app.provisioning_id',true),'')::uuid AND p.status='PROVISIONING'
 AND p.tenant_id=app_tenant() AND p.company_id=app_company() AND p.branch_id=app_branch()
 AND p.actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid
 AND EXISTS(SELECT 1 FROM platform_operator o WHERE o.actor_id=p.actor_id AND o.active)
$$;
GRANT INSERT ON tenant,company,branch,warehouse,tenant_memberships,financial_period TO quincai_app;
CREATE POLICY bootstrap_insert_guard ON tenant AS RESTRICTIVE FOR INSERT TO quincai_app WITH CHECK(id=app_bootstrap_tenant());
DO $$ DECLARE t text;BEGIN
 FOREACH t IN ARRAY ARRAY['company','branch','warehouse','tenant_memberships'] LOOP
  EXECUTE format('CREATE POLICY bootstrap_insert_guard ON %I AS RESTRICTIVE FOR INSERT TO quincai_app WITH CHECK(tenant_id=app_bootstrap_tenant())',t);
 END LOOP;
END $$;
CREATE FUNCTION guard_bootstrap_member() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE p platform_provisioning;BEGIN
 IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=current_user AND rolsuper) OR NOT pg_has_role(current_user,'quincai_app','MEMBER') THEN RETURN NEW; END IF;
 SELECT * INTO p FROM platform_provisioning WHERE id=nullif(current_setting('app.provisioning_id',true),'')::uuid;
 IF p.id IS NULL OR p.status<>'PROVISIONING' OR NEW.tenant_id<>p.tenant_id OR NEW.company_id<>p.company_id OR NEW.branch_id<>p.branch_id
 OR NEW.actor_id<>p.owner_subject OR NEW.role_code<>'owner' OR NOT NEW.active OR NEW.authorization_epoch<>1
 THEN RAISE EXCEPTION 'Initial membership requires verified provisioning owner' USING ERRCODE='42501';END IF;RETURN NEW;
END $$;
CREATE TRIGGER bootstrap_member_guard BEFORE INSERT ON tenant_memberships FOR EACH ROW EXECUTE FUNCTION guard_bootstrap_member();

CREATE TABLE company_business_defaults (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,price_list_name text NOT NULL,currency char(3) NOT NULL CHECK(currency='XAF'),
 locale text NOT NULL CHECK(locale IN('en','fr')),fiscal_review_status text NOT NULL CHECK(fiscal_review_status IN('PROVISIONAL','REVIEWED')),
 tax_configuration_status text NOT NULL CHECK(tax_configuration_status IN('REQUIRES_REVIEW','REVIEWED')),
 PRIMARY KEY(tenant_id,company_id),FOREIGN KEY(tenant_id,company_id)REFERENCES company(tenant_id,id)
);
CREATE TABLE finance_bootstrap_mapping (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,operational_code text NOT NULL,statutory_account_code text,
 review_status text NOT NULL CHECK(review_status IN('PROVISIONAL','REVIEWED')),
 PRIMARY KEY(tenant_id,company_id,operational_code),FOREIGN KEY(tenant_id,company_id)REFERENCES company(tenant_id,id),
 CHECK(review_status='PROVISIONAL' OR length(trim(statutory_account_code))>0)
);
DO $$ DECLARE t text;BEGIN
 FOREACH t IN ARRAY ARRAY['company_business_defaults','finance_bootstrap_mapping'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY scoped_read ON %I FOR SELECT USING(tenant_id=app_tenant() AND company_id=app_company())',t);
 EXECUTE format('CREATE POLICY bootstrap_insert ON %I FOR INSERT WITH CHECK(tenant_id=app_bootstrap_tenant() AND company_id=app_company())',t);
 END LOOP;
END $$;
GRANT SELECT,INSERT ON company_business_defaults,finance_bootstrap_mapping TO quincai_app;
CREATE FUNCTION guard_provisioning_plan() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN
  IF NEW.status<>'PENDING' OR NOT EXISTS(SELECT 1 FROM platform_onboarding_configuration c WHERE c.request_id=NEW.request_id AND c.status='APPROVED_CONFIGURATION' AND c.owner_subject=NEW.owner_subject AND c.plan_code='standard-trial' AND c.opening_mode='EMPTY')
  THEN RAISE EXCEPTION 'Independently approved empty trial required';END IF;
 ELSE
  IF OLD.status='ACTIVE' AND (NEW.status,NEW.activated_at,NEW.tenant_id,NEW.request_id,NEW.actor_id,NEW.request_hash) IS DISTINCT FROM (OLD.status,OLD.activated_at,OLD.tenant_id,OLD.request_id,OLD.actor_id,OLD.request_hash) THEN RAISE EXCEPTION 'Activated provisioning identity is immutable';END IF;
  IF (NEW.id,NEW.request_id,NEW.operation_id,NEW.correlation_id,NEW.actor_id,NEW.request_hash,NEW.tenant_id,NEW.company_id,NEW.branch_id,NEW.warehouse_id,NEW.period_id,NEW.owner_subject,NEW.branch_name,NEW.warehouse_name,NEW.business_date,NEW.storage_prefix,NEW.trial_ends_at,NEW.created_at)
  IS DISTINCT FROM (OLD.id,OLD.request_id,OLD.operation_id,OLD.correlation_id,OLD.actor_id,OLD.request_hash,OLD.tenant_id,OLD.company_id,OLD.branch_id,OLD.warehouse_id,OLD.period_id,OLD.owner_subject,OLD.branch_name,OLD.warehouse_name,OLD.business_date,OLD.storage_prefix,OLD.trial_ends_at,OLD.created_at)
  THEN RAISE EXCEPTION 'Provisioning intent is immutable';END IF;
  IF NOT ((OLD.status IN('PENDING','FAILED') AND NEW.status IN('PROVISIONING','FAILED')) OR (OLD.status='PROVISIONING' AND NEW.status='ACTIVE') OR (OLD.status='ACTIVE' AND NEW.status='ACTIVE' AND OLD.response_text IS NULL))
  THEN RAISE EXCEPTION 'Illegal provisioning transition';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER provisioning_plan_guard BEFORE INSERT OR UPDATE ON platform_provisioning FOR EACH ROW EXECUTE FUNCTION guard_provisioning_plan();
CREATE FUNCTION require_complete_provisioning() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE p platform_provisioning;BEGIN
 SELECT * INTO p FROM platform_provisioning WHERE id=NEW.id;
 IF p.status='ACTIVE' AND (p.response_text IS NULL OR (SELECT count(*) FROM platform_provisioning_step WHERE provisioning_id=p.id)<>9
 OR NOT EXISTS(SELECT 1 FROM tenant WHERE id=p.tenant_id)
 OR NOT EXISTS(SELECT 1 FROM tenant_memberships WHERE tenant_id=p.tenant_id AND company_id=p.company_id AND branch_id=p.branch_id AND actor_id=p.owner_subject AND active AND role_code='owner')
 OR NOT EXISTS(SELECT 1 FROM document_settings WHERE tenant_id=p.tenant_id AND company_id=p.company_id AND branch_id=p.branch_id)
 OR NOT EXISTS(SELECT 1 FROM warehouse WHERE tenant_id=p.tenant_id AND company_id=p.company_id AND branch_id=p.branch_id AND id=p.warehouse_id AND active)
 OR NOT EXISTS(SELECT 1 FROM company_business_defaults WHERE tenant_id=p.tenant_id AND company_id=p.company_id AND currency='XAF')
 OR NOT EXISTS(SELECT 1 FROM financial_period WHERE tenant_id=p.tenant_id AND company_id=p.company_id AND id=p.period_id AND status='Open')
 OR (SELECT count(*) FROM document_series WHERE tenant_id=p.tenant_id AND company_id=p.company_id AND branch_id=p.branch_id)<>3
 OR (SELECT count(*) FROM finance_bootstrap_mapping WHERE tenant_id=p.tenant_id AND company_id=p.company_id)<>22
 OR NOT EXISTS(SELECT 1 FROM tenant_subscription WHERE tenant_id=p.tenant_id AND status='TRIAL' AND trial_ends_at=p.trial_ends_at))
 THEN RAISE EXCEPTION 'Incomplete bootstrap cannot activate';END IF;RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER provisioning_complete AFTER INSERT OR UPDATE ON platform_provisioning DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION require_complete_provisioning();
