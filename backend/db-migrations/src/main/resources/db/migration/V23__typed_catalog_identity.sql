CREATE TABLE catalog_manufacturer (
    tenant_id uuid NOT NULL, company_id uuid NOT NULL, id uuid NOT NULL,
    code text NOT NULL CHECK (length(btrim(code)) BETWEEN 1 AND 80),
    name text NOT NULL CHECK (length(btrim(name)) BETWEEN 1 AND 240),
    PRIMARY KEY (tenant_id, company_id, id),
    UNIQUE (tenant_id, company_id, code),
    FOREIGN KEY (tenant_id, company_id) REFERENCES company (tenant_id, id)
);

CREATE TABLE catalog_product_family (
    tenant_id uuid NOT NULL, company_id uuid NOT NULL, id uuid NOT NULL,
    manufacturer_id uuid NOT NULL,
    product_type text NOT NULL CHECK (length(btrim(product_type)) BETWEEN 1 AND 80),
    model_number text NOT NULL CHECK (length(btrim(model_number)) BETWEEN 1 AND 80),
    PRIMARY KEY (tenant_id, company_id, id),
    UNIQUE (tenant_id, company_id, manufacturer_id, product_type, model_number),
    FOREIGN KEY (tenant_id, company_id, manufacturer_id)
        REFERENCES catalog_manufacturer (tenant_id, company_id, id)
);

CREATE TABLE catalog_variant_identity (
    tenant_id uuid NOT NULL, company_id uuid NOT NULL, product_id uuid NOT NULL,
    family_id uuid NOT NULL,
    colour text NOT NULL CHECK (length(btrim(colour)) BETWEEN 1 AND 80),
    size text NOT NULL CHECK (length(btrim(size)) BETWEEN 1 AND 80),
    PRIMARY KEY (tenant_id, company_id, product_id),
    UNIQUE (tenant_id, company_id, family_id, colour, size),
    FOREIGN KEY (tenant_id, company_id, product_id)
        REFERENCES catalog_product (tenant_id, company_id, id),
    FOREIGN KEY (tenant_id, company_id, family_id)
        REFERENCES catalog_product_family (tenant_id, company_id, id)
);

CREATE TABLE catalog_supplier_offer (
    tenant_id uuid NOT NULL, company_id uuid NOT NULL,
    product_id uuid NOT NULL, supplier_id uuid NOT NULL,
    supplier_sku text NOT NULL CHECK (length(btrim(supplier_sku)) BETWEEN 1 AND 120),
    quoted_cost_xaf bigint NOT NULL CHECK (quoted_cost_xaf >= 0),
    lead_time_days integer NOT NULL CHECK (lead_time_days >= 0),
    minimum_order_milli bigint NOT NULL CHECK (minimum_order_milli > 0),
    preferred boolean NOT NULL DEFAULT false,
    PRIMARY KEY (tenant_id, company_id, product_id, supplier_id),
    FOREIGN KEY (tenant_id, company_id, product_id)
        REFERENCES catalog_variant_identity (tenant_id, company_id, product_id),
    FOREIGN KEY (tenant_id, company_id, supplier_id)
        REFERENCES supplier (tenant_id, company_id, id)
);

DO $$
DECLARE
    relation text;
BEGIN
    FOREACH relation IN ARRAY ARRAY[
        'catalog_manufacturer', 'catalog_product_family',
        'catalog_variant_identity', 'catalog_supplier_offer'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', relation);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', relation);
        EXECUTE format(
            'CREATE POLICY scope_isolation ON %I USING (tenant_id = app_tenant() AND company_id = app_company()) WITH CHECK (tenant_id = app_tenant() AND company_id = app_company())',
            relation
        );
        EXECUTE format('GRANT SELECT, INSERT ON %I TO quincai_app', relation);
    END LOOP;
END
$$;
