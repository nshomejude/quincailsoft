-- Scoped membership administration with two separate active owners and database enforcement.
DROP POLICY scope_isolation ON tenant_memberships;
CREATE POLICY scope_isolation ON tenant_memberships USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
CREATE TABLE identity_change(tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,operation_id uuid NOT NULL,target_actor uuid NOT NULL,requested_by uuid NOT NULL,approved_by uuid,approval_operation uuid,desired_role text NOT NULL,desired_active boolean NOT NULL,expected_epoch bigint NOT NULL,reason text NOT NULL CHECK(length(trim(reason)) BETWEEN 3 AND 2000),status text NOT NULL DEFAULT 'REQUESTED' CHECK(status IN('REQUESTED','APPLIED')),version bigint NOT NULL DEFAULT 0,created_at timestamptz NOT NULL DEFAULT now(),PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,operation_id),UNIQUE(tenant_id,approval_operation),FOREIGN KEY(tenant_id,target_actor,company_id,branch_id) REFERENCES tenant_memberships(tenant_id,actor_id,company_id,branch_id));
CREATE TABLE audit_event(tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,actor_id uuid NOT NULL,operation_id uuid NOT NULL,action text NOT NULL,source_type text NOT NULL,source_id uuid NOT NULL,reason text NOT NULL,recorded_at timestamptz NOT NULL DEFAULT now(),PRIMARY KEY(tenant_id,id),UNIQUE(tenant_id,operation_id,action,source_id),FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id));
ALTER TABLE identity_change ENABLE ROW LEVEL SECURITY;ALTER TABLE identity_change FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON identity_change USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
ALTER TABLE audit_event ENABLE ROW LEVEL SECURITY;ALTER TABLE audit_event FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON audit_event USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
CREATE TRIGGER immutable_fact BEFORE UPDATE OR DELETE ON audit_event FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
GRANT SELECT,INSERT ON identity_change,audit_event TO quincai_app;
GRANT UPDATE(approved_by,approval_operation,status,version) ON identity_change TO quincai_app;
GRANT UPDATE(active,role_code,authorization_epoch) ON tenant_memberships TO quincai_app;
CREATE FUNCTION guard_identity_change() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM tenant_memberships m WHERE m.tenant_id=app_tenant() AND m.company_id=app_company() AND m.branch_id=app_branch() AND m.actor_id=app_actor() AND m.active AND m.role_code='owner') THEN RAISE EXCEPTION 'Active owner required' USING ERRCODE='42501'; END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.requested_by<>app_actor() OR NEW.target_actor=app_actor() OR NEW.approved_by IS NOT NULL OR NEW.status<>'REQUESTED' THEN RAISE EXCEPTION 'Invalid membership request' USING ERRCODE='42501';END IF;
 ELSE
  IF OLD.status<>'REQUESTED' OR NEW.requested_by=app_actor() OR NEW.approved_by<>app_actor() OR NEW.target_actor=app_actor() THEN RAISE EXCEPTION 'Separate owner approval required' USING ERRCODE='42501';END IF;
 END IF;
 IF NEW.desired_role NOT IN('owner','manager','finance','inventory','purchasing','cashier','sales','delivery','auditor') THEN RAISE EXCEPTION 'Unsupported tenant role' USING ERRCODE='23514';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER identity_change_guard BEFORE INSERT OR UPDATE ON identity_change FOR EACH ROW EXECUTE FUNCTION guard_identity_change();
CREATE FUNCTION guard_membership_change() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE request identity_change;BEGIN
 SELECT * INTO request FROM identity_change WHERE tenant_id=OLD.tenant_id AND company_id=OLD.company_id AND id=nullif(current_setting('app.identity_change_id',true),'')::uuid FOR UPDATE;
 IF request.id IS NULL OR request.status<>'REQUESTED' OR request.branch_id<>OLD.branch_id OR request.target_actor<>OLD.actor_id OR request.approved_by<>app_actor() OR request.requested_by=app_actor() OR request.expected_epoch<>OLD.authorization_epoch OR request.desired_role<>NEW.role_code OR request.desired_active<>NEW.active OR NEW.authorization_epoch<>OLD.authorization_epoch+1 THEN RAISE EXCEPTION 'Approved current membership request required' USING ERRCODE='42501';END IF;
 IF OLD.role_code='owner' AND OLD.active AND (NOT NEW.active OR NEW.role_code<>'owner') AND (SELECT count(*) FROM tenant_memberships WHERE tenant_id=OLD.tenant_id AND company_id=OLD.company_id AND branch_id=OLD.branch_id AND active AND role_code='owner')<2 THEN RAISE EXCEPTION 'Last branch owner cannot be removed' USING ERRCODE='23514';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER membership_change_guard BEFORE UPDATE ON tenant_memberships FOR EACH ROW EXECUTE FUNCTION guard_membership_change();
