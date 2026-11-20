ALTER TABLE operation_export_job DROP CONSTRAINT operation_export_job_status_check;
ALTER TABLE operation_export_job ADD CONSTRAINT operation_export_job_status_check CHECK(status IN('QUEUED','READY','FAILED','CANCELLED','EXPIRED'));
-- Operational export payload expiry only; fiscal facts, receipts and backups stay retained.
CREATE TABLE operation_export_retention (
 tenant_id uuid NOT NULL, company_id uuid NOT NULL, branch_id uuid NOT NULL,
 id uuid NOT NULL, actor_id uuid NOT NULL, version bigint NOT NULL DEFAULT 0,
 export_id uuid NOT NULL, digest text NOT NULL, reason text NOT NULL, expires_at timestamptz NOT NULL,
 status text NOT NULL CHECK(status IN ('REQUESTED','APPROVED','PURGED')),
 reviewed_by uuid, created_at timestamptz NOT NULL, purged_at timestamptz,
 PRIMARY KEY(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,export_id) REFERENCES operation_export_job(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id),
 CHECK(reviewed_by IS NULL OR reviewed_by<>actor_id)
);
ALTER TABLE operation_export_retention ENABLE ROW LEVEL SECURITY;
ALTER TABLE operation_export_retention FORCE ROW LEVEL SECURITY;
CREATE POLICY retention_scope ON operation_export_retention USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
GRANT SELECT,INSERT,UPDATE ON operation_export_retention TO quincai_app;
CREATE TRIGGER retention_no_delete BEFORE DELETE ON operation_export_retention FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE FUNCTION protect_export_retention() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN
  IF NEW.status<>'REQUESTED' OR NEW.actor_id IS DISTINCT FROM app_actor() OR NEW.reviewed_by IS NOT NULL OR NEW.purged_at IS NOT NULL THEN RAISE EXCEPTION 'Retention must start with scoped request';END IF;
 ELSE
  IF NEW.version<>OLD.version+1 THEN RAISE EXCEPTION 'Monotonic retention version required';END IF;
  IF ROW(NEW.tenant_id,NEW.company_id,NEW.branch_id,NEW.id,NEW.actor_id,NEW.export_id,NEW.digest,NEW.reason,NEW.expires_at,NEW.created_at) IS DISTINCT FROM ROW(OLD.tenant_id,OLD.company_id,OLD.branch_id,OLD.id,OLD.actor_id,OLD.export_id,OLD.digest,OLD.reason,OLD.expires_at,OLD.created_at) THEN RAISE EXCEPTION 'Retention request evidence immutable';END IF;
  IF OLD.status='REQUESTED' AND NEW.status='APPROVED' THEN
   IF NEW.reviewed_by IS DISTINCT FROM app_actor() OR NEW.reviewed_by=NEW.actor_id OR NEW.purged_at IS NOT NULL THEN RAISE EXCEPTION 'Independent retention approval required';END IF;
  ELSIF OLD.status='APPROVED' AND NEW.status='PURGED' THEN
   IF NEW.reviewed_by IS DISTINCT FROM OLD.reviewed_by OR NEW.actor_id IS DISTINCT FROM app_actor() OR NEW.purged_at IS NULL OR NOT EXISTS(SELECT 1 FROM operation_export_job e WHERE e.tenant_id=NEW.tenant_id AND e.company_id=NEW.company_id AND e.id=NEW.export_id AND e.status='EXPIRED' AND e.content='') THEN RAISE EXCEPTION 'Confirmed payload expiry required';END IF;
  ELSE RAISE EXCEPTION 'Invalid retention transition';END IF;
 END IF; RETURN NEW; END $$;
CREATE TRIGGER retention_guard BEFORE INSERT OR UPDATE ON operation_export_retention FOR EACH ROW EXECUTE FUNCTION protect_export_retention();
CREATE OR REPLACE FUNCTION protect_export_job() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE approval operation_export_retention; BEGIN
 IF OLD.status='READY' AND NEW.status='EXPIRED' THEN
  PERFORM pg_advisory_xact_lock_shared(hashtextextended('tenant-lifecycle:'||OLD.tenant_id,0));
  SELECT * INTO approval FROM operation_export_retention WHERE tenant_id=OLD.tenant_id AND company_id=OLD.company_id AND id=nullif(current_setting('app.retention_request',true),'')::uuid;
  IF approval.id IS NULL OR approval.status<>'APPROVED' OR approval.export_id<>OLD.id OR approval.actor_id IS DISTINCT FROM app_actor() OR approval.digest<>OLD.digest OR approval.expires_at<>OLD.expires_at OR approval.reviewed_by IS NULL OR approval.reviewed_by=approval.actor_id
   OR OLD.expires_at>clock_timestamp() OR NEW.content<>'' OR NEW.version<>OLD.version+1
   OR EXISTS(SELECT 1 FROM platform_legal_hold WHERE tenant_id=OLD.tenant_id AND status='ACTIVE')
   OR EXISTS(SELECT 1 FROM platform_tenant_lifecycle WHERE tenant_id=OLD.tenant_id AND status<>'ACTIVE')
   OR (to_jsonb(NEW)-'status'-'content'-'version') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'content'-'version') THEN RAISE EXCEPTION 'Reviewed, expired, unheld export payload required';END IF;
  RETURN NEW;
 END IF;
 IF OLD.status<>'QUEUED' OR NEW.status='QUEUED' OR ROW(NEW.tenant_id,NEW.company_id,NEW.branch_id,NEW.actor_id,NEW.id,NEW.entity,NEW.created_at,NEW.expires_at,NEW.authorization_epoch) IS DISTINCT FROM ROW(OLD.tenant_id,OLD.company_id,OLD.branch_id,OLD.actor_id,OLD.id,OLD.entity,OLD.created_at,OLD.expires_at,OLD.authorization_epoch) THEN RAISE EXCEPTION 'Export is immutable after completion';END IF;RETURN NEW;END $$;
