-- Database-managed signing material is never projected through an API. Runtime
-- can read but cannot rotate it; approved deployment/key rotation replaces it.
CREATE TABLE subscription_cursor_key(id integer PRIMARY KEY CHECK(id=1),secret bytea NOT NULL CHECK(octet_length(secret)=32));
INSERT INTO subscription_cursor_key SELECT 1,decode(replace(gen_random_uuid()::text||gen_random_uuid()::text,'-',''),'hex');
ALTER TABLE subscription_cursor_key ENABLE ROW LEVEL SECURITY;ALTER TABLE subscription_cursor_key FORCE ROW LEVEL SECURITY;
CREATE POLICY subscription_cursor_read ON subscription_cursor_key FOR SELECT USING(app_actor() IS NOT NULL OR EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
GRANT SELECT ON subscription_cursor_key TO quincai_app;

CREATE TABLE subscription_commercial_term (
 id uuid PRIMARY KEY,tenant_id uuid NOT NULL,invoice_id uuid NOT NULL UNIQUE,
 plan_code text NOT NULL REFERENCES subscription_plan(code),basis text NOT NULL CHECK(basis IN('PRORATED','NEGOTIATED')),
 status text NOT NULL CHECK(status IN('REQUESTED','APPROVED')),
 monthly_price_xaf bigint NOT NULL CHECK(monthly_price_xaf>=0),subtotal_xaf bigint NOT NULL CHECK(subtotal_xaf>0),
 discount_xaf bigint NOT NULL CHECK(discount_xaf>=0 AND discount_xaf<subtotal_xaf),tax_xaf bigint NOT NULL CHECK(tax_xaf>=0),total_xaf bigint NOT NULL CHECK(total_xaf>0),
 period_start date NOT NULL,period_end date NOT NULL CHECK(period_end>=period_start),
 evidence_reference text NOT NULL CHECK(length(trim(evidence_reference)) BETWEEN 1 AND 2000),
 evidence_sha256 text NOT NULL CHECK(evidence_sha256 ~ '^[0-9a-f]{64}$'),reason text NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 2000),
 recorded_by uuid NOT NULL REFERENCES platform_operator(actor_id),reviewed_by uuid REFERENCES platform_operator(actor_id),reviewed_at timestamptz,
 FOREIGN KEY(tenant_id,invoice_id) REFERENCES subscription_invoice(tenant_id,id),
 CHECK(total_xaf::numeric=subtotal_xaf::numeric-discount_xaf::numeric+tax_xaf::numeric),
 CHECK(period_end-period_start<366),
 CHECK((status='REQUESTED' AND reviewed_by IS NULL AND reviewed_at IS NULL) OR (status='APPROVED' AND reviewed_by IS NOT NULL AND reviewed_by<>recorded_by AND reviewed_at IS NOT NULL))
);
ALTER TABLE subscription_commercial_term ENABLE ROW LEVEL SECURITY;ALTER TABLE subscription_commercial_term FORCE ROW LEVEL SECURITY;
CREATE POLICY subscription_platform ON subscription_commercial_term USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
GRANT SELECT,INSERT,UPDATE ON subscription_commercial_term TO quincai_app;
CREATE FUNCTION guard_subscription_commercial_term() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE invoice subscription_invoice; price bigint; days integer;
BEGIN
 PERFORM pg_advisory_xact_lock(hashtextextended('subscription-credit:'||NEW.invoice_id::text,0));
 SELECT * INTO invoice FROM subscription_invoice WHERE id=NEW.invoice_id AND tenant_id=NEW.tenant_id AND kind='INVOICE';
 SELECT monthly_price_xaf INTO price FROM subscription_plan WHERE code=NEW.plan_code;
 IF invoice.id IS NULL OR ROW(NEW.subtotal_xaf,NEW.discount_xaf,NEW.tax_xaf,NEW.total_xaf,NEW.period_start,NEW.period_end,NEW.monthly_price_xaf)
  IS DISTINCT FROM ROW(invoice.subtotal_xaf,invoice.discount_xaf,invoice.tax_xaf,invoice.total_xaf,invoice.period_start,invoice.period_end,price) THEN RAISE EXCEPTION 'Commercial terms must snapshot the exact invoice and plan' USING ERRCODE='23514';END IF;
 IF NEW.basis='PRORATED' THEN
  days:=extract(day from (date_trunc('month',NEW.period_start)+interval '1 month - 1 day'))::integer;
  IF date_trunc('month',NEW.period_start)<>date_trunc('month',NEW.period_end) OR NEW.discount_xaf<>0
   OR NEW.subtotal_xaf::numeric<>floor((2*price::numeric*(NEW.period_end-NEW.period_start+1)+days)/(2*days)) THEN RAISE EXCEPTION 'Proration must use exact inclusive calendar days with half-up XAF rounding' USING ERRCODE='23514';END IF;
 END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.status<>'REQUESTED' OR NEW.recorded_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid THEN RAISE EXCEPTION 'Recorded commercial request required' USING ERRCODE='23514';END IF;
 ELSE
  IF OLD.status<>'REQUESTED' OR NEW.status<>'APPROVED' OR NEW.reviewed_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid OR NEW.reviewed_by=invoice.actor_id
   OR (to_jsonb(NEW)-'status'-'reviewed_by'-'reviewed_at') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'reviewed_by'-'reviewed_at') THEN RAISE EXCEPTION 'Independent immutable commercial approval required' USING ERRCODE='23514';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER subscription_commercial_term_guard BEFORE INSERT OR UPDATE ON subscription_commercial_term FOR EACH ROW EXECUTE FUNCTION guard_subscription_commercial_term();

