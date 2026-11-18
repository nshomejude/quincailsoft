-- Workforce extension. Manual reviewed XAF accruals; no claim of statutory deduction calculation.
CREATE TABLE workforce_employee (
 tenant_id uuid NOT NULL, company_id uuid NOT NULL, branch_id uuid NOT NULL, id uuid NOT NULL,
 employee_number text NOT NULL CHECK(length(employee_number) BETWEEN 1 AND 80), name text NOT NULL CHECK(length(name) BETWEEN 1 AND 240),
 email text NOT NULL, department text NOT NULL, job_title text NOT NULL,
 contract_type text NOT NULL CHECK(contract_type IN('PERMANENT','FIXED_TERM','CASUAL')),
 hired_on date NOT NULL, left_on date, monthly_gross_xaf bigint NOT NULL CHECK(monthly_gross_xaf>=0), version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id), UNIQUE(tenant_id,company_id,branch_id,id), UNIQUE(tenant_id,company_id,employee_number),
 CHECK(left_on IS NULL OR left_on>=hired_on), FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id)
);
CREATE TABLE workforce_attendance (
 tenant_id uuid NOT NULL, company_id uuid NOT NULL, branch_id uuid NOT NULL, id uuid NOT NULL, employee_id uuid NOT NULL,
 business_date date NOT NULL, status text NOT NULL CHECK(status IN('PRESENT','ABSENT','APPROVED_LEAVE')),
 worked_minutes integer NOT NULL CHECK(worked_minutes BETWEEN 0 AND 1440),
 PRIMARY KEY(tenant_id,company_id,id), UNIQUE(tenant_id,company_id,employee_id,business_date),
 CHECK(status='PRESENT' OR worked_minutes=0),
 FOREIGN KEY(tenant_id,company_id,branch_id,employee_id) REFERENCES workforce_employee(tenant_id,company_id,branch_id,id)
);
CREATE TABLE workforce_payroll (
 tenant_id uuid NOT NULL, company_id uuid NOT NULL, branch_id uuid NOT NULL, id uuid NOT NULL, employee_id uuid NOT NULL,
 employee_name text NOT NULL, employee_number text NOT NULL, period_start date NOT NULL, period_end date NOT NULL, business_date date NOT NULL,
 gross_xaf bigint NOT NULL CHECK(gross_xaf>0), deduction_xaf bigint NOT NULL CHECK(deduction_xaf>=0), net_xaf bigint NOT NULL CHECK(net_xaf>=0),
 calculation_policy text NOT NULL CHECK(length(calculation_policy) BETWEEN 1 AND 240),
 status text NOT NULL CHECK(status IN('DRAFT','ACCRUED_UNPAID')), prepared_by uuid NOT NULL, approved_by uuid, journal_id uuid, version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,company_id,id), CHECK(period_end>=period_start AND period_end<=period_start+INTERVAL '1 year'),
 CHECK(gross_xaf::numeric=deduction_xaf::numeric+net_xaf::numeric), CHECK(approved_by IS NULL OR approved_by<>prepared_by),
 CHECK((status='DRAFT' AND approved_by IS NULL AND journal_id IS NULL) OR (status='ACCRUED_UNPAID' AND approved_by IS NOT NULL AND journal_id IS NOT NULL)),
 FOREIGN KEY(tenant_id,company_id,branch_id,employee_id) REFERENCES workforce_employee(tenant_id,company_id,branch_id,id),
 FOREIGN KEY(tenant_id,company_id,journal_id) REFERENCES journal_entry(tenant_id,company_id,id)
);
CREATE INDEX workforce_payroll_register ON workforce_payroll(tenant_id,company_id,branch_id,business_date,id);
CREATE INDEX workforce_payroll_employee ON workforce_payroll(tenant_id,company_id,employee_id,period_start,period_end);
DO $$ DECLARE t text; BEGIN FOREACH t IN ARRAY ARRAY['workforce_employee','workforce_attendance','workforce_payroll'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);
 EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY workforce_scope ON %I USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())',t);
END LOOP; END $$;
GRANT SELECT,INSERT,UPDATE ON workforce_employee,workforce_payroll TO quincai_app;
GRANT SELECT,INSERT ON workforce_attendance TO quincai_app;
CREATE TRIGGER workforce_attendance_immutable BEFORE UPDATE OR DELETE ON workforce_attendance FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE FUNCTION protect_workforce_payroll() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='DELETE' OR OLD.status<>'DRAFT' THEN RAISE EXCEPTION 'Accrued payroll is immutable'; END IF;
 IF (to_jsonb(NEW)-ARRAY['status','approved_by','journal_id','version']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['status','approved_by','journal_id','version']) THEN RAISE EXCEPTION 'Payroll snapshot is immutable'; END IF;
 IF NEW.status<>'ACCRUED_UNPAID' OR NEW.approved_by IS NULL OR NEW.approved_by=OLD.prepared_by THEN RAISE EXCEPTION 'Independent payroll accrual required'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER workforce_payroll_immutable BEFORE UPDATE OR DELETE ON workforce_payroll FOR EACH ROW EXECUTE FUNCTION protect_workforce_payroll();
