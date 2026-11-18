# Authenticated synthetic HTTP measurement

Run against an explicitly provisioned synthetic deployment using Node 22 or newer:

```sh
export QUINCAI_PERF_SYNTHETIC=true
export QUINCAI_PERF_BASE_URL=http://127.0.0.1:8080
# Supply QUINCAI_PERF_TOKEN_OWNER through the environment from your secret manager.
node infrastructure/performance/run.mjs infrastructure/performance/read-scenario.json /tmp/quincai-read-result.json
node --test infrastructure/performance/run.test.mjs
```

The example uses real authenticated product/category endpoints. The token needs catalog:read plus valid scope/epoch claims and current membership. No test token or guessed credentials are included. Remote targets require HTTPS. Redirects are never followed, preventing bearer forwarding. Response bodies are discarded with a 1 MiB bound and never enter results. Results contain operation names and aggregate timings only. Use generic operation names without identity data.

Scheduling is open-loop with round-robin operations and actor tokens. Concurrency exhaustion or lateness exceeding an arrival interval is recorded as a drop, not silently queued away. Limits: 100 concurrent requests, 100 arrivals/second, 600 seconds, 10000 scheduled requests, and 30 second request timeout. Reported percentiles include failed request durations; errors and dropped work are separately counted. Multiple token environment names permit distinct synthetic scopes. The driver reports effective throughput and scheduling delay so its own saturation remains visible.

A thresholded read scenario exits 0 only with `PASS_SCENARIO`; failed thresholds exit 1. Without thresholds it exits 2 (`MEASURED_NOT_ACCEPTED`). PASS refers only to the configured scenario, not architectural production capacity. The example thresholds are a starting point, not measured evidence. Keep dataset size, host sizing, image revision, migration version and application logs with externally collected evidence; this harness does not certify those declarations.

Writes require `QUINCAI_PERF_ALLOW_WRITES=true`. `{{operationId}}` and `{{requestId}}` in body/path are replaced by one fresh request UUID, also used for Idempotency-Key. Expected successful status codes are configurable. Write runs always exit 2, even with good latency: business effects, journal balance, duplicate suppression and cross-tenant isolation require separate database/API reconciliation. A successful HTTP status alone is not correctness acceptance. No existing records are cleaned up automatically.

Missing evidence remains explicit: seeded architecture-scale datasets, sale/payment/stock contention workloads, command replay/reconciliation, workload-weight calibration, long-duration saturation, and private deployment measurement must be executed before production capacity claims. The included unit tests validate runner safeguards and statistics; they do not constitute an application load test.
