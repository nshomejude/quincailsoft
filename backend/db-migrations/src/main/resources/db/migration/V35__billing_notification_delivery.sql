-- Backfill must not silently see an empty cross-tenant source under forced RLS.
-- This is migration-only authority; the application role stays NOBYPASSRLS.
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname=current_user AND (rolsuper OR rolbypassrls))
 AND NOT EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active) THEN
  RAISE EXCEPTION 'V35 requires authorized cross-tenant migration visibility: privileged migration role or enrolled platform migration actor; runtime must remain NOBYPASSRLS' USING ERRCODE='42501';
 END IF;
END $$;
ALTER TABLE subscription_billing_event ADD CONSTRAINT subscription_billing_event_tenant_id UNIQUE(tenant_id,id);
CREATE TABLE subscription_notification_delivery (
 event_id uuid PRIMARY KEY,tenant_id uuid NOT NULL,
 status text NOT NULL CHECK(status IN('PENDING','IN_FLIGHT','RETRY','HTTP_ACCEPTED','DEAD')),
 attempts integer NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 100),attempt_budget integer NOT NULL DEFAULT 8 CHECK(attempt_budget BETWEEN 1 AND 100),
 destination_key text,lease_token uuid,lease_expires_at timestamptz,next_attempt_at timestamptz NOT NULL,
 last_outcome text NOT NULL DEFAULT 'PENDING',last_http_status integer,completed_at timestamptz,version bigint NOT NULL DEFAULT 0,
 FOREIGN KEY(tenant_id,event_id) REFERENCES subscription_billing_event(tenant_id,id),
 CHECK((status='IN_FLIGHT' AND lease_token IS NOT NULL AND lease_expires_at IS NOT NULL) OR (status<>'IN_FLIGHT' AND lease_token IS NULL AND lease_expires_at IS NULL)),
 CHECK(last_http_status IS NULL OR last_http_status BETWEEN 100 AND 599)
);
CREATE TABLE subscription_notification_attempt (
 id uuid PRIMARY KEY,tenant_id uuid NOT NULL,event_id uuid NOT NULL,attempt_number integer NOT NULL,
 stage text NOT NULL CHECK(stage IN('CLAIMED','FINISHED','EXPIRED','BLOCKED')),lease_token uuid,
 actor_id uuid NOT NULL REFERENCES platform_operator(actor_id),outcome text NOT NULL,http_status integer,recorded_at timestamptz NOT NULL,
 UNIQUE(event_id,attempt_number,stage),FOREIGN KEY(tenant_id,event_id) REFERENCES subscription_billing_event(tenant_id,id),
 CHECK(http_status IS NULL OR http_status BETWEEN 100 AND 599)
);
CREATE TABLE subscription_notification_redrive (
 id uuid PRIMARY KEY,event_id uuid NOT NULL REFERENCES subscription_notification_delivery(event_id),
 actor_id uuid NOT NULL REFERENCES platform_operator(actor_id),reason text NOT NULL,request_hash text NOT NULL,response_json text NOT NULL,recorded_at timestamptz NOT NULL
);
ALTER TABLE subscription_notification_delivery ENABLE ROW LEVEL SECURITY;ALTER TABLE subscription_notification_delivery FORCE ROW LEVEL SECURITY;
ALTER TABLE subscription_notification_attempt ENABLE ROW LEVEL SECURITY;ALTER TABLE subscription_notification_attempt FORCE ROW LEVEL SECURITY;
ALTER TABLE subscription_notification_redrive ENABLE ROW LEVEL SECURITY;ALTER TABLE subscription_notification_redrive FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_platform ON subscription_notification_delivery USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
CREATE POLICY notification_platform ON subscription_notification_attempt USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
CREATE POLICY notification_platform ON subscription_notification_redrive USING(EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active)) WITH CHECK(actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND EXISTS(SELECT 1 FROM platform_operator WHERE actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND active));
GRANT SELECT,INSERT,UPDATE ON subscription_notification_delivery TO quincai_app;
GRANT SELECT,INSERT ON subscription_notification_attempt,subscription_notification_redrive TO quincai_app;
CREATE TRIGGER immutable_notification_attempt BEFORE UPDATE OR DELETE ON subscription_notification_attempt FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TRIGGER immutable_notification_redrive BEFORE UPDATE OR DELETE ON subscription_notification_redrive FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE INDEX subscription_notification_due ON subscription_notification_delivery(next_attempt_at,event_id) WHERE status IN('PENDING','RETRY','IN_FLIGHT');
CREATE FUNCTION initialize_subscription_notification() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 INSERT INTO subscription_notification_delivery(event_id,tenant_id,status,next_attempt_at)VALUES(NEW.id,NEW.tenant_id,'PENDING',NEW.occurred_at);RETURN NEW;
END $$;
CREATE TRIGGER subscription_notification_outbox AFTER INSERT ON subscription_billing_event FOR EACH ROW EXECUTE FUNCTION initialize_subscription_notification();
INSERT INTO subscription_notification_delivery(event_id,tenant_id,status,next_attempt_at)SELECT id,tenant_id,'PENDING',occurred_at FROM subscription_billing_event;
CREATE FUNCTION protect_subscription_notification_delivery() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF ROW(NEW.event_id,NEW.tenant_id) IS DISTINCT FROM ROW(OLD.event_id,OLD.tenant_id) OR NEW.attempts<OLD.attempts OR NEW.attempts>OLD.attempts+1 OR NEW.attempt_budget<OLD.attempt_budget OR OLD.status='HTTP_ACCEPTED' THEN
  RAISE EXCEPTION 'Invalid notification identity or terminal delivery mutation' USING ERRCODE='23514';END IF;
 RETURN NEW;END $$;
CREATE TRIGGER notification_delivery_guard BEFORE UPDATE ON subscription_notification_delivery FOR EACH ROW EXECUTE FUNCTION protect_subscription_notification_delivery();
