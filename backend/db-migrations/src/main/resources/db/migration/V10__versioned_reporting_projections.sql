-- Versioned, read-only cross-module projection contracts. PostgreSQL18 security_invoker
-- preserves runtime RLS; owner-rights views must never expose tenant facts.
CREATE VIEW report_posted_sales_v1 WITH (security_invoker=true) AS
 SELECT tenant_id,company_id,branch_id,id,business_date,customer_id,status,total_xaf,cost_xaf,total_xaf::numeric-cost_xaf::numeric AS margin_xaf
 FROM sale WHERE status IN('POSTED','PARTIALLY_RETURNED','FULLY_RETURNED');
CREATE VIEW report_stock_v1 WITH (security_invoker=true) AS
 SELECT b.tenant_id,b.company_id,b.branch_id,b.warehouse_id,b.product_id,p.sku,p.name,p.base_unit,b.quantity_milli,b.reserved_milli,b.allocated_milli,b.quantity_milli::numeric-b.reserved_milli::numeric-b.allocated_milli::numeric AS available_milli,b.value_xaf
 FROM stock_balance b JOIN catalog_product p ON p.tenant_id=b.tenant_id AND p.company_id=b.company_id AND p.id=b.product_id WHERE b.ownership_kind='MERCHANT' AND b.condition='SERVICEABLE';
CREATE VIEW report_receivable_v1 WITH (security_invoker=true) AS
 SELECT r.tenant_id,r.company_id,r.branch_id,r.sale_id,s.customer_id,s.business_date,r.outstanding_xaf
 FROM receivable_balance r JOIN sale s ON s.tenant_id=r.tenant_id AND s.company_id=r.company_id AND s.id=r.sale_id;
CREATE VIEW report_cash_v1 WITH (security_invoker=true) AS
 SELECT c.tenant_id,c.company_id,c.branch_id,c.id,c.till_code,c.business_date,c.status,c.opening_xaf,coalesce(m.amount,0) AS movement_xaf,c.opening_xaf::numeric+coalesce(m.amount,0) AS expected_xaf,c.counted_xaf,
 CASE WHEN c.counted_xaf IS NOT NULL THEN c.counted_xaf::numeric-c.opening_xaf::numeric-coalesce(m.amount,0) END AS variance_xaf
 FROM cash_session c LEFT JOIN(SELECT tenant_id,company_id,cash_session_id,sum(cash_delta_xaf::numeric) AS amount FROM tender_fact GROUP BY tenant_id,company_id,cash_session_id)m ON m.tenant_id=c.tenant_id AND m.company_id=c.company_id AND m.cash_session_id=c.id;
CREATE VIEW report_provider_intent_v1 WITH (security_invoker=true) AS
 SELECT tenant_id,company_id,branch_id,id,sale_id,business_date,environment,currency,status,amount_xaf AS amount,journal_id FROM provider_intent;
GRANT SELECT ON report_posted_sales_v1,report_stock_v1,report_receivable_v1,report_cash_v1,report_provider_intent_v1 TO quincai_app;