CREATE FUNCTION subscription_commercial_matches(invoice subscription_invoice,plan text) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT (invoice.subtotal_xaf=p.monthly_price_xaf AND invoice.discount_xaf=0 AND extract(day from invoice.period_start)=1 AND invoice.period_end=((invoice.period_start+interval '1 month')::date-1))
 OR EXISTS(SELECT 1 FROM subscription_commercial_term t WHERE t.invoice_id=invoice.id AND t.tenant_id=invoice.tenant_id AND t.plan_code=p.code AND t.status='APPROVED'
  AND ROW(t.subtotal_xaf,t.discount_xaf,t.tax_xaf,t.total_xaf,t.period_start,t.period_end,t.monthly_price_xaf)=ROW(invoice.subtotal_xaf,invoice.discount_xaf,invoice.tax_xaf,invoice.total_xaf,invoice.period_start,invoice.period_end,p.monthly_price_xaf))
 FROM subscription_plan p WHERE p.code=plan
$$;

-- Replace guards forward; all existing paid-evidence and consumption safeguards remain.
CREATE OR REPLACE FUNCTION protect_subscription_lifecycle() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE payment subscription_manual_payment; invoice subscription_invoice; selected_plan subscription_plan; paid numeric; credits numeric;
BEGIN
 IF TG_OP='INSERT' THEN
  IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname=current_user AND rolsuper)
   AND (NEW.status<>'TRIAL' OR NEW.version<>0 OR NEW.term_ends_at IS NOT NULL OR NEW.activation_payment_id IS NOT NULL) THEN
   RAISE EXCEPTION 'Runtime subscriptions must start as unactivated trials' USING ERRCODE='23514';END IF;
  RETURN NEW;
 END IF;
 IF ROW(NEW.tenant_id,NEW.trial_ends_at,NEW.created_at) IS DISTINCT FROM ROW(OLD.tenant_id,OLD.trial_ends_at,OLD.created_at)
 OR NEW.version<>OLD.version+1 OR NEW.status='TRIAL'
 OR (NEW.status='EXPIRED' AND NOT ((OLD.status='TRIAL' AND NEW.trial_ends_at<=now()) OR (OLD.status='ACTIVE' AND OLD.term_ends_at<=now()))) THEN
  RAISE EXCEPTION 'Invalid subscription lifecycle transition' USING ERRCODE='23514'; END IF;
 IF NEW.status='ACTIVE' THEN
  SELECT * INTO payment FROM subscription_manual_payment WHERE id=NEW.activation_payment_id AND tenant_id=NEW.tenant_id;
  SELECT * INTO invoice FROM subscription_invoice WHERE id=payment.invoice_id AND tenant_id=NEW.tenant_id AND kind='INVOICE';
  SELECT * INTO selected_plan FROM subscription_plan WHERE code=payment.plan_code;
  SELECT coalesce(sum(amount_xaf::numeric),0) INTO paid FROM subscription_manual_payment WHERE invoice_id=invoice.id AND status='MANUALLY_RECONCILED';
  SELECT coalesce(sum(total_xaf::numeric),0) INTO credits FROM subscription_invoice WHERE original_id=invoice.id;
  IF payment.id IS NULL OR invoice.id IS NULL OR payment.status<>'MANUALLY_RECONCILED' OR payment.plan_code<>NEW.plan_code
   OR NEW.term_ends_at IS DISTINCT FROM ((invoice.period_end+1)::timestamp AT TIME ZONE 'UTC')
   OR invoice.period_start> (now() AT TIME ZONE 'UTC')::date OR NEW.term_ends_at<=now() OR paid<>invoice.total_xaf::numeric-credits
   OR NOT coalesce(subscription_commercial_matches(invoice,payment.plan_code),false) OR credits<>0
   OR NOT EXISTS(SELECT 1 FROM subscription_invoice_activation a WHERE a.invoice_id=invoice.id AND a.tenant_id=NEW.tenant_id AND a.payment_id=payment.id)
   OR OLD.activation_payment_id IS NOT DISTINCT FROM NEW.activation_payment_id THEN
   RAISE EXCEPTION 'Reviewed full subscription settlement required for activation' USING ERRCODE='23514';END IF;
 ELSIF NEW.plan_code IS DISTINCT FROM OLD.plan_code OR NEW.term_ends_at IS DISTINCT FROM OLD.term_ends_at OR NEW.activation_payment_id IS DISTINCT FROM OLD.activation_payment_id THEN
  RAISE EXCEPTION 'Plan changes require reviewed paid activation' USING ERRCODE='23514';
 END IF;
 RETURN NEW; END $$;
