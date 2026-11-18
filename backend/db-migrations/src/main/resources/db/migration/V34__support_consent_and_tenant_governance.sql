CREATE TABLE platform_support_grant (
 id uuid PRIMARY KEY,tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,
 owner_actor_id uuid NOT NULL,operator_id uuid NOT NULL,approved_by uuid,revoked_by uuid,
 status text NOT NULL CHECK(status IN('PENDING','ACTIVE','REVOKED')),expires_at timestamptz NOT NULL,created_at timestamptz NOT NULL,
 ticket_reference text NOT NULL CHECK(length(trim(ticket_reference)) BETWEEN 1 AND 200),reason text NOT NULL CHECK(length(trim(reason)) BETWEEN 3 AND 2000),
 version bigint NOT NULL DEFAULT 0,FOREIGN KEY(tenant_id,company_id,branch_id)REFERENCES branch(tenant_id,company_id,id),
 CHECK(expires_at>created_at AND expires_at<=created_at+interval '24 hours'),CHECK(operator_id<>owner_actor_id),
 CHECK(approved_by IS NULL OR (approved_by<>operator_id AND approved_by<>owner_actor_id)),
 CHECK((status='PENDING' AND approved_by IS NULL AND revoked_by IS NULL) OR (status='ACTIVE' AND approved_by IS NOT NULL AND revoked_by IS NULL) OR (status='REVOKED' AND revoked_by IS NOT NULL))
);
CREATE TABLE platform_governance_request (
 id uuid PRIMARY KEY,tenant_id uuid NOT NULL REFERENCES tenant(id),kind text NOT NULL CHECK(kind IN('SUSPEND','RESUME','PLACE_LEGAL_HOLD','RELEASE_LEGAL_HOLD')),
 status text NOT NULL CHECK(status IN('PENDING','APPLIED')),requested_by uuid NOT NULL REFERENCES platform_operator(actor_id),approved_by uuid REFERENCES platform_operator(actor_id),
 expected_target_version bigint NOT NULL CHECK(expected_target_version>=0),reference text NOT NULL CHECK(length(trim(reference)) BETWEEN 1 AND 200),
 reason text NOT NULL CHECK(length(trim(reason)) BETWEEN 3 AND 2000),hold_id uuid,created_at timestamptz NOT NULL,version bigint NOT NULL DEFAULT 0,
 CHECK(approved_by IS NULL OR approved_by<>requested_by),CHECK((status='PENDING' AND approved_by IS NULL)OR(status='APPLIED' AND approved_by IS NOT NULL)),
 CHECK((kind='RELEASE_LEGAL_HOLD')=(hold_id IS NOT NULL))
);
CREATE TABLE platform_tenant_lifecycle (
 tenant_id uuid PRIMARY KEY REFERENCES tenant(id),status text NOT NULL CHECK(status IN('ACTIVE','SUSPENDED')),
 epoch bigint NOT NULL CHECK(epoch>0),last_decision_id uuid NOT NULL REFERENCES platform_governance_request(id)
);
CREATE TABLE platform_legal_hold (
 id uuid PRIMARY KEY REFERENCES platform_governance_request(id),tenant_id uuid NOT NULL REFERENCES tenant(id),reference text NOT NULL,reason text NOT NULL,
 status text NOT NULL CHECK(status IN('ACTIVE','RELEASED')),placed_by uuid NOT NULL REFERENCES platform_operator(actor_id),released_by uuid REFERENCES platform_operator(actor_id),
 created_at timestamptz NOT NULL,version bigint NOT NULL DEFAULT 0,CHECK((status='ACTIVE' AND released_by IS NULL)OR(status='RELEASED' AND released_by IS NOT NULL))
);
CREATE TABLE platform_governance_operation (
 tenant_id uuid NOT NULL REFERENCES tenant(id),id uuid NOT NULL,actor_id uuid NOT NULL,request_hash text NOT NULL CHECK(length(request_hash)=64),response_text text NOT NULL,created_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,id)
);
CREATE TABLE platform_governance_event (
 id uuid PRIMARY KEY,tenant_id uuid NOT NULL REFERENCES tenant(id),actor_id uuid NOT NULL,correlation_id uuid NOT NULL,source_id uuid NOT NULL,
 event_type text NOT NULL CHECK(event_type IN('SupportConsented','SupportApproved','SupportRevoked','SupportDiagnosticsRead','GovernanceRequested','TenantSuspended','TenantResumed','LegalHoldPlaced','LegalHoldReleased')),
 created_at timestamptz NOT NULL
);
ALTER TABLE platform_support_grant ENABLE ROW LEVEL SECURITY;ALTER TABLE platform_support_grant FORCE ROW LEVEL SECURITY;
CREATE POLICY support_owner ON platform_support_grant USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch() AND EXISTS(SELECT 1 FROM tenant_memberships m WHERE m.tenant_id=app_tenant() AND m.company_id=app_company() AND m.branch_id=app_branch() AND m.actor_id=app_actor() AND m.active AND m.role_code='owner'));
CREATE POLICY support_platform ON platform_support_grant USING(EXISTS(SELECT 1 FROM platform_operator o WHERE o.actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND o.active));
DO $$ DECLARE t text;BEGIN
 FOREACH t IN ARRAY ARRAY['platform_governance_request','platform_tenant_lifecycle','platform_legal_hold','platform_governance_operation','platform_governance_event'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY governance_platform ON %I USING(EXISTS(SELECT 1 FROM platform_operator o WHERE o.actor_id=nullif(current_setting(''app.platform_actor'',true),'''')::uuid AND o.active))',t);
 END LOOP;
END $$;
CREATE POLICY lifecycle_member_read ON platform_tenant_lifecycle FOR SELECT USING(tenant_id=app_tenant());
CREATE POLICY holds_member_read ON platform_legal_hold FOR SELECT USING(tenant_id=app_tenant());
CREATE POLICY governance_owner_commands ON platform_governance_operation USING(tenant_id=app_tenant() AND actor_id=app_actor());
CREATE POLICY governance_owner_events ON platform_governance_event FOR INSERT WITH CHECK(tenant_id=app_tenant() AND actor_id=app_actor());
GRANT SELECT,INSERT,UPDATE ON platform_support_grant,platform_governance_request,platform_tenant_lifecycle,platform_legal_hold TO quincai_app;
GRANT SELECT,INSERT ON platform_governance_operation,platform_governance_event TO quincai_app;
CREATE TRIGGER governance_operation_immutable BEFORE UPDATE OR DELETE ON platform_governance_operation FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TRIGGER governance_event_immutable BEFORE UPDATE OR DELETE ON platform_governance_event FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE FUNCTION guard_support_consent() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' OR NEW.status='REVOKED' THEN
  IF NEW.tenant_id IS DISTINCT FROM app_tenant() OR NEW.company_id IS DISTINCT FROM app_company() OR NEW.branch_id IS DISTINCT FROM app_branch() OR NOT EXISTS(SELECT 1 FROM tenant_memberships m WHERE m.tenant_id=NEW.tenant_id AND m.company_id=NEW.company_id AND m.branch_id=NEW.branch_id AND m.actor_id=app_actor() AND m.active AND m.role_code='owner') THEN RAISE EXCEPTION 'Actual scoped owner required' USING ERRCODE='42501';END IF;
 END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.owner_actor_id IS DISTINCT FROM app_actor() OR NEW.status<>'PENDING' OR NEW.expires_at<=now() OR NEW.expires_at>now()+interval '24 hours' THEN RAISE EXCEPTION 'Current scoped owner consent required' USING ERRCODE='42501';END IF;
 ELSE
  IF (to_jsonb(NEW)-'status'-'approved_by'-'revoked_by'-'version') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'approved_by'-'revoked_by'-'version') OR NEW.version<>OLD.version+1 THEN RAISE EXCEPTION 'Consent scope and identity are immutable';END IF;
  IF NEW.status='ACTIVE' THEN
   IF OLD.status<>'PENDING' OR NEW.expires_at<=now() OR NEW.approved_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid OR NEW.approved_by=NEW.operator_id OR NEW.approved_by=NEW.owner_actor_id OR NEW.revoked_by IS NOT NULL THEN RAISE EXCEPTION 'Independent current platform approval required' USING ERRCODE='42501';END IF;
  ELSIF NEW.status='REVOKED' THEN
   IF OLD.status NOT IN('PENDING','ACTIVE') OR NEW.revoked_by IS DISTINCT FROM app_actor() OR NEW.approved_by IS DISTINCT FROM OLD.approved_by THEN RAISE EXCEPTION 'Scoped owner revocation required' USING ERRCODE='42501';END IF;
  ELSE RAISE EXCEPTION 'Illegal support transition';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER support_consent_guard BEFORE INSERT OR UPDATE ON platform_support_grant FOR EACH ROW EXECUTE FUNCTION guard_support_consent();
CREATE FUNCTION guard_governance_request() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN
  IF NEW.status<>'PENDING' OR NEW.requested_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid THEN RAISE EXCEPTION 'Current platform request actor required';END IF;
 ELSE
  IF (to_jsonb(NEW)-'status'-'approved_by'-'version') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'approved_by'-'version') OR NEW.version<>OLD.version+1 OR OLD.status<>'PENDING' OR NEW.status<>'APPLIED' OR NEW.approved_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid OR NEW.approved_by=OLD.requested_by THEN RAISE EXCEPTION 'Independent governance review required';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER governance_request_guard BEFORE INSERT OR UPDATE ON platform_governance_request FOR EACH ROW EXECUTE FUNCTION guard_governance_request();
CREATE FUNCTION guard_tenant_lifecycle() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE r platform_governance_request;BEGIN
 SELECT * INTO r FROM platform_governance_request WHERE id=NEW.last_decision_id;
 IF r.id IS NULL OR r.status<>'APPLIED' OR r.tenant_id<>NEW.tenant_id OR r.approved_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid
 OR (NEW.status='SUSPENDED' AND r.kind<>'SUSPEND') OR (NEW.status='ACTIVE' AND r.kind<>'RESUME') THEN RAISE EXCEPTION 'Applied independent lifecycle request required';END IF;
 IF TG_OP='INSERT' THEN IF NEW.epoch<>1 OR NEW.status<>'SUSPENDED' OR r.expected_target_version<>0 THEN RAISE EXCEPTION 'Initial suspension epoch required';END IF;
 ELSE IF NEW.tenant_id<>OLD.tenant_id OR NEW.epoch<>OLD.epoch+1 OR NEW.status=OLD.status OR r.expected_target_version<>OLD.epoch THEN RAISE EXCEPTION 'Current monotonic lifecycle epoch required';END IF;END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER tenant_lifecycle_guard BEFORE INSERT OR UPDATE ON platform_tenant_lifecycle FOR EACH ROW EXECUTE FUNCTION guard_tenant_lifecycle();
CREATE FUNCTION guard_legal_hold() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE r platform_governance_request;BEGIN
 IF TG_OP='INSERT' THEN
  SELECT * INTO r FROM platform_governance_request WHERE id=NEW.id;
  IF r.id IS NULL OR r.status<>'APPLIED' OR r.kind<>'PLACE_LEGAL_HOLD' OR r.tenant_id<>NEW.tenant_id OR NEW.status<>'ACTIVE' OR NEW.placed_by IS DISTINCT FROM r.approved_by OR NEW.released_by IS NOT NULL THEN RAISE EXCEPTION 'Independently applied legal hold required';END IF;
 ELSE
  SELECT * INTO r FROM platform_governance_request WHERE id=nullif(current_setting('app.hold_release_request',true),'')::uuid;
  IF r.id IS NULL OR r.status<>'APPLIED' OR r.kind<>'RELEASE_LEGAL_HOLD' OR r.hold_id<>OLD.id OR r.tenant_id<>OLD.tenant_id OR r.expected_target_version<>OLD.version
   OR OLD.status<>'ACTIVE' OR NEW.status<>'RELEASED' OR NEW.released_by IS DISTINCT FROM r.approved_by OR NEW.version<>OLD.version+1
   OR (to_jsonb(NEW)-'status'-'released_by'-'version') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'released_by'-'version') THEN RAISE EXCEPTION 'Current independently reviewed legal hold release required';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER legal_hold_guard BEFORE INSERT OR UPDATE ON platform_legal_hold FOR EACH ROW EXECUTE FUNCTION guard_legal_hold();
-- No deletion grant, hard-delete endpoint or automated retention execution is introduced.
