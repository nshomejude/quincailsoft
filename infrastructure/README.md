# Local infrastructure for the Java migration

Use PostgreSQL18.7 (`postgres:18.7-bookworm`), verified against the official image tag listing and PostgreSQL18 documentation on 18 November2026. This is a patch-tag pin, not an immutable image digest; deployment pipeline must record the resolved digest and scan the image before release.

Supply nonempty `POSTGRES_ADMIN_PASSWORD`, `QUINCAI_MIGRATOR_PASSWORD` and `QUINCAI_RUNTIME_PASSWORD` in your local environment and run `docker compose up -d postgres`. Credentials are never committed. Bind is loopback only. PostgreSQL18 data volume uses `/var/lib/postgresql` per official image conventions.

Compose was not executed because Docker is unavailable in this workspace. Real migration/security tests instead ran against an isolated PostgreSQL18.7 binary from `io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64:18.7.0` using JDBC. This is test tooling; the application backend is Java/Spring Boot.

Configure the API with a real OIDC issuer and audience. No unverified Keycloak version, identity provider, or bypass login is silently substituted. Redis, S3, reverse proxy/TLS, secrets manager, production observability and deployment orchestration remain separate migration tasks.

Primary sources:
- https://www.postgresql.org/docs/18/ddl-rowsecurity.html
- https://hub.docker.com/_/postgres
- https://repo.maven.apache.org/maven2/io/zonky/test/postgres/embedded-postgres-binaries-linux-amd64/18.7.0/
