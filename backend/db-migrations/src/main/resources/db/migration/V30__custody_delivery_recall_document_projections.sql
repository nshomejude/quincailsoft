-- Read-only source contracts for M24/M26. No document module writes source aggregates.
-- Invoker security preserves the underlying tenant/company/branch RLS policies.
CREATE VIEW document_delivery_v1 WITH(security_invoker=true) AS
SELECT f.tenant_id,f.company_id,f.branch_id,f.id,f.business_date,f.version,'COMPLETED'::text AS status,
 f.sale_id,f.customer_id,c.name AS party,l.id AS line_id,p.sku,p.name AS description,p.base_unit,
 l.quantity_milli,f.proof_reference,NULL::uuid AS actor_id,NULL::timestamptz AS collected_at,
 'FULFILLMENT'::text AS evidence_kind
FROM sale_fulfillment f
JOIN sale s ON(s.tenant_id,s.company_id,s.branch_id,s.id)=(f.tenant_id,f.company_id,f.branch_id,f.sale_id)
JOIN sale_line l ON(l.tenant_id,l.company_id,l.sale_id)=(s.tenant_id,s.company_id,s.id)
JOIN customer c ON(c.tenant_id,c.company_id,c.id)=(f.tenant_id,f.company_id,f.customer_id)
JOIN catalog_product p ON(p.tenant_id,p.company_id,p.id)=(l.tenant_id,l.company_id,l.product_id)
WHERE f.status='COMPLETED' AND s.status IN('POSTED','PARTIALLY_RETURNED','FULLY_RETURNED') AND length(trim(f.proof_reference))>0
UNION ALL
SELECT x.tenant_id,x.company_id,x.branch_id,x.id,(x.collected_at AT TIME ZONE b.timezone)::date,
 0::bigint,'COLLECTED',u.sale_id,u.customer_id,c.name,x.id,p.sku,p.name,p.base_unit,
 x.quantity_milli,x.proof_reference,x.actor_id,x.collected_at,'CUSTODY_PICKUP'
FROM custody_collection x
JOIN customer_custody u ON(u.tenant_id,u.company_id,u.branch_id,u.id)=(x.tenant_id,x.company_id,x.branch_id,x.custody_id)
JOIN customer c ON(c.tenant_id,c.company_id,c.id)=(u.tenant_id,u.company_id,u.customer_id)
JOIN catalog_product p ON(p.tenant_id,p.company_id,p.id)=(u.tenant_id,u.company_id,u.product_id)
JOIN branch b ON(b.tenant_id,b.company_id,b.id)=(x.tenant_id,x.company_id,x.branch_id);

CREATE VIEW document_custody_v1 WITH(security_invoker=true) AS
SELECT u.tenant_id,u.company_id,u.branch_id,u.id,u.sale_id,u.customer_id,c.name AS party,
 s.business_date,u.status,u.version,u.product_id,p.sku,p.name AS description,p.base_unit,
 u.warehouse_id,w.name AS warehouse,u.quantity_milli,u.collected_milli,
 u.quantity_milli-u.collected_milli AS remaining_milli
FROM customer_custody u
JOIN sale s ON(s.tenant_id,s.company_id,s.branch_id,s.id)=(u.tenant_id,u.company_id,u.branch_id,u.sale_id)
JOIN customer c ON(c.tenant_id,c.company_id,c.id)=(u.tenant_id,u.company_id,u.customer_id)
JOIN catalog_product p ON(p.tenant_id,p.company_id,p.id)=(u.tenant_id,u.company_id,u.product_id)
JOIN warehouse w ON(w.tenant_id,w.company_id,w.branch_id,w.id)=(u.tenant_id,u.company_id,u.branch_id,u.warehouse_id)
WHERE s.status IN('POSTED','PARTIALLY_RETURNED','FULLY_RETURNED');

CREATE VIEW document_recall_v1 WITH(security_invoker=true) AS
SELECT r.tenant_id,r.company_id,r.branch_id,r.id,r.business_date,r.status,r.version,
 l.id AS lot_id,l.lot_number,l.source_reference,l.expires_on,l.product_id,p.sku,p.name AS description,p.base_unit,
 l.warehouse_id,w.name AS warehouse,r.quantity_milli,l.quarantined_milli,
 r.reason,r.evidence_reference,r.created_by,r.approved_by
FROM inventory_recall r
JOIN inventory_lot l ON(l.tenant_id,l.company_id,l.branch_id,l.id)=(r.tenant_id,r.company_id,r.branch_id,r.lot_id)
JOIN catalog_product p ON(p.tenant_id,p.company_id,p.id)=(l.tenant_id,l.company_id,l.product_id)
JOIN warehouse w ON(w.tenant_id,w.company_id,w.branch_id,w.id)=(l.tenant_id,l.company_id,l.branch_id,l.warehouse_id)
WHERE r.status IN('APPROVED','QUARANTINED','RECOVERED','DISPOSED') AND r.approved_by IS NOT NULL;

CREATE VIEW report_supplier_payables_v1 WITH(security_invoker=true) AS
SELECT p.tenant_id,p.company_id,p.branch_id,p.id,p.supplier_id,s.name AS supplier_name,
 i.business_date,i.supplier_reference,p.original_xaf,p.paid_xaf,
 p.original_xaf-p.paid_xaf AS outstanding_xaf
FROM payable_balance p
JOIN supplier_invoice i ON(i.tenant_id,i.company_id,i.branch_id,i.id)=(p.tenant_id,p.company_id,p.branch_id,p.id)
JOIN supplier s ON(s.tenant_id,s.company_id,s.id)=(p.tenant_id,p.company_id,p.supplier_id);

GRANT SELECT ON document_delivery_v1,document_custody_v1,document_recall_v1,report_supplier_payables_v1 TO quincai_app;
