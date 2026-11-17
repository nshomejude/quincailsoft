-- Company-owned metadata supports safe single-branch financial closing.
ALTER TABLE company ADD COLUMN branch_count bigint NOT NULL DEFAULT 0 CHECK(branch_count>=0);
-- Migration owner temporarily bypasses its own FORCE policy only inside this migration.
ALTER TABLE company NO FORCE ROW LEVEL SECURITY;
ALTER TABLE branch NO FORCE ROW LEVEL SECURITY;
UPDATE company c SET branch_count=(SELECT count(*) FROM branch b WHERE b.tenant_id=c.tenant_id AND b.company_id=c.id);
ALTER TABLE branch FORCE ROW LEVEL SECURITY;
ALTER TABLE company FORCE ROW LEVEL SECURITY;
CREATE FUNCTION maintain_company_branch_count() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public,pg_temp AS $$
BEGIN
 IF TG_OP='INSERT' THEN
  UPDATE company SET branch_count=branch_count+1 WHERE tenant_id=NEW.tenant_id AND id=NEW.company_id;
  IF NOT FOUND THEN RAISE EXCEPTION 'Company scope required for branch provisioning' USING ERRCODE='42501'; END IF;
  RETURN NEW;
 ELSIF TG_OP='DELETE' THEN
  UPDATE company SET branch_count=branch_count-1 WHERE tenant_id=OLD.tenant_id AND id=OLD.company_id;
  IF NOT FOUND THEN RAISE EXCEPTION 'Company scope required for branch removal' USING ERRCODE='42501'; END IF;
  RETURN OLD;
 ELSE
  IF NEW.tenant_id<>OLD.tenant_id OR NEW.company_id<>OLD.company_id THEN RAISE EXCEPTION 'Branch company ownership is immutable' USING ERRCODE='23514'; END IF;
  RETURN NEW;
 END IF;
END $$;
REVOKE ALL ON FUNCTION maintain_company_branch_count() FROM PUBLIC;
CREATE TRIGGER company_branch_count AFTER INSERT OR DELETE OR UPDATE ON branch FOR EACH ROW EXECUTE FUNCTION maintain_company_branch_count();
-- SELECT FOR SHARE requires one column-level UPDATE privilege. It grants no economic metadata mutation.
ALTER TABLE company ADD COLUMN lock_version bigint NOT NULL DEFAULT 0;
GRANT UPDATE(lock_version) ON company TO quincai_app;
