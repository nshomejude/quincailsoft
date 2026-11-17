# Suppliers Java migration

Scoped typed JPA supplier create/list/detail and active eligibility are implemented. Canonical tenant operation retries return the original result; creation emits immutable operation evidence. Purchasing consumes only SupplierService public API. Business fields carry company scope and forced RLS. Supplier editing, activation lifecycle, compliance documents, imports, approval thresholds and complete master-data history remain pending; this module is not complete certification.

Executed integration coverage resides in purchasing/PostgresPurchasingIntegrationTest and docs/migration/m02-test-results.txt.