CREATE OR REPLACE FUNCTION protect_subscription_manual_payment() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE invoice subscription_invoice; paid numeric; credits numeric;
BEGIN
 PERFORM pg_advisory_xact_lock(hashtextextended('subscription-credit:'||NEW.invoice_id::text,0));
 SELECT * INTO invoice FROM subscription_invoice WHERE id=NEW.invoice_id AND tenant_id=NEW.tenant_id AND kind='INVOICE';
 IF invoice.id IS NULL THEN RAISE EXCEPTION 'Subscription invoice required' USING ERRCODE='23514';END IF;
 IF NOT coalesce(subscription_commercial_matches(invoice,NEW.plan_code),false) THEN
  RAISE EXCEPTION 'Invoice must match plan price or approved commercial terms' USING ERRCODE='23514';END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.status<>'RECORDED' OR NEW.recorded_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid THEN RAISE EXCEPTION 'Recorded manual bank evidence required' USING ERRCODE='23514'; END IF;
 ELSE
  IF OLD.status<>'RECORDED' OR NEW.status<>'MANUALLY_RECONCILED' OR NEW.reviewed_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid
  OR (to_jsonb(NEW)-'status'-'reviewed_by'-'reviewed_at') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'reviewed_by'-'reviewed_at') THEN RAISE EXCEPTION 'Independent immutable payment review required' USING ERRCODE='23514'; END IF;
 END IF;
 SELECT coalesce(sum(amount_xaf::numeric),0) INTO paid FROM subscription_manual_payment WHERE invoice_id=NEW.invoice_id AND id<>NEW.id;
 SELECT coalesce(sum(total_xaf::numeric),0) INTO credits FROM subscription_invoice WHERE original_id=NEW.invoice_id;
 IF paid+NEW.amount_xaf::numeric>invoice.total_xaf::numeric-credits THEN RAISE EXCEPTION 'Subscription allocation exceeds unpaid invoice' USING ERRCODE='23514';END IF;
 IF EXISTS(SELECT 1 FROM subscription_manual_payment WHERE invoice_id=NEW.invoice_id AND plan_code<>NEW.plan_code) THEN RAISE EXCEPTION 'Invoice allocations must select one plan' USING ERRCODE='23514';END IF;
 RETURN NEW; END $$;
