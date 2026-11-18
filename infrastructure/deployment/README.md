# Application deployment candidate

This package describes the Java API, merchant Next application, platform Next application, a one-shot schema runner and an internal TLS ingress. It requires separately provisioned PostgreSQL 18, authenticated TLS Redis and distinct merchant/platform OIDC issuers. The API contains platform-control and worker modules; the repository does not provide an independently executable worker JAR, so this package does not invent a worker process. Object storage, provider connectivity, monitored backups, release gates and an approved production target remain deployment dependencies. No production deployment was performed.

## Build immutable artifacts

Use the repository's verified backend package, frozen pnpm lockfile and approved base images. Obtain vulnerability-reviewed Java 25 JDK/runtime, Node >=22 and unprivileged nginx images by immutable digest. No unverified upstream tags are supplied. The selected nginx image must provide `nginx` and support the included TLS directives; the selected Node image must provide `node`, `npm` and a POSIX shell. Record source commit, JAR SHA-256, SBOM, scans/signatures and all resulting image digests in the release dossier.

Run backend packaging and PostgreSQL acceptance through the normal integration job first. These Docker commands build candidates only:

```sh
docker build -f infrastructure/deployment/Dockerfile \
  --build-arg JAVA_RUNTIME_IMAGE="$JAVA_RUNTIME_IMAGE" -t "$API_CANDIDATE_TAG" .
docker build -f infrastructure/deployment/Dockerfile.migrations \
  --build-arg JAVA_BUILD_IMAGE="$JAVA_BUILD_IMAGE" \
  --build-arg JAVA_RUNTIME_IMAGE="$JAVA_RUNTIME_IMAGE" -t "$MIGRATION_CANDIDATE_TAG" .
docker build -f infrastructure/deployment/Dockerfile.web \
  --build-arg NODE_IMAGE="$NODE_IMAGE" --build-arg WEB_APP=web-admin \
  -t "$MERCHANT_CANDIDATE_TAG" .
docker build -f infrastructure/deployment/Dockerfile.web \
  --build-arg NODE_IMAGE="$NODE_IMAGE" --build-arg WEB_APP=platform-admin \
  -t "$PLATFORM_CANDIDATE_TAG" .
```

Next images use `pnpm@11.25.0` from the repository's package-manager pin, `--frozen-lockfile`, the app's existing Next version and standalone output. The runtime contains standalone output, static/public assets and a small secret-file launcher; it does not receive OIDC/database credentials during compilation. Dockerfile-specific ignore files exclude local environments, dependencies and private key files from web context and restrict Java contexts to required artifacts. Never put secrets elsewhere in the build context.

The migration image compiles `MigrationRunner.java` using the exact Flyway/PostgreSQL dependencies extracted from that same verified executable JAR. It neither starts Spring nor opens a listener. It accepts only `info`, `validate` and `migrate`; clean, repair, baseline-on-migrate and out-of-order execution are unavailable. Validation permits only pending migrations before applying them; missing, failed or future applied migrations are not ignored. Version validation and PostgreSQL major-version 18 checks fail closed. The migration account is separate from the runtime role and is mounted only into the tool-profile container.

## Provision configuration and trust

Copy `environment.example` outside the repository and replace every placeholder with approved values. Export the reviewed variables in the operator shell; do not use shell evaluation on untrusted configuration. Use image digests in `QUINCAI_*_IMAGE`, never candidate tags. Runtime and migration secrets must be generated, separate and held outside the repository. Linux secret/config files mounted into UID 10001 containers must be readable by UID 10001; Compose file-backed secrets do not reliably change source-file ownership. Keep private secrets mode 0600 with matching ownership. This also applies to the nginx TLS private key and rendered config. Public certificate/CA files may be readable without exposing private keys.

Configure database certificate verification with `sslmode=verify-full&sslrootcert=/run/secrets/database_ca.pem`; issue the certificate for the database hostname in the JDBC URL. The API runtime must pass existing startup checks for PostgreSQL18, no superuser/BYPASSRLS, no object/schema-creation ownership and forced tenant RLS. Supply no migrator password to the API. V35 explicitly rejects a migration connection without cross-tenant visibility before backfilling notification deliveries. With this runner, use a separately controlled migration role authorized with SUPERUSER or BYPASSRLS; the runner does not configure an enrolled platform-actor session. Never grant those attributes to the runtime role.

Merchant and platform origins must be distinct HTTPS DNS origins on port 443. The platform issuer AND API audience must differ from merchant identity. Register exact `/auth/callback` redirects for each origin and enroll operators through the existing audited governance process. Scopes in the example contain only `openid profile`: add the approved capability set for each client before workflow testing. Setting requested scopes does not itself grant membership or authority. No demo-login/HTTP-OIDC bypass is allowed.

Each Redis URL secret must use `rediss://` with separate authenticated ACL credentials. Restrict merchant keys to `quincai:web:*` and platform keys to `quincai:platform:*` with only commands required by the existing session implementation; validate access denial for the other prefix. Use a trusted TLS certificate chain, durable availability and an explicit retention/security policy. A Redis outage must remain an authentication failure, not a session bypass. The launcher loads secrets only into the child process environment; they are not Docker build arguments or Compose environment values.

