-- Explicit reviewed company policies. No statutory leave accrual, tax rate or benefit formula is invented.
CREATE TABLE workforce_leave (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,employee_id uuid NOT NULL,
 from_date date NOT NULL,through_date date NOT NULL,leave_type text NOT NULL CHECK(leave_type IN('ANNUAL','SICK','UNPAID','OTHER')),
 requested_minutes integer NOT NULL CHECK(requested_minutes>0),status text NOT NULL CHECK(status IN('REQUESTED','APPROVED','REJECTED','CANCELLED')),
 requested_by uuid NOT NULL,reviewed_by uuid,reason text NOT NULL,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),CHECK(through_date>=from_date AND through_date<=from_date+INTERVAL '1 year'),
 CHECK(requested_minutes<=(through_date-from_date+1)*1440),CHECK(reviewed_by IS NULL OR reviewed_by<>requested_by),
 CHECK(status NOT IN('APPROVED','REJECTED') OR reviewed_by IS NOT NULL),
 FOREIGN KEY(tenant_id,company_id,branch_id,employee_id) REFERENCES workforce_employee(tenant_id,company_id,branch_id,id));
CREATE TABLE workforce_benefit_policy (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,
 code text NOT NULL CHECK(code ~ '^[A-Z0-9_]{1,60}$'),name text NOT NULL,
 effective_from date NOT NULL,effective_through date NOT NULL,
 employee_deduction_xaf bigint NOT NULL CHECK(employee_deduction_xaf>=0),employer_contribution_xaf bigint NOT NULL CHECK(employer_contribution_xaf>=0),
 policy_reference text NOT NULL,status text NOT NULL CHECK(status IN('REQUESTED','APPROVED','REJECTED')),
 requested_by uuid NOT NULL,reviewed_by uuid,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,branch_id,id),
 CHECK(effective_through>=effective_from),CHECK(employee_deduction_xaf>0 OR employer_contribution_xaf>0),
 CHECK(reviewed_by IS NULL OR reviewed_by<>requested_by),CHECK(status='REQUESTED' OR reviewed_by IS NOT NULL),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id));
CREATE TABLE workforce_benefit_election (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,
 employee_id uuid NOT NULL,policy_id uuid NOT NULL,policy_code text NOT NULL,from_date date NOT NULL,through_date date NOT NULL,
 status text NOT NULL CHECK(status IN('REQUESTED','APPROVED','REJECTED','CANCELLED')),
 requested_by uuid NOT NULL,reviewed_by uuid,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,branch_id,id),CHECK(through_date>=from_date),
 CHECK(reviewed_by IS NULL OR reviewed_by<>requested_by),CHECK(status NOT IN('APPROVED','REJECTED') OR reviewed_by IS NOT NULL),
 FOREIGN KEY(tenant_id,company_id,branch_id,employee_id) REFERENCES workforce_employee(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id,policy_id) REFERENCES workforce_benefit_policy(tenant_id,company_id,branch_id,id));
CREATE TABLE workforce_benefit_snapshot (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,payroll_id uuid NOT NULL,election_id uuid NOT NULL,policy_id uuid NOT NULL,
 code text NOT NULL,name text NOT NULL,employee_deduction_xaf bigint NOT NULL CHECK(employee_deduction_xaf>=0),
 employer_contribution_xaf bigint NOT NULL CHECK(employer_contribution_xaf>=0),policy_reference text NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,payroll_id,election_id),UNIQUE(tenant_id,company_id,payroll_id,code),
 CHECK(employee_deduction_xaf>0 OR employer_contribution_xaf>0),
 FOREIGN KEY(tenant_id,company_id,branch_id,payroll_id) REFERENCES workforce_payroll(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id,election_id) REFERENCES workforce_benefit_election(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id,policy_id) REFERENCES workforce_benefit_policy(tenant_id,company_id,branch_id,id));
