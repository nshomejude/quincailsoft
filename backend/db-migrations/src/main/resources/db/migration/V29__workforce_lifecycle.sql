-- Forward-only workforce lifecycle. Historical imports retain explicit reviewed amounts, not invented statutory formulas.
ALTER TABLE workforce_payroll ADD COLUMN replaces_payroll_id uuid;
ALTER TABLE workforce_payroll ADD CONSTRAINT workforce_replacement_fk FOREIGN KEY(tenant_id,company_id,replaces_payroll_id) REFERENCES workforce_payroll(tenant_id,company_id,id);
DO $$ DECLARE c record; BEGIN FOR c IN SELECT conname FROM pg_constraint WHERE conrelid='workforce_payroll'::regclass AND contype='c' AND pg_get_constraintdef(oid) LIKE '%status%' LOOP
 EXECUTE format('ALTER TABLE workforce_payroll DROP CONSTRAINT %I',c.conname);
END LOOP; END $$;
ALTER TABLE workforce_payroll ADD CONSTRAINT workforce_payroll_state CHECK(
 (status IN('DRAFT','CANCELLED') AND approved_by IS NULL AND journal_id IS NULL) OR
 (status IN('ACCRUED_UNPAID','REVERSED','PAID_RECORDED') AND approved_by IS NOT NULL AND journal_id IS NOT NULL));
ALTER TABLE workforce_payroll ADD CONSTRAINT workforce_payroll_branch_identity UNIQUE(tenant_id,company_id,branch_id,id);
CREATE TABLE workforce_contract (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,employee_id uuid NOT NULL,
 effective_from date NOT NULL,contract_type text NOT NULL CHECK(contract_type IN('PERMANENT','FIXED_TERM','CASUAL')),
 monthly_gross_xaf bigint NOT NULL CHECK(monthly_gross_xaf>=0),reference text NOT NULL CHECK(length(reference) BETWEEN 1 AND 240),recorded_by uuid NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,employee_id,effective_from),
 FOREIGN KEY(tenant_id,company_id,branch_id,employee_id) REFERENCES workforce_employee(tenant_id,company_id,branch_id,id));
CREATE TABLE workforce_deduction (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,payroll_id uuid NOT NULL,
 code text NOT NULL CHECK(length(code) BETWEEN 1 AND 80),label text NOT NULL CHECK(length(label) BETWEEN 1 AND 240),
 amount_xaf bigint NOT NULL CHECK(amount_xaf>0),policy_reference text NOT NULL CHECK(length(policy_reference) BETWEEN 1 AND 240),
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,payroll_id,code),
 FOREIGN KEY(tenant_id,company_id,branch_id,payroll_id) REFERENCES workforce_payroll(tenant_id,company_id,branch_id,id));
CREATE TABLE workforce_reversal (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,payroll_id uuid NOT NULL,
 business_date date NOT NULL,status text NOT NULL CHECK(status IN('REQUESTED','APPLIED')),reason text NOT NULL,
 requested_by uuid NOT NULL,approved_by uuid,journal_id uuid,version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,payroll_id),CHECK(approved_by IS NULL OR approved_by<>requested_by),
 CHECK((status='REQUESTED' AND approved_by IS NULL AND journal_id IS NULL) OR (status='APPLIED' AND approved_by IS NOT NULL AND journal_id IS NOT NULL)),
 FOREIGN KEY(tenant_id,company_id,branch_id,payroll_id) REFERENCES workforce_payroll(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,journal_id) REFERENCES journal_entry(tenant_id,company_id,id));
CREATE TABLE workforce_payment (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,payroll_id uuid NOT NULL,
 business_date date NOT NULL,amount_xaf bigint NOT NULL CHECK(amount_xaf>0),bank_evidence_id uuid NOT NULL,journal_id uuid NOT NULL,recorded_by uuid NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,payroll_id),UNIQUE(tenant_id,company_id,bank_evidence_id),
 FOREIGN KEY(tenant_id,company_id,branch_id,payroll_id) REFERENCES workforce_payroll(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,bank_evidence_id) REFERENCES bank_evidence(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,journal_id) REFERENCES journal_entry(tenant_id,company_id,id));
