-- Read-only operational document source; source ownership remains with sales.
CREATE VIEW document_pick_ticket_v1 WITH (security_invoker=true) AS
SELECT f.tenant_id,f.company_id,f.branch_id,f.id,f.business_date,f.status,f.version,
 c.name AS party,f.sale_id,s.warehouse_id,w.name AS warehouse,
 p.id AS line_id,p.sku,p.name AS description,p.base_unit,
 l.authorized_milli,coalesce(d.dispatched_milli,0) AS dispatched_milli,
 l.authorized_milli-coalesce(d.dispatched_milli,0) AS remaining_milli,
 f.method,f.scheduled_date,f.delivery_address AS address,f.recipient_name,f.recipient_phone,f.notes
FROM sale_fulfillment f
JOIN sale s ON (s.tenant_id,s.company_id,s.branch_id,s.id)=(f.tenant_id,f.company_id,f.branch_id,f.sale_id)
JOIN customer c ON (c.tenant_id,c.company_id,c.id)=(f.tenant_id,f.company_id,f.customer_id)
JOIN warehouse w ON (w.tenant_id,w.company_id,w.branch_id,w.id)=(s.tenant_id,s.company_id,s.branch_id,s.warehouse_id)
JOIN LATERAL (
 SELECT product_id,sum(quantity_milli) AS authorized_milli FROM sale_line
 WHERE tenant_id=s.tenant_id AND company_id=s.company_id AND sale_id=s.id GROUP BY product_id
) l ON true
JOIN catalog_product p ON (p.tenant_id,p.company_id,p.id)=(s.tenant_id,s.company_id,l.product_id)
LEFT JOIN LATERAL (
 SELECT sum(quantity_milli) AS dispatched_milli FROM fulfillment_dispatch_line
 WHERE tenant_id=f.tenant_id AND company_id=f.company_id AND branch_id=f.branch_id
  AND fulfillment_id=f.id AND product_id=l.product_id
) d ON true
WHERE f.status IN ('PENDING','PARTIAL') AND s.status='POSTED'
 AND l.authorized_milli>coalesce(d.dispatched_milli,0);
GRANT SELECT ON document_pick_ticket_v1 TO quincai_app;
