-- Authoritative tenant-wide resource counts. Payroll employees do not consume login seats.
CREATE TABLE subscription_quota_state (
 tenant_id uuid NOT NULL REFERENCES tenant(id),resource text NOT NULL CHECK(resource IN('STAFF_SEATS','BRANCHES','WAREHOUSES')),
 used bigint NOT NULL DEFAULT 0 CHECK(used>=0),reserved bigint NOT NULL DEFAULT 0 CHECK(reserved>=0),
 PRIMARY KEY(tenant_id,resource)
);
CREATE TABLE subscription_seat_reference (
 tenant_id uuid NOT NULL REFERENCES tenant(id),actor_id uuid NOT NULL,active_memberships bigint NOT NULL CHECK(active_memberships>=0),
 PRIMARY KEY(tenant_id,actor_id)
);
CREATE TABLE subscription_quota_reservation (
 tenant_id uuid NOT NULL REFERENCES tenant(id),id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,
 actor_id uuid NOT NULL,resource text NOT NULL CHECK(resource IN('STAFF_SEATS','BRANCHES','WAREHOUSES')),
 subject_id uuid NOT NULL,subject_key text NOT NULL,units bigint NOT NULL CHECK(units BETWEEN 0 AND 1),
 status text NOT NULL CHECK(status IN('RESERVED','COMMITTED','RELEASED')),created_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,id),FOREIGN KEY(tenant_id,company_id,branch_id)REFERENCES branch(tenant_id,company_id,id)
);
CREATE UNIQUE INDEX quota_subject_hold ON subscription_quota_reservation(tenant_id,resource,subject_key) WHERE status='RESERVED';
CREATE TABLE subscription_quota_operation (
 tenant_id uuid NOT NULL REFERENCES tenant(id),operation_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,actor_id uuid NOT NULL,
 request_hash text NOT NULL CHECK(length(request_hash)=64),response_text text NOT NULL,created_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,operation_id),FOREIGN KEY(tenant_id,company_id,branch_id)REFERENCES branch(tenant_id,company_id,id)
);
DO $$ DECLARE t text;BEGIN
 FOREACH t IN ARRAY ARRAY['subscription_quota_state','subscription_seat_reference','subscription_quota_reservation','subscription_quota_operation'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 IF t IN('subscription_quota_reservation','subscription_quota_operation') THEN
 EXECUTE format('CREATE POLICY own_scope ON %I USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())',t);
 ELSE
 EXECUTE format('CREATE POLICY own_tenant ON %I USING(tenant_id=app_tenant()) WITH CHECK(tenant_id=app_tenant())',t);
 END IF;
 END LOOP;
END $$;
GRANT SELECT ON subscription_quota_state TO quincai_app;
GRANT SELECT,INSERT ON subscription_quota_reservation,subscription_quota_operation TO quincai_app;
GRANT UPDATE(status) ON subscription_quota_reservation TO quincai_app;
CREATE TRIGGER quota_operation_immutable BEFORE UPDATE OR DELETE ON subscription_quota_operation FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();

-- Backfill under migration-owner authority, then restore FORCE before runtime can use the tables.
ALTER TABLE tenant NO FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_memberships NO FORCE ROW LEVEL SECURITY;
ALTER TABLE branch NO FORCE ROW LEVEL SECURITY;
ALTER TABLE warehouse NO FORCE ROW LEVEL SECURITY;
ALTER TABLE subscription_quota_state NO FORCE ROW LEVEL SECURITY;
ALTER TABLE subscription_seat_reference NO FORCE ROW LEVEL SECURITY;
INSERT INTO subscription_seat_reference SELECT tenant_id,actor_id,count(*) FROM tenant_memberships WHERE active GROUP BY tenant_id,actor_id;
INSERT INTO subscription_quota_state(tenant_id,resource,used)
 SELECT t.id,'STAFF_SEATS',(SELECT count(*) FROM subscription_seat_reference s WHERE s.tenant_id=t.id AND s.active_memberships>0) FROM tenant t
 UNION ALL SELECT t.id,'BRANCHES',(SELECT count(*) FROM branch b WHERE b.tenant_id=t.id) FROM tenant t
 UNION ALL SELECT t.id,'WAREHOUSES',(SELECT count(*) FROM warehouse w WHERE w.tenant_id=t.id AND w.active) FROM tenant t;
ALTER TABLE tenant FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_memberships FORCE ROW LEVEL SECURITY;
ALTER TABLE branch FORCE ROW LEVEL SECURITY;
ALTER TABLE warehouse FORCE ROW LEVEL SECURITY;
ALTER TABLE subscription_quota_state FORCE ROW LEVEL SECURITY;
ALTER TABLE subscription_seat_reference FORCE ROW LEVEL SECURITY;

CREATE FUNCTION subscription_quota_limit(t uuid,r text) RETURNS bigint LANGUAGE plpgsql STABLE AS $$ DECLARE lim bigint; BEGIN
 IF t IS DISTINCT FROM app_tenant() THEN RAISE EXCEPTION 'Tenant scope required' USING ERRCODE='42501';END IF;
 SELECT CASE r WHEN 'STAFF_SEATS' THEN p.seat_limit WHEN 'BRANCHES' THEN p.branch_limit WHEN 'WAREHOUSES' THEN p.warehouse_limit END INTO lim
 FROM tenant_subscription s JOIN subscription_plan p ON p.code=s.plan_code WHERE s.tenant_id=t;
 IF FOUND THEN RETURN lim;END IF;
 SELECT CASE r WHEN 'STAFF_SEATS' THEN plan.seat_limit WHEN 'BRANCHES' THEN plan.branch_limit WHEN 'WAREHOUSES' THEN plan.warehouse_limit END INTO lim
 FROM platform_provisioning boot JOIN platform_onboarding_configuration c ON c.request_id=boot.request_id JOIN subscription_plan plan ON plan.code=c.plan_code
 WHERE boot.tenant_id=t AND boot.status='PROVISIONING' AND boot.id=nullif(current_setting('app.provisioning_id',true),'')::uuid
 AND boot.actor_id=nullif(current_setting('app.platform_actor',true),'')::uuid;
 RETURN lim; -- NULL explicitly means no SaaS contract; retain established legacy/perpetual authorization behavior.
END $$;

CREATE FUNCTION quota_resource_lock(t uuid,r text) RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path=public,pg_temp AS $$ BEGIN
 IF t IS DISTINCT FROM app_tenant() OR r NOT IN('STAFF_SEATS','BRANCHES','WAREHOUSES') THEN RAISE EXCEPTION 'Scoped quota required' USING ERRCODE='42501';END IF;
 -- Freeze the effective subscribed plan while admitting usage; a concurrent plan transition waits.
 PERFORM 1 FROM tenant_subscription WHERE tenant_id=t FOR SHARE;
 PERFORM pg_advisory_xact_lock(hashtextextended('tenant-quota:'||t||':'||r,0));
 INSERT INTO subscription_quota_state(tenant_id,resource)VALUES(t,r) ON CONFLICT DO NOTHING;
 PERFORM 1 FROM subscription_quota_state WHERE tenant_id=t AND resource=r FOR UPDATE;
END $$;
REVOKE ALL ON FUNCTION quota_resource_lock(uuid,text) FROM PUBLIC;GRANT EXECUTE ON FUNCTION quota_resource_lock(uuid,text) TO quincai_app;

CREATE FUNCTION quota_reservation_guard() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public,pg_temp AS $$
DECLARE lim bigint;u bigint;h bigint;ref bigint; BEGIN
 PERFORM quota_resource_lock(NEW.tenant_id,NEW.resource);
 IF TG_OP='INSERT' THEN
  IF NEW.status<>'RESERVED' OR NEW.actor_id IS DISTINCT FROM app_actor() OR NEW.company_id IS DISTINCT FROM app_company() OR NEW.branch_id IS DISTINCT FROM app_branch()
  THEN RAISE EXCEPTION 'Scoped reservation actor required' USING ERRCODE='42501';END IF;
  lim:=subscription_quota_limit(NEW.tenant_id,NEW.resource);IF lim IS NULL THEN RAISE EXCEPTION 'Explicit SaaS contract required for capacity reservation' USING ERRCODE='23514';END IF;
  IF NEW.subject_key IS DISTINCT FROM (CASE WHEN NEW.resource='STAFF_SEATS' THEN NEW.subject_id::text ELSE NEW.company_id::text||':'||NEW.subject_id::text END) THEN RAISE EXCEPTION 'Canonical quota subject required';END IF;
  IF NEW.resource='STAFF_SEATS' THEN SELECT active_memberships INTO ref FROM subscription_seat_reference WHERE tenant_id=NEW.tenant_id AND actor_id=NEW.subject_id;NEW.units:=CASE WHEN coalesce(ref,0)>0 THEN 0 ELSE 1 END;
  ELSE NEW.units:=1;END IF;
  SELECT used,reserved INTO u,h FROM subscription_quota_state WHERE tenant_id=NEW.tenant_id AND resource=NEW.resource;
  IF NEW.units>0 AND u::numeric+h::numeric+NEW.units>lim THEN RAISE EXCEPTION 'Licensed capacity exhausted' USING ERRCODE='23514';END IF;
  UPDATE subscription_quota_state SET reserved=reserved+NEW.units WHERE tenant_id=NEW.tenant_id AND resource=NEW.resource;
 ELSE
  IF (to_jsonb(NEW)-'status') IS DISTINCT FROM (to_jsonb(OLD)-'status') OR OLD.status<>'RESERVED' OR NEW.status NOT IN('COMMITTED','RELEASED') THEN RAISE EXCEPTION 'Reservation identity/state is immutable';END IF;
  IF NEW.status='COMMITTED' AND (pg_trigger_depth()<2 OR nullif(current_setting('app.quota_consuming',true),'') IS DISTINCT FROM OLD.id::text) THEN RAISE EXCEPTION 'Only owning resource change can commit capacity' USING ERRCODE='42501';END IF;
  UPDATE subscription_quota_state SET reserved=reserved-OLD.units WHERE tenant_id=OLD.tenant_id AND resource=OLD.resource;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER quota_reservation_guard BEFORE INSERT OR UPDATE ON subscription_quota_reservation FOR EACH ROW EXECUTE FUNCTION quota_reservation_guard();
REVOKE ALL ON FUNCTION quota_reservation_guard() FROM PUBLIC;

CREATE FUNCTION quota_source_change() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public,pg_temp AS $$
DECLARE t uuid;r text;subject uuid;k text;comp uuid;before_active boolean;after_active boolean;delta bigint:=0;refs bigint;lim bigint;u bigint;h bigint;hold subscription_quota_reservation;bound uuid; BEGIN
 t:=CASE WHEN TG_OP='DELETE' THEN OLD.tenant_id ELSE NEW.tenant_id END;
 -- Administrative migration fixtures may omit scope; runtime source RLS never can. Set only the row's tenant for this narrow counter function.
 IF t IS DISTINCT FROM app_tenant() THEN
  IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND rolsuper) THEN PERFORM set_config('app.tenant_id',t::text,true);
  ELSE RAISE EXCEPTION 'Resource tenant scope required' USING ERRCODE='42501';END IF;
 END IF;
 IF TG_OP='UPDATE' AND (NEW.tenant_id,NEW.company_id) IS DISTINCT FROM (OLD.tenant_id,OLD.company_id) THEN RAISE EXCEPTION 'Quota resource ownership cannot change';END IF;
 IF TG_TABLE_NAME='tenant_memberships' THEN
  r:='STAFF_SEATS';subject:=CASE WHEN TG_OP='DELETE' THEN OLD.actor_id ELSE NEW.actor_id END;k:=subject::text;
  IF TG_OP='UPDATE' AND NEW.actor_id<>OLD.actor_id THEN RAISE EXCEPTION 'Login identity cannot change';END IF;
  before_active:=TG_OP<>'INSERT' AND OLD.active;after_active:=TG_OP<>'DELETE' AND NEW.active;
 ELSIF TG_TABLE_NAME='branch' THEN
  r:='BRANCHES';subject:=CASE WHEN TG_OP='DELETE' THEN OLD.id ELSE NEW.id END;comp:=CASE WHEN TG_OP='DELETE' THEN OLD.company_id ELSE NEW.company_id END;k:=comp::text||':'||subject::text;
  before_active:=TG_OP<>'INSERT';after_active:=TG_OP<>'DELETE';
 ELSE
  r:='WAREHOUSES';subject:=CASE WHEN TG_OP='DELETE' THEN OLD.id ELSE NEW.id END;comp:=CASE WHEN TG_OP='DELETE' THEN OLD.company_id ELSE NEW.company_id END;k:=comp::text||':'||subject::text;
  before_active:=TG_OP<>'INSERT' AND OLD.active;after_active:=TG_OP<>'DELETE' AND NEW.active;
 END IF;
 IF before_active=after_active THEN RETURN coalesce(NEW,OLD);END IF;
 PERFORM quota_resource_lock(t,r);
 IF r='STAFF_SEATS' THEN
  SELECT active_memberships INTO refs FROM subscription_seat_reference WHERE tenant_id=t AND actor_id=subject FOR UPDATE;refs:=coalesce(refs,0);
  IF after_active THEN delta:=CASE WHEN refs=0 THEN 1 ELSE 0 END;refs:=refs+1;ELSE IF refs<1 THEN RAISE EXCEPTION 'Seat reference invariant failed';END IF;refs:=refs-1;delta:=CASE WHEN refs=0 THEN -1 ELSE 0 END;END IF;
  INSERT INTO subscription_seat_reference(tenant_id,actor_id,active_memberships)VALUES(t,subject,refs)ON CONFLICT(tenant_id,actor_id)DO UPDATE SET active_memberships=excluded.active_memberships;
 ELSE delta:=CASE WHEN after_active THEN 1 ELSE -1 END;END IF;
 SELECT used,reserved INTO u,h FROM subscription_quota_state WHERE tenant_id=t AND resource=r;
 bound:=nullif(current_setting('app.quota_reservation_id',true),'')::uuid;
 IF after_active AND bound IS NOT NULL THEN
  SELECT * INTO hold FROM subscription_quota_reservation WHERE tenant_id=t AND id=bound FOR UPDATE;
  IF hold.id IS NULL OR hold.status<>'RESERVED' OR hold.resource<>r OR hold.subject_key<>k OR hold.units<>delta THEN RAISE EXCEPTION 'Capacity hold does not match actual resource change';END IF;
  PERFORM set_config('app.quota_consuming',hold.id::text,true);UPDATE subscription_quota_reservation SET status='COMMITTED' WHERE tenant_id=t AND id=bound;PERFORM set_config('app.quota_consuming','',true);
  h:=h-hold.units;
 END IF;
 lim:=subscription_quota_limit(t,r);
 IF delta>0 AND lim IS NOT NULL AND u::numeric+h::numeric+delta>lim THEN RAISE EXCEPTION 'Licensed capacity exhausted' USING ERRCODE='23514';END IF;
 UPDATE subscription_quota_state SET used=used+delta WHERE tenant_id=t AND resource=r;
 RETURN coalesce(NEW,OLD);
END $$;
REVOKE ALL ON FUNCTION quota_source_change() FROM PUBLIC;
CREATE TRIGGER quota_membership AFTER INSERT OR UPDATE OR DELETE ON tenant_memberships FOR EACH ROW EXECUTE FUNCTION quota_source_change();
CREATE TRIGGER quota_branch AFTER INSERT OR UPDATE OR DELETE ON branch FOR EACH ROW EXECUTE FUNCTION quota_source_change();
CREATE TRIGGER quota_warehouse AFTER INSERT OR UPDATE OR DELETE ON warehouse FOR EACH ROW EXECUTE FUNCTION quota_source_change();
