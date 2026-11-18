# API deployment candidate

This package runs the Java 25 API against an independently provisioned PostgreSQL 18 database and real OIDC issuer. It is **not a complete SaaS release**: web/identity/object storage, TLS ingress, monitored backups, production acceptance and an approved target are still required. No production deployment was executed. Docker is unavailable in the implementation workspace.

1. Build and test the complete backend with PostgreSQL enabled. Package `backend/api-server/target/api-server-0.1.0-SNAPSHOT-exec.jar` using Maven `package`.
2. Select a vulnerability-reviewed Java 25 runtime image by immutable digest. Build from repository root:

   ```sh
   docker build -f infrastructure/deployment/Dockerfile \
     --build-arg JAVA_RUNTIME_IMAGE="$JAVA_RUNTIME_IMAGE" -t "$CANDIDATE_TAG" .
   ```

   Publish to the approved registry, scan/sign it and set `QUINCAI_API_IMAGE` to its resulting immutable digest. Record commit, JAR SHA-256 and image digest in the release dossier. A tag alone is rejected by the preflight.
3. Apply Flyway migrations with the separate migrator account, after backup and compatibility checks. The runtime has migrations disabled and never receives migrator credentials. Do not skip reserved/missing migrations in an established deployment; coordinate V23–V26 with Desktop.
4. Supply `QUINCAI_DATABASE_URL` (JDBC PostgreSQL URL with `sslmode=verify-full` and CA root trust), runtime `QUINCAI_DATABASE_USER`, HTTPS `QUINCAI_OIDC_ISSUER`, `QUINCAI_OIDC_AUDIENCE`, and `QUINCAI_DATABASE_PASSWORD_FILE`. The secret is outside the repository, readable only by its owner. On the Linux deployment host its owner must match runtime UID 10001 so the mounted file remains readable; Docker Compose file-backed secrets do not reliably remap source-file ownership. Do not commit or print credentials.
5. Run `node infrastructure/deployment/validate-environment.mjs`. Run the separate full release gate; passing this environment check does not make incomplete modules releasable.
6. Once release acceptance and the target are approved, run `docker compose -f infrastructure/deployment/compose.yaml up -d`. The listener is loopback-only on port 8080; terminate TLS in the approved ingress. Confirm `/actuator/health`, authenticated tenant isolation, payment ambiguity handling and restore evidence before admitting users.

The container runs as non-root with read-only root filesystem, bounded temporary space/logs/resources, no Linux capabilities and graceful shutdown. The new `DeploymentSafetyVerifier` prevents startup with schema/database creation privileges, application-table ownership (including inherited ownership), or tenant tables lacking forced RLS. The existing verifier rejects superuser/BYPASSRLS and non-PostgreSQL18 connections.

Resource and pool limits here are conservative candidate settings, not measured capacity claims. Resolve pool budgets across API replicas/workers and benchmark the declared workload before production. On-premises mode requires its own approved local CA/IdP and deployment acceptance.

Control-plane endpoints are disabled by default. To enable them, provide separate platform issuer/audience and platform operator enrollment through the existing platform security configuration. Never reuse merchant issuer/audience or generate demo identities in production.

Rollback must account for data migrations and new writes; changing the image back alone is not a safe database rollback. Use the reviewed recovery runbook and restore rehearsal under `infrastructure/recovery/`.
