# Candidate dependency matrix and execution gate

Pinned backend: Java 25 (compiler/enforcer `[25,26)`), Spring Boot 4.0.9, Spring Modulith 2.0.9, Maven 3.9.11. Spring Boot BOM supplies the matching Spring Framework/Security, Hibernate, Flyway, PostgreSQL driver and JUnit versions; Maven dependency-tree output is required in release evidence. No milestone/SNAPSHOT framework is selected.

Official evidence inspected:
- https://docs.spring.io/spring-boot/4.0/system-requirements.html — 4.0.9 supports Java 25 within its documented range.
- https://spring.io/blog/2026/10/23/spring-modulith-2-2-rc1-2-1-2-2-0-9-and-1-4-14-released/ — 2.0.9 stable release.
- https://spring.io/blog/2025/11/21/spring-modulith-2-0-ga-1-4-5-and-1-3-11-released/ — Modulith 2.0 baseline Boot 4/Framework 7.
- https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip.sha512 — distribution checksum pinned by backend/mvnw.

The `mvnw` launcher downloads Maven only over HTTPS and verifies SHA-512 before extracting. Java 25 is required, never silently replaced with Java 21. Redis/object storage/mobile/desktop compatibility is a separate uncompleted gate.

Modulith starter-core/test are used for module boundaries only. No persistent Modulith event publication starter is installed. PostgreSQL custom outbox is the sole intended durable domain-event publisher.

Runtime identity requires OIDC issuer discovery/JWKS and audience verification. The token's UUID subject, tenant/company/branch claims and authorization epoch are validated. Every ScopedTransactions operation confirms the exact active database membership and epoch before executing domain code under SET LOCAL RLS context. Neither tenant headers nor client-selected role strings grant authority. Human login, MFA and PKCE belong to the configured identity provider and web/mobile client integration; this resource server does not claim to implement enrollment or recovery.

Membership/permission changes must atomically update authorization_epoch and issue new tokens; stale tokens fail closed. This foundation is not proof of production penetration testing or completed SSO lifecycle.