-- Existing unitemized amounts remain labelled; migration does not fabricate deduction policy detail.
ALTER TABLE workforce_payroll NO FORCE ROW LEVEL SECURITY;
INSERT INTO workforce_deduction(tenant_id,company_id,branch_id,id,payroll_id,code,label,amount_xaf,policy_reference)
 SELECT tenant_id,company_id,branch_id,gen_random_uuid(),id,'REVIEWED_AGGREGATE','Previously reviewed aggregate deduction',deduction_xaf,calculation_policy FROM workforce_payroll WHERE deduction_xaf>0;
ALTER TABLE workforce_payroll FORCE ROW LEVEL SECURITY;
DO $$ DECLARE t text; BEGIN FOREACH t IN ARRAY ARRAY['workforce_contract','workforce_deduction','workforce_reversal','workforce_payment'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t); EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY workforce_scope ON %I USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())',t);
END LOOP; END $$;
GRANT SELECT,INSERT ON workforce_contract,workforce_deduction,workforce_payment TO quincai_app;
GRANT SELECT,INSERT,UPDATE ON workforce_reversal TO quincai_app;
CREATE TRIGGER workforce_contract_immutable BEFORE UPDATE OR DELETE ON workforce_contract FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TRIGGER workforce_deduction_immutable BEFORE UPDATE OR DELETE ON workforce_deduction FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TRIGGER workforce_payment_immutable BEFORE UPDATE OR DELETE ON workforce_payment FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE FUNCTION guard_workforce_deduction() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NOT EXISTS(SELECT FROM workforce_payroll WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND id=NEW.payroll_id AND status='DRAFT') THEN RAISE EXCEPTION 'Deduction belongs to a draft payroll'; END IF; RETURN NEW;
END $$;
CREATE TRIGGER workforce_deduction_draft BEFORE INSERT ON workforce_deduction FOR EACH ROW EXECUTE FUNCTION guard_workforce_deduction();
CREATE FUNCTION check_workforce_deduction_sum() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE expected bigint;actual numeric;pid uuid; BEGIN
 IF TG_TABLE_NAME='workforce_payroll' THEN pid=NEW.id; ELSE pid=NEW.payroll_id; END IF;
 SELECT deduction_xaf INTO expected FROM workforce_payroll WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND id=pid;
 SELECT coalesce(sum(amount_xaf::numeric),0) INTO actual FROM workforce_deduction WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND payroll_id=pid;
 IF expected::numeric<>actual THEN RAISE EXCEPTION 'Payroll deduction lines must reconcile exactly'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER workforce_payroll_deductions AFTER INSERT ON workforce_payroll DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_workforce_deduction_sum();
CREATE CONSTRAINT TRIGGER workforce_deduction_sum AFTER INSERT ON workforce_deduction DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_workforce_deduction_sum();
CREATE OR REPLACE FUNCTION protect_workforce_payroll() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Payroll evidence cannot be deleted'; END IF;
 IF (to_jsonb(NEW)-ARRAY['status','approved_by','journal_id','version']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['status','approved_by','journal_id','version']) THEN RAISE EXCEPTION 'Payroll snapshot is immutable'; END IF;
 IF OLD.status='DRAFT' AND NEW.status='CANCELLED' THEN RETURN NEW; END IF;
 IF OLD.status='DRAFT' AND NEW.status='ACCRUED_UNPAID' AND NEW.approved_by IS NOT NULL AND NEW.approved_by<>OLD.prepared_by THEN RETURN NEW; END IF;
 IF OLD.status='ACCRUED_UNPAID' AND ROW(NEW.approved_by,NEW.journal_id) IS NOT DISTINCT FROM ROW(OLD.approved_by,OLD.journal_id) THEN
  IF NEW.status='REVERSED' AND EXISTS(SELECT FROM workforce_reversal WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND payroll_id=NEW.id AND status='APPLIED') THEN RETURN NEW; END IF;
  IF NEW.status='PAID_RECORDED' AND EXISTS(SELECT FROM workforce_payment WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND payroll_id=NEW.id AND amount_xaf=NEW.net_xaf) THEN RETURN NEW; END IF;
 END IF;
 RAISE EXCEPTION 'Payroll transition requires immutable independent evidence';