DO $$ DECLARE t text; BEGIN FOREACH t IN ARRAY ARRAY['workforce_leave','workforce_benefit_policy','workforce_benefit_election','workforce_benefit_snapshot'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY workforce_scope ON %I USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())',t);
END LOOP;END $$;
GRANT SELECT,INSERT,UPDATE ON workforce_leave,workforce_benefit_policy,workforce_benefit_election TO quincai_app;
GRANT SELECT,INSERT ON workforce_benefit_snapshot TO quincai_app;
CREATE FUNCTION guard_workforce_review() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='DELETE' OR (to_jsonb(NEW)-ARRAY['status','reviewed_by','version']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['status','reviewed_by','version']) THEN RAISE EXCEPTION 'Workforce request content is immutable';END IF;
 IF OLD.status='REQUESTED' AND NEW.status IN('APPROVED','REJECTED') AND NEW.reviewed_by IS NOT NULL AND NEW.reviewed_by<>OLD.requested_by THEN RETURN NEW;END IF;
 IF TG_TABLE_NAME<>'workforce_benefit_policy' AND OLD.status IN('REQUESTED','APPROVED') AND NEW.status='CANCELLED' AND NEW.reviewed_by IS NOT DISTINCT FROM OLD.reviewed_by THEN RETURN NEW;END IF;
 RAISE EXCEPTION 'Invalid workforce review transition';
END $$;
CREATE TRIGGER workforce_leave_review BEFORE UPDATE OR DELETE ON workforce_leave FOR EACH ROW EXECUTE FUNCTION guard_workforce_review();
CREATE TRIGGER workforce_benefit_policy_review BEFORE UPDATE OR DELETE ON workforce_benefit_policy FOR EACH ROW EXECUTE FUNCTION guard_workforce_review();
CREATE TRIGGER workforce_benefit_election_review BEFORE UPDATE OR DELETE ON workforce_benefit_election FOR EACH ROW EXECUTE FUNCTION guard_workforce_review();
CREATE TRIGGER workforce_benefit_snapshot_immutable BEFORE UPDATE OR DELETE ON workforce_benefit_snapshot FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE FUNCTION guard_workforce_benefit_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE p workforce_payroll;e workforce_benefit_election;r workforce_benefit_policy; BEGIN
 SELECT * INTO p FROM workforce_payroll WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND id=NEW.payroll_id;
 SELECT * INTO e FROM workforce_benefit_election WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND id=NEW.election_id;
 SELECT * INTO r FROM workforce_benefit_policy WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND id=NEW.policy_id;
 IF p.id IS NULL OR p.status<>'DRAFT' OR e.id IS NULL OR e.status<>'APPROVED' OR r.id IS NULL OR r.status<>'APPROVED' OR e.policy_id<>r.id OR e.employee_id<>p.employee_id OR e.from_date>p.period_start OR e.through_date<p.period_end OR r.effective_from>p.period_start OR r.effective_through<p.period_end OR ROW(NEW.code,NEW.name,NEW.employee_deduction_xaf,NEW.employer_contribution_xaf,NEW.policy_reference) IS DISTINCT FROM ROW(r.code,r.name,r.employee_deduction_xaf,r.employer_contribution_xaf,r.policy_reference) THEN RAISE EXCEPTION 'Payroll benefits must snapshot active approved policy and election';END IF;
 IF NEW.employee_deduction_xaf>0 AND NOT EXISTS(SELECT FROM workforce_deduction d WHERE d.tenant_id=NEW.tenant_id AND d.company_id=NEW.company_id AND d.payroll_id=NEW.payroll_id AND d.code='BENEFIT_'||NEW.code AND d.amount_xaf=NEW.employee_deduction_xaf AND d.policy_reference=NEW.policy_reference) THEN RAISE EXCEPTION 'Employee benefit deduction must match reviewed payroll line';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER workforce_benefit_snapshot_guard BEFORE INSERT ON workforce_benefit_snapshot FOR EACH ROW EXECUTE FUNCTION guard_workforce_benefit_snapshot();
CREATE INDEX workforce_leave_register ON workforce_leave(tenant_id,company_id,branch_id,from_date,id);
CREATE INDEX workforce_benefit_election_employee ON workforce_benefit_election(tenant_id,company_id,branch_id,employee_id,from_date,through_date);
CREATE FUNCTION guard_workforce_request_insert() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NEW.status<>'REQUESTED' OR NEW.reviewed_by IS NOT NULL OR NEW.requested_by IS DISTINCT FROM app_actor() THEN RAISE EXCEPTION 'Workforce request must start unreviewed under current actor';END IF;RETURN NEW;
END $$;
CREATE TRIGGER workforce_leave_initial BEFORE INSERT ON workforce_leave FOR EACH ROW EXECUTE FUNCTION guard_workforce_request_insert();
CREATE TRIGGER workforce_benefit_policy_initial BEFORE INSERT ON workforce_benefit_policy FOR EACH ROW EXECUTE FUNCTION guard_workforce_request_insert();
CREATE TRIGGER workforce_benefit_election_initial BEFORE INSERT ON workforce_benefit_election FOR EACH ROW EXECUTE FUNCTION guard_workforce_request_insert();
