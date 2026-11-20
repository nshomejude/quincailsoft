-- Architecture §41: preserved planning/evidence only. There is deliberately no APPLIED/cutover state.
CREATE TABLE recovery_plan(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,actor_id uuid NOT NULL,version bigint NOT NULL DEFAULT 0,
 incident_reference text NOT NULL,target_point timestamptz NOT NULL,source_artifact_reference text NOT NULL,preservation_reason text NOT NULL,
 status text NOT NULL CHECK(status IN('REQUESTED','COLLECTING','MATCH_REVIEW_REQUIRED','DIFFERENCES_QUARANTINED','RECEIPTS_REVIEWED','CANCELLED')),
 scope_approved_by uuid,reviewed_by uuid,next_batch integer NOT NULL DEFAULT 0 CHECK(next_batch>=0),candidate_count bigint NOT NULL DEFAULT 0 CHECK(candidate_count>=0),
 match_count bigint NOT NULL DEFAULT 0 CHECK(match_count>=0),difference_count bigint NOT NULL DEFAULT 0 CHECK(difference_count>=0),
 fence_decision_id uuid,fence_epoch bigint,candidate_artifact_sha256 text,egress_evidence_reference text,compared_at timestamptz,created_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,branch_id,id),
 CHECK(scope_approved_by IS NULL OR scope_approved_by<>actor_id),CHECK(reviewed_by IS NULL OR reviewed_by<>actor_id),
 CHECK(status NOT IN('COLLECTING','MATCH_REVIEW_REQUIRED','DIFFERENCES_QUARANTINED','RECEIPTS_REVIEWED') OR scope_approved_by IS NOT NULL),
 CHECK(status NOT IN('MATCH_REVIEW_REQUIRED','DIFFERENCES_QUARANTINED','RECEIPTS_REVIEWED') OR(fence_decision_id IS NOT NULL AND fence_epoch>0 AND candidate_artifact_sha256 ~ '^[a-f0-9]{64}$' AND egress_evidence_reference IS NOT NULL AND compared_at IS NOT NULL)),
 CHECK(status<>'RECEIPTS_REVIEWED' OR(reviewed_by IS NOT NULL AND difference_count=0)),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id));
CREATE TABLE recovery_candidate_receipt(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,actor_id uuid NOT NULL,version bigint NOT NULL DEFAULT 0,
 plan_id uuid NOT NULL,operation_id uuid NOT NULL,request_hash text NOT NULL CHECK(request_hash ~ '^[a-f0-9]{64}$'),
 source_type text,source_id uuid,response_digest text CHECK(response_digest IS NULL OR response_digest ~ '^[a-f0-9]{64}$'),
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,plan_id,operation_id),
 FOREIGN KEY(tenant_id,company_id,branch_id,plan_id) REFERENCES recovery_plan(tenant_id,company_id,branch_id,id));
CREATE TABLE recovery_receipt_difference(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,plan_id uuid NOT NULL,operation_id uuid NOT NULL,
 classification text NOT NULL CHECK(classification IN('MISSING_FROM_CANDIDATE','EXTRA_IN_CANDIDATE','CONTENT_CONFLICT','RESPONSE_EVIDENCE_UNAVAILABLE')),
 live_request_hash text,candidate_request_hash text,
 PRIMARY KEY(tenant_id,company_id,plan_id,operation_id),FOREIGN KEY(tenant_id,company_id,branch_id,plan_id) REFERENCES recovery_plan(tenant_id,company_id,branch_id,id));
DO $$ DECLARE t text;BEGIN FOREACH t IN ARRAY ARRAY['recovery_plan','recovery_candidate_receipt','recovery_receipt_difference'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY recovery_scope ON %I USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())',t);
END LOOP;END $$;
GRANT SELECT,INSERT,UPDATE ON recovery_plan TO quincai_app;
GRANT SELECT,INSERT ON recovery_candidate_receipt,recovery_receipt_difference TO quincai_app;
CREATE TRIGGER recovery_receipts_immutable BEFORE UPDATE OR DELETE ON recovery_candidate_receipt FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TRIGGER recovery_difference_immutable BEFORE UPDATE OR DELETE ON recovery_receipt_difference FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE FUNCTION guard_recovery_candidate() RETURNS trigger LANGUAGE plpgsql AS $$BEGIN
 IF NOT EXISTS(SELECT FROM recovery_plan WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND id=NEW.plan_id AND status='COLLECTING' AND actor_id=app_actor()) THEN RAISE EXCEPTION 'Only plan maker can append to collecting candidate';END IF;RETURN NEW;
