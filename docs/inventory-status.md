# Inventory workflow implementation status

Implemented in the local integration backend, with transaction ownership and idempotency delegated to server.command:

- Goods receipt posting validates approved/partially received purchase source, supplier, warehouse and aggregate remaining quantities; increases physical stock and updates weighted-average product cost. Purchase closes only when fully received.
- Approved receipt/issue adjustments change stock, preserve movement evidence, and reject insufficient merchant-owned serviceable quantity.
- Counts capture product versions and physical snapshot, accept explicit actual counts through the capture command, require approval, reject stale snapshots and duplicate product lines, and post actual counted variance.
- Same-branch/same-company warehouse transfers dispatch into explicit transit, then receive partial quantities. Destination only increases on receipt; quantities cannot exceed dispatched remainder.
- Customer custody supports partial collection with proof; deducts physical custody without a second sale/ownership posting.
- Fulfillment requires matching completed sale and proof; refuses completion while customer custody remains unresolved.
- Approved sellable customer returns and supplier physical returns enforce source identity, aggregate eligible quantity and finality. Financial credit/refund remains a separate pending workflow.
- Approved zero-loss repack/assembly/disassembly changes component/output quantities and transfers weighted cost.
- Lot quarantine blocks affected aggregate available stock; evidence-backed recovery releases it and disposal decrements physical stock.

`change_stock(c,user,product_id,delta,warehouse_id=None,source=None,allow_custody=False)` is the integration entry point for all stock mutations, including existing sale completion. Product `stock_positions` maps warehouse IDs to decimal quantities; aggregate stock must reconcile. Legacy opening stock is assigned only when the branch has exactly one warehouse. Adding warehouses requires explicit opening balances.

Twelve focused tests pass in tests/test_inventory_workflows.py: receipt/order, overreceipt rollback, count variance/staleness, custody partial/excess, transfer partial receipt/excess, custody protection, return finality, quarantine disposal, repack cost, scope/version, and cut rejection. These are local domain tests, not production certification.

Still blocked: cross-branch inventory authority, exclusive transfer reservation, damaged/quarantine return inspection, loss-bearing transformation recipes, cut output product/lot lineage, robust lot/serial attribution, fiscal recognition policy, journal integration, immutable movement tables, complete warehouse-opening UX and distributed offline reconciliation. Partial lot quarantine currently blocks the affected aggregate quantity; it is not complete traceability. Generic product and custody edits must protect server-derived balances. Count approval must accept Counting -> In review -> Approved. The capture command takes quantities keyed by every snapshot product and marks the measurement captured; posting rejects missing capture. Inventory operators can start/capture; manager authority is required for posting. All physical postings require a valid open financial period.
