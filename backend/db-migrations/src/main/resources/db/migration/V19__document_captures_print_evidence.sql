-- M24 owns captures/print attempts. These captures are explicitly distinct from atomic fiscal issuance.
CREATE TABLE document_capture(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,
 operation_id uuid NOT NULL,actor_id uuid NOT NULL,template_id text NOT NULL,source_id uuid NOT NULL,
 content text NOT NULL,sha256 varchar(64) NOT NULL,captured_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,operation_id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id));
CREATE TABLE document_print_attempt(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,
 operation_id uuid NOT NULL,actor_id uuid NOT NULL,original_id uuid,capture_id uuid,
 paper text NOT NULL CHECK(paper IN('A4','58mm','80mm')),duplicate boolean NOT NULL,
 reason text NOT NULL CHECK(length(trim(reason)) BETWEEN 3 AND 2000),document_hash varchar(64) NOT NULL,requested_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,operation_id),CHECK((original_id IS NULL)<>(capture_id IS NULL)),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,original_id) REFERENCES document_snapshot(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,capture_id) REFERENCES document_capture(tenant_id,company_id,id));
CREATE TABLE document_print_outcome(
 tenant_id uuid NOT NULL,company_id uuid NOT NULL,branch_id uuid NOT NULL,id uuid NOT NULL,attempt_id uuid NOT NULL,
 operation_id uuid NOT NULL,actor_id uuid NOT NULL,status text NOT NULL CHECK(status IN('PRINTED','FAILED')),
 reason text NOT NULL CHECK(length(trim(reason)) BETWEEN 3 AND 2000),recorded_at timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,company_id,id),UNIQUE(tenant_id,operation_id),UNIQUE(tenant_id,company_id,attempt_id),
 FOREIGN KEY(tenant_id,company_id,branch_id) REFERENCES branch(tenant_id,company_id,id),
 FOREIGN KEY(tenant_id,company_id,attempt_id) REFERENCES document_print_attempt(tenant_id,company_id,id));
DO $$ DECLARE t text;BEGIN FOREACH t IN ARRAY ARRAY['document_capture','document_print_attempt','document_print_outcome'] LOOP
 EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
 EXECUTE format('CREATE POLICY scope_isolation ON %I USING(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch()) WITH CHECK(tenant_id=app_tenant() AND company_id=app_company() AND branch_id=app_branch())',t);
 EXECUTE format('CREATE TRIGGER immutable_fact BEFORE UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation()',t);
 EXECUTE format('GRANT SELECT,INSERT ON %I TO quincai_app',t);
END LOOP;END $$;
-- Read-only contracts owned by procurement. Explicit columns; no arbitrary entity export.
CREATE VIEW document_purchase_v1 WITH(security_invoker=true) AS
 SELECT p.tenant_id,p.company_id,p.branch_id,p.id,p.business_date,p.status,p.version,p.total_xaf,s.name AS party,
 l.id AS line_id,l.product_id,c.name AS description,c.base_unit,l.quantity_milli,l.received_milli,l.unit_price_xaf,l.total_xaf AS line_total_xaf
 FROM purchase_order p JOIN supplier s ON(s.tenant_id,s.company_id,s.id)=(p.tenant_id,p.company_id,p.supplier_id)
 JOIN purchase_line l ON(l.tenant_id,l.company_id,l.purchase_id)=(p.tenant_id,p.company_id,p.id)
 JOIN catalog_product c ON(c.tenant_id,c.company_id,c.id)=(l.tenant_id,l.company_id,l.product_id);
CREATE VIEW document_receipt_v1 WITH(security_invoker=true) AS
 SELECT r.tenant_id,r.company_id,r.branch_id,r.id,r.business_date,r.total_xaf,p.supplier_id,s.name AS party,r.purchase_id,
 l.id AS line_id,l.product_id,c.name AS description,c.base_unit,l.quantity_milli,l.value_xaf
 FROM purchase_receipt r JOIN purchase_order p ON(p.tenant_id,p.company_id,p.id)=(r.tenant_id,r.company_id,r.purchase_id)
 JOIN supplier s ON(s.tenant_id,s.company_id,s.id)=(p.tenant_id,p.company_id,p.supplier_id)
 JOIN purchase_receipt_line l ON(l.tenant_id,l.company_id,l.receipt_id)=(r.tenant_id,r.company_id,r.id)
 JOIN catalog_product c ON(c.tenant_id,c.company_id,c.id)=(l.tenant_id,l.company_id,l.product_id);
