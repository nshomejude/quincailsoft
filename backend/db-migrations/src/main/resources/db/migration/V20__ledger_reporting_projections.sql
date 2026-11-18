-- Versioned reporting read contracts owned by the underlying source modules.
CREATE VIEW report_sales_lines_v1 WITH(security_invoker=true) AS
 SELECT s.tenant_id,s.company_id,s.branch_id,s.id AS sale_id,l.id AS line_id,s.business_date,s.customer_id,l.product_id,l.quantity_milli,l.unit_price_xaf,l.line_total_xaf
 FROM sale s JOIN sale_line l ON(l.tenant_id,l.company_id,l.sale_id)=(s.tenant_id,s.company_id,s.id)
 WHERE s.status IN('POSTED','PARTIALLY_RETURNED','FULLY_RETURNED');
CREATE VIEW report_returns_v1 WITH(security_invoker=true) AS SELECT tenant_id,company_id,branch_id,id,sale_id,customer_id,business_date,credit_xaf,customer_credit_xaf FROM customer_return WHERE status='POSTED';
CREATE VIEW report_deposit_v1 WITH(security_invoker=true) AS SELECT tenant_id,company_id,branch_id,id,customer_id,original_xaf,allocated_xaf,refunded_xaf,original_xaf::numeric-allocated_xaf-refunded_xaf AS remaining_xaf FROM deposit_balance;
CREATE VIEW report_expense_v1 WITH(security_invoker=true) AS SELECT tenant_id,company_id,branch_id,id,business_date,category,payee,amount_xaf,paid_fact_id FROM operating_expense WHERE status='POSTED';
CREATE VIEW report_purchase_progress_v1 WITH(security_invoker=true) AS SELECT tenant_id,company_id,branch_id,id AS purchase_id,line_id,business_date,status,party,product_id,description,base_unit,quantity_milli,received_milli,quantity_milli::numeric-received_milli AS remaining_milli,line_total_xaf FROM document_purchase_v1;
CREATE VIEW report_supplier_invoice_v1 WITH(security_invoker=true) AS SELECT tenant_id,company_id,branch_id,id,receipt_id,supplier_id,business_date,supplier_reference,total_xaf FROM supplier_invoice;
CREATE VIEW report_stock_movement_v1 WITH(security_invoker=true) AS
 SELECT m.tenant_id,m.company_id,m.branch_id,m.id,m.warehouse_id,m.product_id,(m.occurred_at AT TIME ZONE b.timezone)::date AS business_date,m.occurred_at,m.quantity_milli,m.value_xaf,m.operation_id,m.reason
 FROM stock_movement m JOIN branch b ON(b.tenant_id,b.company_id,b.id)=(m.tenant_id,m.company_id,m.branch_id);
CREATE VIEW report_count_variance_v1 WITH(security_invoker=true) AS SELECT tenant_id,company_id,branch_id,id,business_date,source_product,description,base_unit,source_quantity_milli,output_quantity_milli,output_quantity_milli::numeric-source_quantity_milli AS variance_milli FROM document_inventory_job_v1 WHERE kind='COUNT' AND status='POSTED';
CREATE VIEW report_journal_line_v1 WITH(security_invoker=true) AS SELECT tenant_id,company_id,branch_id,id AS journal_id,line_id,business_date,source_type,source_id,account_code,debit_xaf,credit_xaf,debit_xaf::numeric-credit_xaf AS net_xaf FROM document_journal_v1;
CREATE VIEW report_tender_fact_v1 WITH(security_invoker=true) AS SELECT tenant_id,company_id,branch_id,id,kind,business_date,customer_id,sale_id,deposit_id,cash_session_id,amount_xaf,cash_delta_xaf,journal_id FROM tender_fact;
CREATE VIEW report_audit_event_v1 WITH(security_invoker=true) AS
 SELECT a.tenant_id,a.company_id,a.branch_id,a.id,(a.recorded_at AT TIME ZONE b.timezone)::date AS business_date,a.recorded_at,a.actor_id,a.action,a.source_type,a.source_id,a.reason
 FROM audit_event a JOIN branch b ON(b.tenant_id,b.company_id,b.id)=(a.tenant_id,a.company_id,a.branch_id);
GRANT SELECT ON report_sales_lines_v1,report_returns_v1,report_deposit_v1,report_expense_v1,report_purchase_progress_v1,report_supplier_invoice_v1,report_stock_movement_v1,report_count_variance_v1,report_journal_line_v1,report_tender_fact_v1,report_audit_event_v1 TO quincai_app;