END $$;
CREATE FUNCTION protect_workforce_reversal() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='DELETE' OR OLD.status<>'REQUESTED' OR NEW.status<>'APPLIED' OR NEW.approved_by IS NULL OR NEW.approved_by=OLD.requested_by OR (to_jsonb(NEW)-ARRAY['status','approved_by','journal_id','version']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['status','approved_by','journal_id','version']) THEN RAISE EXCEPTION 'Independent immutable reversal required'; END IF; RETURN NEW;
END $$;
CREATE TRIGGER workforce_reversal_immutable BEFORE UPDATE OR DELETE ON workforce_reversal FOR EACH ROW EXECUTE FUNCTION protect_workforce_reversal();

-- Bank evidence retains a mandatory, typed, branch-scoped consumer reference.
-- Existing settlements and payroll payments share a registry; orphan source IDs cannot commit.
ALTER TABLE bank_evidence ADD CONSTRAINT bank_evidence_branch_identity UNIQUE(tenant_id,company_id,branch_id,id);
ALTER TABLE settlement ADD CONSTRAINT settlement_bank_consumer_identity UNIQUE(tenant_id,company_id,branch_id,id,bank_evidence_id);
ALTER TABLE workforce_payment ADD CONSTRAINT workforce_bank_consumer_identity UNIQUE(tenant_id,company_id,branch_id,id,bank_evidence_id);
CREATE TABLE bank_consumption (
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,evidence_id uuid NOT NULL,
 settlement_id uuid,payroll_payment_id uuid,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,evidence_id),UNIQUE(tenant_id,company_id,id,evidence_id),
 CHECK((settlement_id IS NOT NULL AND settlement_id=id AND payroll_payment_id IS NULL) OR (payroll_payment_id IS NOT NULL AND payroll_payment_id=id AND settlement_id IS NULL)),
 FOREIGN KEY(tenant_id,company_id,branch_id,evidence_id) REFERENCES bank_evidence(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id,settlement_id,evidence_id) REFERENCES settlement(tenant_id,company_id,branch_id,id,bank_evidence_id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(tenant_id,company_id,branch_id,payroll_payment_id,evidence_id) REFERENCES workforce_payment(tenant_id,company_id,branch_id,id,bank_evidence_id) DEFERRABLE INITIALLY DEFERRED
);
ALTER TABLE bank_evidence NO FORCE ROW LEVEL SECURITY;
INSERT INTO bank_consumption(tenant_id,company_id,branch_id,id,evidence_id,settlement_id)
 SELECT tenant_id,company_id,branch_id,consumed_by,id,consumed_by FROM bank_evidence WHERE consumed_by IS NOT NULL;
ALTER TABLE bank_evidence FORCE ROW LEVEL SECURITY;
ALTER TABLE bank_evidence DROP CONSTRAINT bank_evidence_tenant_id_company_id_consumed_by_fkey;
ALTER TABLE bank_evidence ADD CONSTRAINT bank_evidence_consumption_fk FOREIGN KEY(tenant_id,company_id,consumed_by,id) REFERENCES bank_consumption(tenant_id,company_id,id,evidence_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE bank_consumption ENABLE ROW LEVEL SECURITY;ALTER TABLE bank_consumption FORCE ROW LEVEL SECURITY;
CREATE POLICY bank_consumption_scope ON bank_consumption USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
GRANT SELECT,INSERT ON bank_consumption TO quincai_app;
CREATE TRIGGER bank_consumption_immutable BEFORE UPDATE OR DELETE ON bank_consumption FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
