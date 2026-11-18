ALTER TABLE sale_fulfillment
 DROP CONSTRAINT sale_fulfillment_status_check,
 ADD CONSTRAINT sale_fulfillment_status_check
 CHECK (status IN ('PENDING', 'PARTIAL', 'DISPATCHED', 'COMPLETED')),
 ADD CONSTRAINT sale_fulfillment_branch_identity UNIQUE (tenant_id, company_id, branch_id, id);

CREATE TABLE fulfillment_dispatch_line (
 tenant_id uuid NOT NULL,
 company_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 fulfillment_id uuid NOT NULL,
 operation_id uuid NOT NULL,
 product_id uuid NOT NULL,
 quantity_milli bigint NOT NULL CHECK (quantity_milli > 0),
 dispatched_at timestamptz NOT NULL,
 dispatched_by uuid NOT NULL,
 PRIMARY KEY (tenant_id, company_id, fulfillment_id, operation_id, product_id),
 FOREIGN KEY (tenant_id, company_id, branch_id, fulfillment_id)
     REFERENCES sale_fulfillment (tenant_id, company_id, branch_id, id),
 FOREIGN KEY (tenant_id, company_id, product_id)
     REFERENCES catalog_product (tenant_id, company_id, id)
);

ALTER TABLE fulfillment_dispatch_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE fulfillment_dispatch_line FORCE ROW LEVEL SECURITY;
CREATE POLICY scope_isolation ON fulfillment_dispatch_line
 USING (tenant_id = app_tenant() AND company_id = app_company() AND branch_id = app_branch())
 WITH CHECK (tenant_id = app_tenant() AND company_id = app_company() AND branch_id = app_branch());
GRANT SELECT, INSERT ON fulfillment_dispatch_line TO quincai_app;
