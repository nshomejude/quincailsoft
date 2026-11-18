-- Bounded import/export/recovery control records; no generic business-record store.
ALTER TABLE supplier ADD COLUMN contact_person text NOT NULL DEFAULT '', ADD COLUMN address text NOT NULL DEFAULT '', ADD COLUMN tax_reference text NOT NULL DEFAULT '', ADD COLUMN payment_terms_days integer NOT NULL DEFAULT 0 CHECK(payment_terms_days BETWEEN 0 AND 3650), ADD COLUMN bank_reference text NOT NULL DEFAULT '';
CREATE TABLE operation_import_preview(
 tenant_id uuid NOT NULL,
 company_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 id uuid NOT NULL,
 actor_id uuid NOT NULL,
 version bigint NOT NULL DEFAULT 0,
 entity text NOT NULL,
 digest text NOT NULL,
 expires_at timestamptz NOT NULL,
 committed boolean NOT NULL,
 created_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id),
 CHECK(entity IN('PRODUCT','CUSTOMER','SUPPLIER','CATEGORY','UNIT'))
);
ALTER TABLE operation_import_preview ENABLE ROW LEVEL SECURITY;ALTER TABLE operation_import_preview FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON operation_import_preview USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch() AND actor_id=app_actor()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch() AND actor_id=app_actor());
GRANT SELECT,INSERT,UPDATE ON operation_import_preview TO quincai_app;
CREATE TABLE operation_import_row(
 tenant_id uuid NOT NULL,
 company_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 id uuid NOT NULL,
 actor_id uuid NOT NULL,
 version bigint NOT NULL DEFAULT 0,
 preview_id uuid NOT NULL,
 row_number integer NOT NULL,
 name text NOT NULL,
 code text NOT NULL,
 base_unit text NOT NULL,
 selling_price_xaf text NOT NULL,
 tax_basis_points text NOT NULL,
 relation_id uuid,
 brand text NOT NULL,
 barcode text NOT NULL,
 description text NOT NULL,
 pack_size text NOT NULL,
 reorder_level text NOT NULL,
 track_stock boolean NOT NULL,
 track_serial boolean NOT NULL,
 track_expiry boolean NOT NULL,
 customer_type text NOT NULL,
 contact_person text NOT NULL,
 phone text NOT NULL,
 email text NOT NULL,
 address text NOT NULL,
 tax_reference text NOT NULL,
 credit_limit_xaf text NOT NULL,
 payment_terms_days integer NOT NULL,
 consent boolean NOT NULL,
 bank_reference text NOT NULL,
 symbol text NOT NULL,
 conversion_factor text NOT NULL,
 rounding_precision integer NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,preview_id) REFERENCES operation_import_preview(tenant_id,company_id,id),
 UNIQUE(tenant_id,company_id,preview_id,row_number)
);
ALTER TABLE operation_import_row ENABLE ROW LEVEL SECURITY;ALTER TABLE operation_import_row FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON operation_import_row USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch() AND actor_id=app_actor()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch() AND actor_id=app_actor());
GRANT SELECT,INSERT ON operation_import_row TO quincai_app;
CREATE TRIGGER immutable_operation_fact BEFORE UPDATE OR DELETE ON operation_import_row FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TABLE operation_export_job(
 tenant_id uuid NOT NULL,
 company_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 id uuid NOT NULL,
 actor_id uuid NOT NULL,
 version bigint NOT NULL DEFAULT 0,
 entity text NOT NULL,
 status text NOT NULL,
 row_count integer NOT NULL,
 digest text NOT NULL,
 content text NOT NULL,
 created_at timestamptz NOT NULL,
 expires_at timestamptz NOT NULL,
 authorization_epoch bigint NOT NULL,
 failure text NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id),
 CHECK(status IN('QUEUED','READY','FAILED','CANCELLED')),
 CHECK(row_count BETWEEN 0 AND 5000),
 CHECK(entity IN('PRODUCT','CUSTOMER','SUPPLIER','CATEGORY','UNIT'))
);
ALTER TABLE operation_export_job ENABLE ROW LEVEL SECURITY;ALTER TABLE operation_export_job FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON operation_export_job USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch() AND actor_id=app_actor()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch() AND actor_id=app_actor());
GRANT SELECT,INSERT,UPDATE ON operation_export_job TO quincai_app;
CREATE TABLE operation_notification(
 tenant_id uuid NOT NULL,
 company_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 id uuid NOT NULL,
 actor_id uuid NOT NULL,
 version bigint NOT NULL DEFAULT 0,
 kind text NOT NULL,
 message text NOT NULL,
 source_id uuid NOT NULL,
 created_at timestamptz NOT NULL,
 read_at timestamptz,
 PRIMARY KEY(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id)
);
ALTER TABLE operation_notification ENABLE ROW LEVEL SECURITY;ALTER TABLE operation_notification FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON operation_notification USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch() AND actor_id=app_actor()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch() AND actor_id=app_actor());
GRANT SELECT,INSERT,UPDATE ON operation_notification TO quincai_app;
CREATE TABLE operation_backup_manifest(
 tenant_id uuid NOT NULL,
 company_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 id uuid NOT NULL,
 actor_id uuid NOT NULL,
 version bigint NOT NULL DEFAULT 0,
 digest text NOT NULL,
 created_at timestamptz NOT NULL,
 reason text NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id)
);
ALTER TABLE operation_backup_manifest ENABLE ROW LEVEL SECURITY;ALTER TABLE operation_backup_manifest FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON operation_backup_manifest USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
GRANT SELECT,INSERT ON operation_backup_manifest TO quincai_app;
CREATE TRIGGER immutable_operation_fact BEFORE UPDATE OR DELETE ON operation_backup_manifest FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TABLE operation_manifest_section(
 tenant_id uuid NOT NULL,
 company_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 id uuid NOT NULL,
 actor_id uuid NOT NULL,
 version bigint NOT NULL DEFAULT 0,
 manifest_id uuid NOT NULL,
 name text NOT NULL,
 row_count bigint NOT NULL,
 digest text NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,manifest_id) REFERENCES operation_backup_manifest(tenant_id,company_id,id),
 UNIQUE(tenant_id,company_id,manifest_id,name)
);
ALTER TABLE operation_manifest_section ENABLE ROW LEVEL SECURITY;ALTER TABLE operation_manifest_section FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON operation_manifest_section USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
GRANT SELECT,INSERT ON operation_manifest_section TO quincai_app;
CREATE TRIGGER immutable_operation_fact BEFORE UPDATE OR DELETE ON operation_manifest_section FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE TABLE operation_recovery_rehearsal(
 tenant_id uuid NOT NULL,
 company_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 id uuid NOT NULL,
 actor_id uuid NOT NULL,
 version bigint NOT NULL DEFAULT 0,
 backup_id uuid NOT NULL,
 status text NOT NULL,
 current_digest text NOT NULL,
 differences text NOT NULL,
 reason text NOT NULL,
 created_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,backup_id) REFERENCES operation_backup_manifest(tenant_id,company_id,id),
 CHECK(status IN('MATCH_REVIEW_REQUIRED','DIFFERENCES_REVIEW_REQUIRED'))
);
ALTER TABLE operation_recovery_rehearsal ENABLE ROW LEVEL SECURITY;ALTER TABLE operation_recovery_rehearsal FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON operation_recovery_rehearsal USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch());
GRANT SELECT,INSERT ON operation_recovery_rehearsal TO quincai_app;
CREATE TRIGGER immutable_operation_fact BEFORE UPDATE OR DELETE ON operation_recovery_rehearsal FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
CREATE UNIQUE INDEX one_queued_export_per_tenant ON operation_export_job(tenant_id) WHERE status='QUEUED';
CREATE FUNCTION protect_import_preview() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.committed OR NOT NEW.committed OR ROW(NEW.tenant_id,NEW.company_id,NEW.branch_id,NEW.actor_id,NEW.id,NEW.entity,NEW.digest,NEW.expires_at,NEW.created_at) IS DISTINCT FROM ROW(OLD.tenant_id,OLD.company_id,OLD.branch_id,OLD.actor_id,OLD.id,OLD.entity,OLD.digest,OLD.expires_at,OLD.created_at) THEN RAISE EXCEPTION 'Import preview is immutable except single commit';END IF;RETURN NEW;END $$;
CREATE TRIGGER protect_preview BEFORE UPDATE ON operation_import_preview FOR EACH ROW EXECUTE FUNCTION protect_import_preview();
CREATE FUNCTION protect_export_job() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.status<>'QUEUED' OR NEW.status='QUEUED' OR ROW(NEW.tenant_id,NEW.company_id,NEW.branch_id,NEW.actor_id,NEW.id,NEW.entity,NEW.created_at,NEW.expires_at,NEW.authorization_epoch) IS DISTINCT FROM ROW(OLD.tenant_id,OLD.company_id,OLD.branch_id,OLD.actor_id,OLD.id,OLD.entity,OLD.created_at,OLD.expires_at,OLD.authorization_epoch) THEN RAISE EXCEPTION 'Export is immutable after completion';END IF;RETURN NEW;END $$;
CREATE TRIGGER protect_export BEFORE UPDATE ON operation_export_job FOR EACH ROW EXECUTE FUNCTION protect_export_job();
CREATE FUNCTION protect_notification() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.read_at IS NOT NULL OR NEW.read_at IS NULL OR ROW(NEW.tenant_id,NEW.company_id,NEW.branch_id,NEW.actor_id,NEW.id,NEW.kind,NEW.message,NEW.source_id,NEW.created_at) IS DISTINCT FROM ROW(OLD.tenant_id,OLD.company_id,OLD.branch_id,OLD.actor_id,OLD.id,OLD.kind,OLD.message,OLD.source_id,OLD.created_at) THEN RAISE EXCEPTION 'Notification content is immutable';END IF;RETURN NEW;END $$;
CREATE TRIGGER protect_notification BEFORE UPDATE ON operation_notification FOR EACH ROW EXECUTE FUNCTION protect_notification();