END $$;
CREATE TRIGGER recovery_candidate_guard BEFORE INSERT ON recovery_candidate_receipt FOR EACH ROW EXECUTE FUNCTION guard_recovery_candidate();
CREATE FUNCTION guard_recovery_plan() RETURNS trigger LANGUAGE plpgsql AS $$BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Recovery evidence must be retained';END IF;
 IF ROW(NEW.tenant_id,NEW.company_id,NEW.branch_id,NEW.id,NEW.actor_id,NEW.incident_reference,NEW.target_point,NEW.source_artifact_reference,NEW.preservation_reason,NEW.created_at) IS DISTINCT FROM ROW(OLD.tenant_id,OLD.company_id,OLD.branch_id,OLD.id,OLD.actor_id,OLD.incident_reference,OLD.target_point,OLD.source_artifact_reference,OLD.preservation_reason,OLD.created_at) THEN RAISE EXCEPTION 'Recovery scope and preservation directive are immutable';END IF;
 IF OLD.status='REQUESTED' AND NEW.status IN('COLLECTING','CANCELLED') THEN RETURN NEW;END IF;
 IF OLD.status='COLLECTING' AND NEW.status IN('COLLECTING','MATCH_REVIEW_REQUIRED','DIFFERENCES_QUARANTINED','CANCELLED') THEN RETURN NEW;END IF;
 IF OLD.status='MATCH_REVIEW_REQUIRED' AND NEW.status IN('RECEIPTS_REVIEWED','CANCELLED') THEN RETURN NEW;END IF;
 IF OLD.status='DIFFERENCES_QUARANTINED' AND NEW.status='CANCELLED' THEN RETURN NEW;END IF;
 RAISE EXCEPTION 'Recovery plan transition is not permitted';
END $$;
CREATE TRIGGER recovery_plan_guard BEFORE UPDATE OR DELETE ON recovery_plan FOR EACH ROW EXECUTE FUNCTION guard_recovery_plan();
CREATE INDEX recovery_plan_register ON recovery_plan(tenant_id,company_id,branch_id,created_at,id);
-- Fixed source projection; no original responses, credentials, documents or unrelated tenant payloads exported.
CREATE VIEW recovery_retained_receipt_v1 WITH(security_invoker=true) AS
 SELECT tenant_id,company_id,branch_id,operation_id,request_hash,source_type,source_id,
 CASE WHEN response_json IS NULL THEN NULL ELSE encode(sha256(convert_to(response_json::text,'UTF8')),'hex') END AS response_digest,created_at
 FROM idempotency_result WHERE status='ACCEPTED' AND (source_type IS NULL OR source_type NOT LIKE 'Recovery%');
GRANT SELECT ON recovery_retained_receipt_v1 TO quincai_app;
CREATE FUNCTION guard_recovery_plan_insert() RETURNS trigger LANGUAGE plpgsql AS $$BEGIN
 IF NEW.status<>'REQUESTED' OR NEW.actor_id IS DISTINCT FROM app_actor() OR NEW.scope_approved_by IS NOT NULL OR NEW.reviewed_by IS NOT NULL OR NEW.candidate_count<>0 OR NEW.next_batch<>0 OR NEW.fence_decision_id IS NOT NULL THEN RAISE EXCEPTION 'Recovery plan must start unreviewed under the scoped actor';END IF;RETURN NEW;
END $$;
CREATE TRIGGER recovery_plan_initial BEFORE INSERT ON recovery_plan FOR EACH ROW EXECUTE FUNCTION guard_recovery_plan_insert();
