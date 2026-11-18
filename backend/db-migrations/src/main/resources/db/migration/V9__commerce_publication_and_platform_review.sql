-- Deployment-managed entitlements. Runtime merchant/platform web APIs cannot grant these.
CREATE TABLE subscription_entitlement(tenant_id uuid NOT NULL REFERENCES tenant(id),id uuid NOT NULL UNIQUE,feature text NOT NULL CHECK(feature='COMMERCE'),enabled boolean NOT NULL,expires_at timestamptz NOT NULL,PRIMARY KEY(tenant_id,feature));
ALTER TABLE subscription_entitlement ENABLE ROW LEVEL SECURITY;ALTER TABLE subscription_entitlement FORCE ROW LEVEL SECURITY;
CREATE POLICY own_entitlement ON subscription_entitlement FOR SELECT USING(tenant_id=app_tenant());
GRANT SELECT ON subscription_entitlement TO quincai_app;
-- Anonymous access can inspect only this opaque publication gate, never subscription or tenant identifiers.
CREATE TABLE public_publication_gate(id uuid PRIMARY KEY,enabled boolean NOT NULL,expires_at timestamptz NOT NULL);
ALTER TABLE public_publication_gate ENABLE ROW LEVEL SECURITY;ALTER TABLE public_publication_gate FORCE ROW LEVEL SECURITY;
CREATE POLICY gate_read ON public_publication_gate FOR SELECT USING(true);CREATE POLICY gate_owner_write ON public_publication_gate FOR ALL USING(true) WITH CHECK(true);
GRANT SELECT ON public_publication_gate TO quincai_app;
CREATE FUNCTION refresh_publication_gate() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public,pg_temp AS $$ BEGIN
 IF TG_OP='DELETE' THEN UPDATE public_publication_gate SET enabled=false WHERE id=OLD.id;RETURN OLD;END IF;
 IF TG_OP='UPDATE' AND NEW.id IS DISTINCT FROM OLD.id THEN RAISE EXCEPTION 'Entitlement identifier is immutable';END IF;
 INSERT INTO public_publication_gate VALUES(NEW.id,NEW.enabled,NEW.expires_at) ON CONFLICT(id) DO UPDATE SET enabled=EXCLUDED.enabled,expires_at=EXCLUDED.expires_at;RETURN NEW;END $$;
REVOKE ALL ON FUNCTION refresh_publication_gate() FROM PUBLIC;
CREATE TRIGGER publication_gate AFTER INSERT OR UPDATE OR DELETE ON subscription_entitlement FOR EACH ROW EXECUTE FUNCTION refresh_publication_gate();
-- Merchant-owned publishing aggregates; separate sanitized anonymous projection.
CREATE TABLE commerce_store(tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,slug text NOT NULL UNIQUE CHECK(slug ~ '^[a-z0-9][a-z0-9-]{2,62}$'),display_name text NOT NULL,enabled boolean NOT NULL DEFAULT false,version bigint NOT NULL DEFAULT 0,PRIMARY KEY(tenant_id,company_id,id),FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id));
CREATE TABLE commerce_listing(tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,store_id uuid NOT NULL,product_id uuid NOT NULL,title text NOT NULL,description text NOT NULL,unit text NOT NULL,price_xaf bigint NOT NULL CHECK(price_xaf>=0),published boolean NOT NULL,version bigint NOT NULL DEFAULT 0,PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,company_id,store_id,product_id),FOREIGN KEY(tenant_id,company_id,store_id) REFERENCES commerce_store(tenant_id,company_id,id),FOREIGN KEY(tenant_id,company_id,product_id) REFERENCES catalog_product(tenant_id,company_id,id));
DO $$ DECLARE t text;BEGIN FOREACH t IN ARRAY ARRAY['commerce_store','commerce_listing'] LOOP EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);EXECUTE format('CREATE POLICY scope_isolation ON %I USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())',t);END LOOP;END $$;
GRANT SELECT,INSERT,UPDATE ON commerce_store,commerce_listing TO quincai_app;
-- No tenant, company, branch, private SKU, costs, balances, contact/customer or product IDs appear here.
CREATE TABLE public_store_projection(slug text PRIMARY KEY,display_name text NOT NULL,publication_gate uuid NOT NULL REFERENCES public_publication_gate(id));
CREATE TABLE public_listing_projection(id uuid PRIMARY KEY,slug text NOT NULL REFERENCES public_store_projection(slug) ON DELETE CASCADE,title text NOT NULL,description text NOT NULL,unit text NOT NULL,price_xaf bigint NOT NULL CHECK(price_xaf>=0));
DO $$ DECLARE t text;BEGIN FOREACH t IN ARRAY ARRAY['public_store_projection','public_listing_projection'] LOOP EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);EXECUTE format('CREATE POLICY public_read ON %I FOR SELECT USING(true)',t);EXECUTE format('CREATE POLICY projection_owner_write ON %I FOR ALL USING(true) WITH CHECK(true)',t);END LOOP;END $$;
GRANT SELECT ON public_store_projection,public_listing_projection TO quincai_app;
-- Trigger runs as migration owner, but retains tenant RLS context while reading merchant source.
CREATE FUNCTION refresh_public_store() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public,pg_temp AS $$ DECLARE store commerce_store;gate uuid;BEGIN
 IF TG_TABLE_NAME='commerce_store' THEN store:=NEW; IF TG_OP='UPDATE' AND OLD.slug<>NEW.slug THEN DELETE FROM public_store_projection WHERE slug=OLD.slug;END IF;
 ELSE SELECT * INTO store FROM commerce_store WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND id=NEW.store_id;END IF;
 DELETE FROM public_store_projection WHERE slug=store.slug;
 IF store.enabled THEN
  SELECT id INTO gate FROM subscription_entitlement WHERE tenant_id=store.tenant_id AND feature='COMMERCE' AND enabled AND expires_at>now();
  IF gate IS NULL THEN RAISE EXCEPTION 'Active commerce entitlement required' USING ERRCODE='42501';END IF;
  INSERT INTO public_store_projection VALUES(store.slug,store.display_name,gate);
  INSERT INTO public_listing_projection SELECT l.id,store.slug,l.title,l.description,l.unit,l.price_xaf FROM commerce_listing l WHERE l.tenant_id=store.tenant_id AND l.company_id=store.company_id AND l.store_id=store.id AND l.published;
 END IF;RETURN NEW;END $$;
