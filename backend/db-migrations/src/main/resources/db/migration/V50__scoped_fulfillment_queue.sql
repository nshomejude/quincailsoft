CREATE TABLE fulfillment_cursor_key (
 id integer PRIMARY KEY CHECK(id=1),
 secret bytea NOT NULL CHECK(octet_length(secret)=32)
);
INSERT INTO fulfillment_cursor_key
SELECT 1,decode(replace(gen_random_uuid()::text||gen_random_uuid()::text,'-',''),'hex');
ALTER TABLE fulfillment_cursor_key ENABLE ROW LEVEL SECURITY;
ALTER TABLE fulfillment_cursor_key FORCE ROW LEVEL SECURITY;
CREATE POLICY fulfillment_cursor_read ON fulfillment_cursor_key FOR SELECT
 USING(app_actor() IS NOT NULL);
GRANT SELECT ON fulfillment_cursor_key TO quincai_app;

CREATE VIEW fulfillment_queue_v1 WITH(security_invoker=true) AS
SELECT f.tenant_id,f.company_id,f.branch_id,f.id,f.created_at,f.status,
 f.sale_id,f.customer_id,c.name AS customer_name,s.warehouse_id,
 f.method,f.scheduled_date,f.recipient_name,f.delivery_address,
 f.business_date,f.proof_reference,f.version,f.recipient_phone,f.notes
FROM sale_fulfillment f
JOIN sale s ON(s.tenant_id,s.company_id,s.branch_id,s.id)=(f.tenant_id,f.company_id,f.branch_id,f.sale_id)
JOIN customer c ON(c.tenant_id,c.company_id,c.id)=(f.tenant_id,f.company_id,f.customer_id);
GRANT SELECT ON fulfillment_queue_v1 TO quincai_app;
CREATE INDEX fulfillment_queue_scope_order ON sale_fulfillment
 (tenant_id,company_id,branch_id,created_at,id);
