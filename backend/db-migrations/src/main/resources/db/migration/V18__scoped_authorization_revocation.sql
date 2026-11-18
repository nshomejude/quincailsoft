-- Revocation changes only an authorization epoch. It cannot change identity, branch or role.
CREATE TABLE identity_revocation(tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,actor_id uuid NOT NULL,target_actor uuid NOT NULL,expected_epoch bigint NOT NULL CHECK(expected_epoch>0),reason text NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 2000),created_at timestamptz NOT NULL,created_transaction xid8 NOT NULL DEFAULT pg_current_xact_id(),PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,id),FOREIGN KEY(tenant_id,target_actor,company_id,branch_id)REFERENCES tenant_memberships(tenant_id,actor_id,company_id,branch_id));
ALTER TABLE identity_revocation ENABLE ROW LEVEL SECURITY;ALTER TABLE identity_revocation FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON identity_revocation USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
GRANT SELECT,INSERT ON identity_revocation TO quincai_app;
CREATE TRIGGER immutable_revocation BEFORE UPDATE OR DELETE ON identity_revocation FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE FUNCTION guard_revocation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NEW.actor_id IS DISTINCT FROM app_actor() OR NOT EXISTS(SELECT FROM tenant_memberships m WHERE m.tenant_id=NEW.tenant_id AND m.company_id=NEW.company_id AND m.branch_id=NEW.branch_id AND m.actor_id=app_actor() AND m.active AND (m.actor_id=NEW.target_actor OR m.role_code='owner')) THEN RAISE EXCEPTION 'Self or active branch owner required' USING ERRCODE='42501';END IF;
 NEW.created_transaction=pg_current_xact_id();RETURN NEW;END $$;
CREATE TRIGGER guard_revocation BEFORE INSERT ON identity_revocation FOR EACH ROW EXECUTE FUNCTION guard_revocation();
CREATE OR REPLACE FUNCTION guard_membership_change() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE request identity_change;revoke identity_revocation;BEGIN
 IF ROW(NEW.tenant_id,NEW.company_id,NEW.branch_id,NEW.actor_id) IS DISTINCT FROM ROW(OLD.tenant_id,OLD.company_id,OLD.branch_id,OLD.actor_id) THEN RAISE EXCEPTION 'Membership identity immutable' USING ERRCODE='42501';END IF;
 IF NEW.role_code=OLD.role_code AND NEW.active=OLD.active AND NEW.authorization_epoch=OLD.authorization_epoch+1 THEN
  SELECT * INTO revoke FROM identity_revocation WHERE tenant_id=OLD.tenant_id AND company_id=OLD.company_id AND branch_id=OLD.branch_id AND id=nullif(current_setting('app.revocation_id',true),'')::uuid;
  IF revoke.id IS NOT NULL AND revoke.target_actor=OLD.actor_id AND revoke.actor_id=app_actor() AND revoke.expected_epoch=OLD.authorization_epoch AND revoke.created_transaction=pg_current_xact_id() THEN RETURN NEW;END IF;
 END IF;
 SELECT * INTO request FROM identity_change WHERE tenant_id=OLD.tenant_id AND company_id=OLD.company_id AND id=nullif(current_setting('app.identity_change_id',true),'')::uuid FOR UPDATE;
 IF request.id IS NULL OR request.status<>'REQUESTED' OR request.branch_id IS DISTINCT FROM OLD.branch_id OR request.target_actor IS DISTINCT FROM OLD.actor_id OR request.approved_by IS DISTINCT FROM app_actor() OR request.requested_by=app_actor() OR request.target_actor=app_actor() OR request.expected_epoch<>OLD.authorization_epoch OR request.desired_role IS DISTINCT FROM NEW.role_code OR request.desired_active IS DISTINCT FROM NEW.active OR NEW.authorization_epoch<>OLD.authorization_epoch+1 THEN RAISE EXCEPTION 'Approved current membership request required' USING ERRCODE='42501';END IF;
 IF OLD.role_code='owner' AND OLD.active AND (NOT NEW.active OR NEW.role_code<>'owner') AND (SELECT count(*) FROM tenant_memberships WHERE tenant_id=OLD.tenant_id AND company_id=OLD.company_id AND branch_id=OLD.branch_id AND active AND role_code='owner')<2 THEN RAISE EXCEPTION 'Last branch owner cannot be removed' USING ERRCODE='23514';END IF;RETURN NEW;
END $$;