CREATE VIEW document_supplier_return_v1 WITH(security_invoker=true) AS
 SELECT r.tenant_id,r.company_id,r.branch_id,r.id,r.business_date,r.total_xaf,r.receipt_id,
 l.id AS line_id,l.product_id,c.name AS description,c.base_unit,l.quantity_milli,l.value_xaf
 FROM purchase_supplier_return r JOIN purchase_supplier_return_line l ON(l.tenant_id,l.company_id,l.return_id)=(r.tenant_id,r.company_id,r.id)
 JOIN catalog_product c ON(c.tenant_id,c.company_id,c.id)=(l.tenant_id,l.company_id,l.product_id);
-- Finance owns these projections. Never expose provider phone/reference or cost snapshots.
CREATE VIEW document_customer_statement_v1 WITH(security_invoker=true) AS
 SELECT s.tenant_id,s.company_id,s.branch_id,s.customer_id,c.name AS party,s.id AS source_id,s.business_date,'SALE_RECEIVABLE'::text AS kind,s.status,s.total_xaf AS original_xaf,r.outstanding_xaf AS remaining_xaf
 FROM sale s JOIN receivable_balance r ON(r.tenant_id,r.company_id,r.sale_id)=(s.tenant_id,s.company_id,s.id)
 JOIN customer c ON(c.tenant_id,c.company_id,c.id)=(s.tenant_id,s.company_id,s.customer_id)
 UNION ALL
 SELECT d.tenant_id,d.company_id,d.branch_id,d.customer_id,c.name,d.id,NULL::date,'DEPOSIT_LIABILITY','CURRENT',d.original_xaf,(d.original_xaf::numeric-d.allocated_xaf-d.refunded_xaf)::bigint
 FROM deposit_balance d JOIN customer c ON(c.tenant_id,c.company_id,c.id)=(d.tenant_id,d.company_id,d.customer_id);
CREATE VIEW document_tender_v1 WITH(security_invoker=true) AS SELECT tenant_id,company_id,branch_id,id,kind,business_date,amount_xaf,customer_id,sale_id,deposit_id,cash_session_id,journal_id,reason FROM tender_fact;
CREATE VIEW document_expense_v1 WITH(security_invoker=true) AS SELECT tenant_id,company_id,branch_id,id,business_date,category,payee,amount_xaf,reason,status,version,paid_fact_id FROM operating_expense;
CREATE VIEW document_journal_v1 WITH(security_invoker=true) AS
 SELECT j.tenant_id,j.company_id,j.branch_id,j.id,j.business_date,j.source_type,j.source_id,l.id AS line_id,l.account_code,l.debit_xaf,l.credit_xaf
 FROM journal_entry j JOIN journal_line l ON(l.tenant_id,l.company_id,l.entry_id)=(j.tenant_id,j.company_id,j.id);
CREATE VIEW document_inventory_job_v1 WITH(security_invoker=true) AS
 SELECT j.tenant_id,j.company_id,j.branch_id,j.id,j.business_date,j.kind,j.status,j.version,j.source_product,p.name AS description,p.base_unit,j.source_quantity_milli,j.output_quantity_milli,j.output_product,j.evidence_reference
 FROM inventory_job j JOIN catalog_product p ON(p.tenant_id,p.company_id,p.id)=(j.tenant_id,j.company_id,j.source_product);
CREATE VIEW document_transfer_v1 WITH(security_invoker=true) AS
 SELECT t.tenant_id,t.company_id,t.branch_id,t.id,t.business_date,t.status,t.version,p.name AS description,p.base_unit,t.quantity_milli,t.received_milli,t.origin_warehouse,t.destination_warehouse
 FROM inventory_transfer t JOIN catalog_product p ON(p.tenant_id,p.company_id,p.id)=(t.tenant_id,t.company_id,t.product_id);
GRANT SELECT ON document_purchase_v1,document_receipt_v1,document_supplier_return_v1,document_customer_statement_v1,document_tender_v1,document_expense_v1,document_journal_v1,document_inventory_job_v1,document_transfer_v1 TO quincai_app;
