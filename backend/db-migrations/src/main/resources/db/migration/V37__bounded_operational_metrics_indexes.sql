CREATE INDEX outbox_scope_created_metrics_idx ON outbox_event(tenant_id,company_id,branch_id,created_at,id);
CREATE INDEX provider_scope_status_metrics_idx ON provider_intent(tenant_id,company_id,branch_id,status,id);
CREATE INDEX journal_scope_date_metrics_idx ON journal_entry(tenant_id,company_id,branch_id,business_date DESC,id DESC);
