-- §§5.1/8.1: typed, reviewable onboarding preparation. No incomplete tenant is activated.
CREATE TABLE platform_onboarding_configuration (
 request_id uuid PRIMARY KEY REFERENCES platform_onboarding_request(id),
 legal_name text NOT NULL CHECK(length(trim(legal_name)) BETWEEN 1 AND 200),
 registration_number text NOT NULL CHECK(length(trim(registration_number)) BETWEEN 1 AND 100),
 address text NOT NULL CHECK(length(trim(address)) BETWEEN 1 AND 500),
 owner_subject uuid NOT NULL, owner_email text NOT NULL CHECK(length(owner_email) BETWEEN 3 AND 254),
 region text NOT NULL CHECK(length(region) BETWEEN 1 AND 80), timezone text NOT NULL,
 locale text NOT NULL CHECK(locale IN ('en','fr')), document_prefix text NOT NULL CHECK(document_prefix ~ '^[A-Z0-9-]{1,16}$'),
 opening_mode text NOT NULL CHECK(opening_mode IN ('EMPTY','CSV','SUPERVISED')),
 privacy_version text NOT NULL, terms_version text NOT NULL, plan_code text NOT NULL,
 configured_by uuid NOT NULL REFERENCES platform_operator(actor_id),
 approved_by uuid REFERENCES platform_operator(actor_id),
 status text NOT NULL CHECK(status IN ('AWAITING_IDENTITY','IDENTITY_VERIFIED','APPROVED_CONFIGURATION')),
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 identity_verified_at timestamptz, identity_issuer text, created_at timestamptz NOT NULL,
 CHECK(approved_by IS NULL OR approved_by<>configured_by),
 CHECK((status='AWAITING_IDENTITY' AND identity_verified_at IS NULL AND identity_issuer IS NULL AND approved_by IS NULL)
    OR (status='IDENTITY_VERIFIED' AND identity_verified_at IS NOT NULL AND identity_issuer IS NOT NULL AND approved_by IS NULL)
    OR (status='APPROVED_CONFIGURATION' AND identity_verified_at IS NOT NULL AND identity_issuer IS NOT NULL AND approved_by IS NOT NULL))
);
CREATE TABLE platform_onboarding_command (
 operation_id uuid PRIMARY KEY, request_id uuid NOT NULL REFERENCES platform_onboarding_request(id),
 actor_id uuid NOT NULL REFERENCES platform_operator(actor_id), request_hash text NOT NULL CHECK(length(request_hash)=64),
 response_text text NOT NULL, created_at timestamptz NOT NULL
);
CREATE TABLE platform_onboarding_event (
 id uuid PRIMARY KEY, request_id uuid NOT NULL REFERENCES platform_onboarding_request(id),
 operation_id uuid NOT NULL UNIQUE REFERENCES platform_onboarding_command(operation_id) DEFERRABLE INITIALLY DEFERRED,
 actor_id uuid NOT NULL REFERENCES platform_operator(actor_id),
 event_type text NOT NULL CHECK(event_type IN ('OnboardingConfigured.v1','OnboardingIdentityVerified.v1','OnboardingConfigurationApproved.v1')),
 configuration_version bigint NOT NULL CHECK(configuration_version>=0), created_at timestamptz NOT NULL
);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['platform_onboarding_configuration','platform_onboarding_command','platform_onboarding_event'] LOOP
  EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY platform_only ON %I USING(EXISTS(SELECT 1 FROM platform_operator o WHERE o.actor_id=nullif(current_setting(''app.platform_actor'',true),'''')::uuid AND o.active)) WITH CHECK(EXISTS(SELECT 1 FROM platform_operator o WHERE o.actor_id=nullif(current_setting(''app.platform_actor'',true),'''')::uuid AND o.active))',t);
 END LOOP;
END $$;
GRANT SELECT,INSERT,UPDATE ON platform_onboarding_configuration TO quincai_app;
GRANT SELECT,INSERT ON platform_onboarding_command,platform_onboarding_event TO quincai_app;
CREATE TRIGGER immutable_onboarding_command BEFORE UPDATE OR DELETE ON platform_onboarding_command FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TRIGGER immutable_onboarding_event BEFORE UPDATE OR DELETE ON platform_onboarding_event FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE FUNCTION protect_onboarding_configuration() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.status='APPROVED_CONFIGURATION' OR NEW.request_id<>OLD.request_id OR NEW.configured_by<>OLD.configured_by
 OR (NEW.legal_name,NEW.registration_number,NEW.address,NEW.owner_subject,NEW.owner_email,NEW.region,NEW.timezone,NEW.locale,NEW.document_prefix,NEW.opening_mode,NEW.privacy_version,NEW.terms_version,NEW.plan_code,NEW.created_at)
 IS DISTINCT FROM (OLD.legal_name,OLD.registration_number,OLD.address,OLD.owner_subject,OLD.owner_email,OLD.region,OLD.timezone,OLD.locale,OLD.document_prefix,OLD.opening_mode,OLD.privacy_version,OLD.terms_version,OLD.plan_code,OLD.created_at)
 THEN RAISE EXCEPTION 'Onboarding configuration identity is immutable'; END IF;
 IF NOT ((OLD.status='AWAITING_IDENTITY' AND NEW.status='IDENTITY_VERIFIED') OR (OLD.status='IDENTITY_VERIFIED' AND NEW.status='APPROVED_CONFIGURATION'))
 THEN RAISE EXCEPTION 'Illegal onboarding configuration transition'; END IF;
 IF OLD.identity_verified_at IS NOT NULL AND (NEW.identity_verified_at,NEW.identity_issuer) IS DISTINCT FROM (OLD.identity_verified_at,OLD.identity_issuer)
 THEN RAISE EXCEPTION 'Owner identity evidence is immutable'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER onboarding_configuration_transition BEFORE UPDATE ON platform_onboarding_configuration FOR EACH ROW EXECUTE FUNCTION protect_onboarding_configuration();
