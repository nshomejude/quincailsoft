-- Company/supplier identity spans branches. Preserve posted reference text; use
-- a separate key for duplicate detection. ASCII case and outer POSIX whitespace
-- are normalized; punctuation and interior whitespace remain significant.
-- Existing collisions intentionally stop migration for reviewed reconciliation.
CREATE UNIQUE INDEX supplier_invoice_normalized_reference_uq
 ON supplier_invoice(tenant_id,company_id,supplier_id,
  (lower(regexp_replace(supplier_reference COLLATE "C", '^[[:space:]]+|[[:space:]]+$', '', 'g'))));
ALTER TABLE supplier_invoice ADD CONSTRAINT supplier_invoice_normalized_reference_nonempty
 CHECK(length(regexp_replace(supplier_reference COLLATE "C", '^[[:space:]]+|[[:space:]]+$', '', 'g'))>0);
