# Scoped operational observability

`ScopedOperationalMetrics` is disabled unless `quincai.observability.enabled=true`. It provides authenticated `GET /api/v1/observability/metrics` in Prometheus text format. The caller must have `observability:read` (or its `SCOPE_` authority) and a current active owner/finance membership with the current authorization epoch. Existing tenant/company/branch scoped transactions and RLS apply. It never enumerates tenants or bypasses RLS. No tenant, customer, actor, phone, amount, payload or reference labels are emitted.

Keep the endpoint on a private network. Do not add it to anonymous Actuator exposure or public ingress. `prometheus.yml` is a deployment template with an intentionally unusable example target; provision an expiring scoped bearer token through a secret file and configure private TLS routing. Do not disable TLS verification. One job observes one explicitly selected scope. Prometheus job/instance labels describe the collector, not a tenant identity. The repository has no running monitoring service by virtue of these files.

Queries have a 2 second transaction-local statement timeout and 500 ms lock timeout. `quincai.observability.sample-limit` defaults to 1000 and permits 1..10000. Samples bound aggregate work/output, while finding pending entries may inspect more rows; statement timeout remains the hard database execution bound. Query failures produce an HTTP error, never false zero gauges. V37 adds scope/order indexes.

* `outbox_sample`: oldest events with no consumer delivery or at least one incomplete delivery; age is oldest such event. Completed known consumers cannot prove that every possible future consumer has processed an event.
* `unknown_sample`, `unknown_live`, `unknown_sandbox`: UNKNOWN provider intents, deterministic ID sample. No live/sandbox mixing in counts.
* `journal_sample`, `journal_imbalanced`: latest journal entries by business date/ID; checks fewer than two lines or nonzero exact numeric debit-minus-credit sum.
* Each `_sample_complete` is 1 only when count is strictly below the configured limit. A zero is conservative (including exactly the limit), not evidence of loss. Journal checks do not attest to unsampled history.

The example alerts detect collector unavailability, sample truncation, journal imbalance, live UNKNOWN payments and old pending outbox. Thresholds are examples requiring operator approval; no production alert delivery or observability acceptance is claimed. No automatic repair or payment retry occurs.
