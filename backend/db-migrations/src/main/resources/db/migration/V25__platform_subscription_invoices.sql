-- SaaS fees are separate from merchant tills, customer AR and provider settlement.
CREATE TABLE subscription_invoice (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES tenant(id),
 actor_id uuid NOT NULL REFERENCES platform_operator(actor_id), request_id uuid NOT NULL,
 request_hash text NOT NULL, kind text NOT NULL CHECK(kind IN ('INVOICE','CREDIT_NOTE')),
 original_id uuid, number text NOT NULL UNIQUE,
 issuer_name text NOT NULL, issuer_tax_reference text NOT NULL,
 customer_name text NOT NULL, customer_tax_reference text NOT NULL,
 description text NOT NULL, jurisdiction text NOT NULL,
 period_start date NOT NULL, period_end date NOT NULL, due_date date NOT NULL,
 subtotal_xaf bigint NOT NULL CHECK(subtotal_xaf>0),
 discount_xaf bigint NOT NULL CHECK(discount_xaf>=0 AND discount_xaf<subtotal_xaf),
 tax_xaf bigint NOT NULL CHECK(tax_xaf>=0),
 total_xaf bigint NOT NULL CHECK(total_xaf>0), created_at timestamptz NOT NULL,
 UNIQUE(tenant_id,id), FOREIGN KEY(tenant_id,original_id) REFERENCES subscription_invoice(tenant_id,id),
 CHECK(period_end>=period_start), CHECK(due_date>=period_start),
 CHECK(total_xaf::numeric=subtotal_xaf::numeric-discount_xaf::numeric+tax_xaf::numeric),
 CHECK((kind='INVOICE' AND original_id IS NULL) OR (kind='CREDIT_NOTE' AND original_id IS NOT NULL))
);
CREATE TABLE subscription_billing_event (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL, invoice_id uuid NOT NULL,
 actor_id uuid NOT NULL REFERENCES platform_operator(actor_id), request_id uuid NOT NULL,
 event_type text NOT NULL CHECK(event_type IN ('SUBSCRIPTION_INVOICE_ISSUED','SUBSCRIPTION_CREDIT_ISSUED')),
 occurred_at timestamptz NOT NULL,
 FOREIGN KEY(tenant_id,invoice_id) REFERENCES subscription_invoice(tenant_id,id),
 UNIQUE(invoice_id,event_type)
);
ALTER TABLE subscription_invoice ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription_invoice FORCE ROW LEVEL SECURITY;
ALTER TABLE subscription_billing_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription_billing_event FORCE ROW LEVEL SECURITY;
CREATE POLICY platform_billing ON subscription_invoice USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
CREATE POLICY platform_billing ON subscription_billing_event USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
CREATE POLICY merchant_billing_read ON subscription_invoice FOR SELECT USING(tenant_id=app_tenant());
GRANT SELECT,INSERT ON subscription_invoice,subscription_billing_event TO quincai_app;
CREATE TRIGGER immutable_subscription_invoice BEFORE UPDATE OR DELETE ON subscription_invoice FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TRIGGER immutable_subscription_billing_event BEFORE UPDATE OR DELETE ON subscription_billing_event FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();

-- Preserve credit conservation even if another application path bypasses this service.
CREATE FUNCTION guard_subscription_credit() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE original subscription_invoice; credited_net numeric; credited_tax numeric;
BEGIN
 IF NEW.kind='CREDIT_NOTE' THEN
  PERFORM pg_advisory_xact_lock(hashtextextended('subscription-credit:'||NEW.original_id::text,0));
  SELECT * INTO original FROM subscription_invoice WHERE id=NEW.original_id AND tenant_id=NEW.tenant_id;
  IF original.id IS NULL OR original.kind<>'INVOICE' OR original.actor_id=NEW.actor_id THEN
   RAISE EXCEPTION 'Independent correction of an original subscription invoice required' USING ERRCODE='23514';
  END IF;
  IF ROW(NEW.issuer_name,NEW.issuer_tax_reference,NEW.customer_name,NEW.customer_tax_reference,NEW.jurisdiction)
      IS DISTINCT FROM ROW(original.issuer_name,original.issuer_tax_reference,original.customer_name,original.customer_tax_reference,original.jurisdiction) THEN
   RAISE EXCEPTION 'Credit identity must match original subscription invoice' USING ERRCODE='23514';
  END IF;
  SELECT coalesce(sum(subtotal_xaf::numeric-discount_xaf::numeric),0),coalesce(sum(tax_xaf::numeric),0)
    INTO credited_net,credited_tax FROM subscription_invoice WHERE original_id=NEW.original_id;
  IF credited_net+NEW.subtotal_xaf::numeric-NEW.discount_xaf::numeric>original.subtotal_xaf::numeric-original.discount_xaf::numeric
     OR credited_tax+NEW.tax_xaf::numeric>original.tax_xaf::numeric THEN
   RAISE EXCEPTION 'Subscription credits exceed original net or tax' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER subscription_credit_guard BEFORE INSERT ON subscription_invoice FOR EACH ROW EXECUTE FUNCTION guard_subscription_credit();
