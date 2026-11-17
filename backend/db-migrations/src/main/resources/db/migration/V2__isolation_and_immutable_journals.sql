-- All context comes from verified identity, set transaction-locally by the API.
-- Functions are SECURITY INVOKER; no privileged tenant-data bypass.
CREATE FUNCTION app_tenant() RETURNS uuid LANGUAGE sql STABLE AS $$ SELECT nullif(current_setting('app.tenant_id',true),'')::uuid $$;
CREATE FUNCTION app_company() RETURNS uuid LANGUAGE sql STABLE AS $$ SELECT nullif(current_setting('app.company_id',true),'')::uuid $$;
CREATE FUNCTION app_branch() RETURNS uuid LANGUAGE sql STABLE AS $$ SELECT nullif(current_setting('app.branch_id',true),'')::uuid $$;
CREATE FUNCTION app_actor() RETURNS uuid LANGUAGE sql STABLE AS $$ SELECT nullif(current_setting('app.actor_id',true),'')::uuid $$;
DO $$ DECLARE t text; predicate text; BEGIN
 FOR t IN SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename<>'flyway_schema_history' LOOP
  predicate := 'tenant_id=app_tenant()';
  IF t='tenant' THEN predicate := 'id=app_tenant()';
  ELSIF t='tenant_memberships' THEN predicate := predicate || ' AND actor_id=app_actor()';
  ELSIF t='company' THEN predicate := predicate || ' AND id=app_company()';
  ELSIF t IN ('catalog_product','customer','financial_period') THEN predicate := predicate || ' AND company_id=app_company()';
  ELSIF t='branch' THEN predicate := predicate || ' AND company_id=app_company() AND id=app_branch()';
  ELSIF t='sale_line' THEN predicate := predicate || ' AND company_id=app_company() AND EXISTS (SELECT 1 FROM sale s WHERE s.tenant_id=sale_line.tenant_id AND s.company_id=sale_line.company_id AND s.id=sale_line.sale_id)';
  ELSIF t='journal_line' THEN predicate := predicate || ' AND company_id=app_company() AND EXISTS (SELECT 1 FROM journal_entry j WHERE j.tenant_id=journal_line.tenant_id AND j.company_id=journal_line.company_id AND j.id=journal_line.entry_id)';
  ELSIF t IN ('outbox_delivery','inbox_receipt') THEN predicate := predicate || ' AND EXISTS (SELECT 1 FROM outbox_event e WHERE e.tenant_id='||t||'.tenant_id AND e.id='||t||'.event_id)';
  ELSIF t NOT IN ('tenant_change_clock') THEN predicate := predicate || ' AND company_id=app_company() AND branch_id=app_branch()';
  END IF;
  EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY scope_isolation ON %I USING (%s) WITH CHECK (%s)',t,predicate,predicate);
 END LOOP;
END $$;
CREATE FUNCTION reject_fact_mutation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Posted facts are immutable; use a linked reversal' USING ERRCODE='23514'; END $$;
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['stock_movement','journal_entry','journal_line','outbox_event','inbox_receipt','change_batch'] LOOP
  EXECUTE format('CREATE TRIGGER immutable_fact BEFORE UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation()',t);
 END LOOP;
END $$;
CREATE FUNCTION protect_sale() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.status NOT IN ('DRAFT','HELD') THEN RAISE EXCEPTION 'Posted sale is immutable' USING ERRCODE='23514'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER sale_immutable BEFORE UPDATE OR DELETE ON sale FOR EACH ROW EXECUTE FUNCTION protect_sale();
CREATE FUNCTION protect_sale_line() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE s sale; BEGIN
 SELECT * INTO s FROM sale WHERE tenant_id=coalesce(NEW.tenant_id,OLD.tenant_id) AND company_id=coalesce(NEW.company_id,OLD.company_id) AND id=coalesce(NEW.sale_id,OLD.sale_id) FOR UPDATE;
 IF s.id IS NULL OR s.status NOT IN ('DRAFT','HELD') THEN RAISE EXCEPTION 'Lines require an accessible draft sale' USING ERRCODE='23514'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER sale_line_immutable BEFORE INSERT OR UPDATE OR DELETE ON sale_line FOR EACH ROW EXECUTE FUNCTION protect_sale_line();
CREATE FUNCTION check_journal_balance() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE jid uuid; debits numeric; credits numeric; n bigint; BEGIN
 IF TG_TABLE_NAME='journal_entry' THEN jid:=NEW.id; ELSE jid:=NEW.entry_id; END IF;
 SELECT count(*),coalesce(sum(debit_xaf),0),coalesce(sum(credit_xaf),0) INTO n,debits,credits FROM journal_line WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND entry_id=jid;
 IF n<2 OR debits=0 OR debits<>credits THEN RAISE EXCEPTION 'Journal batch must contain balanced positive entries' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER journal_balanced AFTER INSERT ON journal_entry DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_journal_balance();
CREATE CONSTRAINT TRIGGER journal_lines_balanced AFTER INSERT ON journal_line DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_journal_balance();
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO quincai_app;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO quincai_app;
GRANT INSERT,UPDATE ON catalog_product,stock_balance,sale,sale_line,idempotency_result,outbox_delivery,tenant_change_clock TO quincai_app;
GRANT DELETE ON sale_line TO quincai_app;
GRANT INSERT ON stock_movement,journal_entry,journal_line,outbox_event,inbox_receipt,change_batch TO quincai_app;
-- No runtime TRUNCATE, DDL, membership changes, ledger UPDATE/DELETE or role escalation.

-- A balanced existing batch cannot receive extra balancing pairs in a later transaction.
CREATE FUNCTION seal_journal_lines() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE origin xid8; BEGIN
 SELECT created_transaction_id INTO origin FROM journal_entry WHERE tenant_id=NEW.tenant_id AND company_id=NEW.company_id AND id=NEW.entry_id;
 IF origin IS NULL OR origin<>pg_current_xact_id() THEN RAISE EXCEPTION 'Journal lines must be inserted with their batch' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER journal_line_sealed BEFORE INSERT ON journal_line FOR EACH ROW EXECUTE FUNCTION seal_journal_lines();
CREATE FUNCTION protect_operation_identity() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NEW.tenant_id<>OLD.tenant_id OR NEW.operation_id<>OLD.operation_id OR NEW.request_hash<>OLD.request_hash OR NEW.company_id<>OLD.company_id OR NEW.branch_id<>OLD.branch_id THEN
 RAISE EXCEPTION 'Canonical operation identity cannot change' USING ERRCODE='23514'; END IF;
 IF OLD.source_id IS NOT NULL AND (NEW.source_id IS DISTINCT FROM OLD.source_id OR NEW.source_type IS DISTINCT FROM OLD.source_type) THEN
 RAISE EXCEPTION 'Canonical source result cannot change' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER operation_identity_immutable BEFORE UPDATE ON idempotency_result FOR EACH ROW EXECUTE FUNCTION protect_operation_identity();
