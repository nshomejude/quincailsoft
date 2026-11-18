CREATE TABLE subscription_plan (
 code text PRIMARY KEY CHECK(code ~ '^[a-z0-9][a-z0-9-]{0,63}$'), name text NOT NULL,
 monthly_price_xaf bigint NOT NULL CHECK(monthly_price_xaf>=0),
 seat_limit integer NOT NULL CHECK(seat_limit>0), branch_limit integer NOT NULL CHECK(branch_limit>0),
 warehouse_limit integer NOT NULL CHECK(warehouse_limit>0), commerce_access boolean NOT NULL,
 effective_at timestamptz NOT NULL
);
INSERT INTO subscription_plan VALUES('standard-trial','Standard trial',0,5,1,1,false,'2024-01-01T00:00:00Z');
CREATE TABLE tenant_subscription (
 tenant_id uuid PRIMARY KEY REFERENCES tenant(id), plan_code text NOT NULL REFERENCES subscription_plan(code),
 status text NOT NULL CHECK(status IN('TRIAL','ACTIVE','EXPIRED','CANCELLED')),
 trial_ends_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, term_ends_at timestamptz, activation_payment_id uuid,
 CHECK(trial_ends_at>created_at), CHECK(status<>'ACTIVE' OR term_ends_at IS NOT NULL)
);
CREATE TABLE subscription_notice (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES tenant_subscription(tenant_id),
 subscription_version bigint NOT NULL CHECK(subscription_version>=0),
 delivery_reference text NOT NULL, delivered_at timestamptz NOT NULL, grace_ends_at timestamptz NOT NULL,
 contract_reference text NOT NULL, reason text NOT NULL,
 status text NOT NULL CHECK(status IN('RECORDED','APPROVED')),
 recorded_by uuid NOT NULL REFERENCES platform_operator(actor_id), reviewed_by uuid REFERENCES platform_operator(actor_id),
 reviewed_at timestamptz, CHECK(grace_ends_at>delivered_at),
 CHECK((status='RECORDED' AND reviewed_by IS NULL AND reviewed_at IS NULL) OR
       (status='APPROVED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND reviewed_by<>recorded_by))
);
CREATE TABLE subscription_lifecycle_operation (
 id uuid PRIMARY KEY, actor_id uuid NOT NULL REFERENCES platform_operator(actor_id), tenant_id uuid,
 kind text NOT NULL, request_hash text NOT NULL, request_json text NOT NULL, response_json text NOT NULL, occurred_at timestamptz NOT NULL
);
ALTER TABLE subscription_plan ENABLE ROW LEVEL SECURITY; ALTER TABLE subscription_plan FORCE ROW LEVEL SECURITY;
CREATE POLICY subscription_plan_read ON subscription_plan FOR SELECT USING(true);
CREATE POLICY subscription_plan_write ON subscription_plan FOR INSERT WITH CHECK(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
GRANT SELECT,INSERT ON subscription_plan TO quincai_app;
CREATE TRIGGER immutable_subscription_plan BEFORE UPDATE OR DELETE ON subscription_plan FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
ALTER TABLE tenant_subscription ENABLE ROW LEVEL SECURITY; ALTER TABLE tenant_subscription FORCE ROW LEVEL SECURITY;
CREATE POLICY subscription_platform ON tenant_subscription USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
CREATE POLICY subscription_merchant_read ON tenant_subscription FOR SELECT USING(tenant_id=app_tenant());
-- PostgreSQL SELECT ... FOR SHARE also evaluates UPDATE visibility. Permit row
-- locks, but never a merchant mutation: every new row fails this WITH CHECK.
CREATE POLICY subscription_merchant_lock ON tenant_subscription FOR UPDATE USING(tenant_id=app_tenant()) WITH CHECK(false);
GRANT SELECT,INSERT,UPDATE ON tenant_subscription TO quincai_app;
ALTER TABLE subscription_notice ENABLE ROW LEVEL SECURITY; ALTER TABLE subscription_notice FORCE ROW LEVEL SECURITY;
CREATE POLICY subscription_platform ON subscription_notice USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
CREATE POLICY subscription_merchant_read ON subscription_notice FOR SELECT USING(tenant_id=app_tenant());
CREATE POLICY subscription_merchant_lock ON subscription_notice FOR UPDATE USING(tenant_id=app_tenant()) WITH CHECK(false);
GRANT SELECT,INSERT,UPDATE ON subscription_notice TO quincai_app;
ALTER TABLE subscription_lifecycle_operation ENABLE ROW LEVEL SECURITY; ALTER TABLE subscription_lifecycle_operation FORCE ROW LEVEL SECURITY;
CREATE POLICY subscription_platform ON subscription_lifecycle_operation USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
GRANT SELECT,INSERT ON subscription_lifecycle_operation TO quincai_app;
CREATE TRIGGER immutable_subscription_lifecycle_operation BEFORE UPDATE OR DELETE ON subscription_lifecycle_operation FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TABLE subscription_manual_payment (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES tenant_subscription(tenant_id), invoice_id uuid NOT NULL,
 plan_code text NOT NULL REFERENCES subscription_plan(code), amount_xaf bigint NOT NULL CHECK(amount_xaf>0),
 bank_reference text NOT NULL, evidence_sha256 text NOT NULL CHECK(evidence_sha256 ~ '^[0-9a-f]{64}$'), reason text NOT NULL,
 status text NOT NULL CHECK(status IN('RECORDED','MANUALLY_RECONCILED')),
 recorded_by uuid NOT NULL REFERENCES platform_operator(actor_id), reviewed_by uuid REFERENCES platform_operator(actor_id), reviewed_at timestamptz,
 FOREIGN KEY(tenant_id,invoice_id) REFERENCES subscription_invoice(tenant_id,id),
 CHECK((status='RECORDED' AND reviewed_by IS NULL AND reviewed_at IS NULL) OR
       (status='MANUALLY_RECONCILED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND reviewed_by<>recorded_by))
);
CREATE UNIQUE INDEX subscription_bank_reference ON subscription_manual_payment(lower(trim(bank_reference)));
CREATE TABLE subscription_invoice_activation(invoice_id uuid PRIMARY KEY,tenant_id uuid NOT NULL,payment_id uuid NOT NULL REFERENCES subscription_manual_payment(id),activated_at timestamptz NOT NULL,FOREIGN KEY(tenant_id,invoice_id) REFERENCES subscription_invoice(tenant_id,id));
ALTER TABLE subscription_invoice_activation ENABLE ROW LEVEL SECURITY; ALTER TABLE subscription_invoice_activation FORCE ROW LEVEL SECURITY;
CREATE POLICY subscription_platform ON subscription_invoice_activation USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
GRANT SELECT,INSERT ON subscription_invoice_activation TO quincai_app;
CREATE TRIGGER immutable_subscription_invoice_activation BEFORE UPDATE OR DELETE ON subscription_invoice_activation FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
ALTER TABLE tenant_subscription ADD FOREIGN KEY(activation_payment_id) REFERENCES subscription_manual_payment(id);
ALTER TABLE subscription_manual_payment ENABLE ROW LEVEL SECURITY; ALTER TABLE subscription_manual_payment FORCE ROW LEVEL SECURITY;
CREATE POLICY subscription_platform ON subscription_manual_payment USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
CREATE POLICY subscription_merchant_read ON subscription_manual_payment FOR SELECT USING(tenant_id=app_tenant());
GRANT SELECT,INSERT,UPDATE ON subscription_manual_payment TO quincai_app;
CREATE FUNCTION protect_subscription_lifecycle() RETURNS trigger LANGUAGE plpgsql AS $$
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
   OR invoice.subtotal_xaf<>selected_plan.monthly_price_xaf OR invoice.discount_xaf<>0 OR credits<>0
   OR extract(day from invoice.period_start)<>1 OR invoice.period_end<>((invoice.period_start+interval '1 month')::date-1)
   OR NOT EXISTS(SELECT 1 FROM subscription_invoice_activation a WHERE a.invoice_id=invoice.id AND a.tenant_id=NEW.tenant_id AND a.payment_id=payment.id)
   OR OLD.activation_payment_id IS NOT DISTINCT FROM NEW.activation_payment_id THEN
   RAISE EXCEPTION 'Reviewed full subscription settlement required for activation' USING ERRCODE='23514';END IF;
 ELSIF NEW.plan_code IS DISTINCT FROM OLD.plan_code OR NEW.term_ends_at IS DISTINCT FROM OLD.term_ends_at OR NEW.activation_payment_id IS DISTINCT FROM OLD.activation_payment_id THEN
  RAISE EXCEPTION 'Plan changes require reviewed paid activation' USING ERRCODE='23514';
 END IF;
 RETURN NEW; END $$;
CREATE TRIGGER subscription_lifecycle_guard BEFORE INSERT OR UPDATE ON tenant_subscription FOR EACH ROW EXECUTE FUNCTION protect_subscription_lifecycle();
CREATE FUNCTION protect_subscription_notice() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN
  IF NEW.status<>'RECORDED' OR NEW.recorded_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid OR NEW.delivered_at>now() THEN RAISE EXCEPTION 'Recorded delivery evidence required' USING ERRCODE='23514'; END IF;
 ELSE
  IF OLD.status<>'RECORDED' OR NEW.status<>'APPROVED' OR NEW.reviewed_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid
  OR (to_jsonb(NEW)-'status'-'reviewed_by'-'reviewed_at') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'reviewed_by'-'reviewed_at') THEN RAISE EXCEPTION 'Independent immutable notice review required' USING ERRCODE='23514'; END IF;
 END IF; RETURN NEW; END $$;
CREATE TRIGGER subscription_notice_guard BEFORE INSERT OR UPDATE ON subscription_notice FOR EACH ROW EXECUTE FUNCTION protect_subscription_notice();
CREATE FUNCTION protect_subscription_manual_payment() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE invoice subscription_invoice; paid numeric; credits numeric;
BEGIN
 PERFORM pg_advisory_xact_lock(hashtextextended('subscription-credit:'||NEW.invoice_id::text,0));
 SELECT * INTO invoice FROM subscription_invoice WHERE id=NEW.invoice_id AND tenant_id=NEW.tenant_id AND kind='INVOICE';
 IF invoice.id IS NULL THEN RAISE EXCEPTION 'Subscription invoice required' USING ERRCODE='23514';END IF;
 IF invoice.subtotal_xaf<>(SELECT monthly_price_xaf FROM subscription_plan WHERE code=NEW.plan_code) OR invoice.discount_xaf<>0
 OR extract(day from invoice.period_start)<>1 OR invoice.period_end<>((invoice.period_start+interval '1 month')::date-1) THEN
  RAISE EXCEPTION 'Invoice must match selected monthly plan price and complete calendar period' USING ERRCODE='23514';END IF;
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
CREATE TRIGGER subscription_manual_payment_guard BEFORE INSERT OR UPDATE ON subscription_manual_payment FOR EACH ROW EXECUTE FUNCTION protect_subscription_manual_payment();
-- Entitlements remain inaccessible to ordinary merchant mutations; the control plane can
-- materialize the selected trial plan, keeping the existing publication gate in sync.
CREATE POLICY subscription_platform_write ON subscription_entitlement USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
GRANT INSERT,UPDATE ON subscription_entitlement TO quincai_app;
CREATE FUNCTION guard_subscription_entitlement_writer() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname=current_user AND rolsuper)
 AND NOT EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active) THEN
  RAISE EXCEPTION 'Enrolled control-plane operator required for entitlement writes' USING ERRCODE='42501';
 END IF; RETURN NULL; END $$;
CREATE TRIGGER subscription_entitlement_writer BEFORE INSERT OR UPDATE ON subscription_entitlement FOR EACH STATEMENT EXECUTE FUNCTION guard_subscription_entitlement_writer();