REVOKE ALL ON FUNCTION refresh_public_store() FROM PUBLIC;
CREATE TRIGGER refresh_store AFTER INSERT OR UPDATE ON commerce_store FOR EACH ROW EXECUTE FUNCTION refresh_public_store();
CREATE TRIGGER refresh_listing AFTER INSERT OR UPDATE ON commerce_listing FOR EACH ROW EXECUTE FUNCTION refresh_public_store();
-- Separate control-plane registry; only privileged deployment/provisioning may enroll operators.
CREATE TABLE platform_operator(actor_id uuid PRIMARY KEY,active boolean NOT NULL DEFAULT true);
CREATE TABLE platform_onboarding_request(id uuid PRIMARY KEY,display_name text NOT NULL,requested_by uuid NOT NULL REFERENCES platform_operator(actor_id),approved_by uuid REFERENCES platform_operator(actor_id),status text NOT NULL CHECK(status IN('REQUESTED','APPROVED_FOR_PROVISIONING')),reason text NOT NULL,version bigint NOT NULL DEFAULT 0,created_at timestamptz NOT NULL DEFAULT now(),approval_operation uuid UNIQUE, CHECK(approved_by IS NULL OR approved_by<>requested_by), CHECK((status='REQUESTED' AND approved_by IS NULL AND approval_operation IS NULL) OR (status='APPROVED_FOR_PROVISIONING' AND approved_by IS NOT NULL AND approval_operation IS NOT NULL)));
ALTER TABLE platform_operator ENABLE ROW LEVEL SECURITY;ALTER TABLE platform_operator FORCE ROW LEVEL SECURITY;
CREATE POLICY operator_self ON platform_operator USING(actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid);
ALTER TABLE platform_onboarding_request ENABLE ROW LEVEL SECURITY;ALTER TABLE platform_onboarding_request FORCE ROW LEVEL SECURITY;
CREATE POLICY platform_only ON platform_onboarding_request USING(EXISTS(SELECT 1 FROM platform_operator o WHERE o.actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND o.active)) WITH CHECK(EXISTS(SELECT 1 FROM platform_operator o WHERE o.actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid AND o.active));
GRANT SELECT ON platform_operator TO quincai_app;GRANT SELECT,INSERT,UPDATE ON platform_onboarding_request TO quincai_app;
CREATE FUNCTION guard_platform_review() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN IF NEW.requested_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid OR NEW.status<>'REQUESTED' OR NEW.approved_by IS NOT NULL THEN RAISE EXCEPTION 'Invalid platform request' USING ERRCODE='42501';END IF;
 ELSE IF OLD.status<>'REQUESTED' OR NEW.id IS DISTINCT FROM OLD.id OR NEW.created_at IS DISTINCT FROM OLD.created_at OR NEW.requested_by IS DISTINCT FROM OLD.requested_by OR NEW.display_name IS DISTINCT FROM OLD.display_name OR NEW.reason IS DISTINCT FROM OLD.reason OR NEW.approved_by IS NULL OR NEW.approval_operation IS NULL OR NEW.approved_by IS DISTINCT FROM nullif(current_setting('app.platform_actor',true),'')::uuid OR NEW.approved_by=NEW.requested_by OR NEW.status<>'APPROVED_FOR_PROVISIONING' THEN RAISE EXCEPTION 'Separate immutable platform review required' USING ERRCODE='42501';END IF;END IF;RETURN NEW;END $$;
CREATE TRIGGER platform_review_guard BEFORE INSERT OR UPDATE ON platform_onboarding_request FOR EACH ROW EXECUTE FUNCTION guard_platform_review();
