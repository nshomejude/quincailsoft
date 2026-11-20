# Fulfillment service acceptance

The result records focused PostgreSQL 18.7 execution with Java 25 and SHA-256
hashes for the tested source files. The retained text report is the executed
PostgresSaleIntegrationTest result. The scenario exercises the production JPA
services with a non-owner, NOBYPASSRLS role and forced row policies.

The web proxy now permits only the implemented fulfillment reads and commands.
TypeScript checking and 25 web consumer tests passed, including method/path
rejection cases. Session forwarding, origin validation and operation identity
use the existing BFF implementation.

This evidence covers service behavior. The original S74/S75 web screens,
browser comparisons, print acceptance, concurrent dispatch and endpoint-level
OIDC permission/lifecycle cases remain open. Historical import is deferred.
Full-reactor verification is recorded separately after its process terminates.