The supplied certificate chain must cover both application hostnames (or adapt the two server blocks for separate certificates). Install a valid chain and test its expiry/hostname trust with approved certificate tooling. The validator checks configuration/file properties, not certificate cryptography or external service reachability.

```sh
node infrastructure/deployment/ingress/render.mjs
node infrastructure/deployment/validate-environment.mjs
docker compose -f infrastructure/deployment/compose.yaml config --quiet
# Validate TLS config in the actual approved ingress image, without starting traffic:
docker compose -f infrastructure/deployment/compose.yaml run --rm --no-deps ingress -t -c /etc/nginx/nginx.conf
```

The ingress publishes only loopback 8443. Place it behind the approved internal network's port-443 TCP forwarding/VPN/L4 boundary. Preserve TLS end-to-end to this listener and the configured application hostnames; do not use an unreviewed public bind. No API/merchant/platform backend port is published. The ingress forwards only to Next apps, rejects direct `/admin` and `/actuator` paths and overwrites forwarded headers. Merchant/platform API calls remain server-side. The frontend Docker network is internal; backend outbound access is required for external DB/IdP/Redis. Enforce host/network egress restrictions as part of the deployment target's policy.

## Controlled rollout

1. Complete the commit-bound application release gates, migration compatibility review, load/resource budget and backup/restore rehearsal. Archive the previous image manifest and schema version. Stop new writes, jobs and provider processing if the approved migration plan requires quiescence; avoid two releases executing workers simultaneously.
2. Verify a recent encrypted backup/PITR recovery point and restoration evidence. Check pending migrations against the same API artifact. On an existing schema, do not baseline or skip reserved/missing migrations to obtain a green result.
3. Run the explicit one-shot migration sequence using the tools profile:

   ```sh
   docker compose -f infrastructure/deployment/compose.yaml --profile tools run --rm migrate info
   docker compose -f infrastructure/deployment/compose.yaml --profile tools run --rm migrate validate
   docker compose -f infrastructure/deployment/compose.yaml --profile tools run --rm migrate migrate
   ```

   Inspect the exit status and migration history before runtime startup. Compose runtime startup deliberately does not run migrations automatically. Never use the runtime account for migrations.
4. Start the API and both apps on the private networks, then ingress:

   ```sh
   docker compose -f infrastructure/deployment/compose.yaml up -d api merchant platform
   docker compose -f infrastructure/deployment/compose.yaml up -d ingress
   ```

   There is no false readiness healthcheck here: run actual `/actuator/health` from the internal operator network, OIDC login/callback checks and authenticated API workflows. Next can start before API availability; visible dependency failures are not success. Reload/recreate ingress after replacing app containers so nginx resolves current upstream IPs.
5. Verify TLS, cookie flags, merchant/platform identity isolation, scoped denial paths, migration version, worker/outbox behavior, payment ambiguity handling, logs/metrics and backup monitoring. Verify desktop and narrow UI workflows with actual role/branch data. Admit traffic only after the deployment owner accepts this evidence.

Resource limits are candidate bounds, not capacity measurements. Budget all API pools against the database limit. No autoscaling or recovery guarantee is inferred from Compose restart policies.

## Safe rollback

Rollback is a reviewed decision, not an automatic `down` or old-image restart. Freeze incoming writes and internal/provider processing, preserve logs, image digests, migration history and new operation IDs. Never delete volumes, purge Redis sessions indiscriminately, run Flyway clean/repair or reverse SQL migrations ad hoc.

For an application-only rollback, first verify that the previous artifact can read/write the current schema and interpret every record/event written by the candidate. If the migration is explicitly backward-compatible and this compatibility is tested, restore the approved previous API/web image digests without applying old migrations, recreate apps/API, recreate ingress, then repeat auth/health/business checks before reopening traffic. Reconcile pending provider requests using their original operation identities.

If compatibility is not demonstrated, keep the service stopped for writes and choose an approved forward fix or rehearsed database/PITR recovery. Restoring a database after external payments/new transactions requires reconciliation and potentially controlled replay; it is not merely changing an image. Follow `infrastructure/recovery/README.md`, retain the pre-recovery database and evidence under the retention policy, validate balances/stock/idempotency/outbox/provider states, then resume reads, internal processing and external traffic in that order. Record the approver, timestamps, data-loss window, reconciliation outcomes and rollback/forward-fix artifacts.

## Executed evidence and remaining gates

Configuration tests: `node --test infrastructure/deployment/*.test.mjs` passed (26 tests). The migration runner compiled with Java25 against the packaged API's exact libraries and rejected clean/repair/baseline/unknown commands before any database configuration access. Compose YAML parsed and private-port/service separation checks passed. Merchant/platform Next production builds are validated separately by their app owners.

Docker is unavailable in this workspace. Docker image builds, `docker compose config`, nginx configuration execution, certificate validation, container startup, migration execution through this runner, Redis ACL/network checks and full rollout/rollback rehearsal are **NOT_RUN**. The package is an application deployment candidate, not production release certification. `QUINCAI_DEPLOYMENT_MODE=api-only` exists only for the older API-only validation contract; it does not validate this complete Compose deployment.
