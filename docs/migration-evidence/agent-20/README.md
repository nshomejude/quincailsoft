# Assignment 20: independent behavioral reconciliation

Read AGENTS.md, architecture §§31,33–34,38,44, the migration summary/canonical coverage/parity inventories, the parity evidence validator, actual legacy boundary/security/inventory tests, agent handoff evidence available at review time, changed source and concrete Java assertions. Historical tests and candidate source lists are not acceptance. Owned files are this directory only; no application, acceptance register or generated inventory was edited.

## Result

`proposed-acceptance.json` contains one bounded candidate for normative replacement and two explicitly incomplete mappings. No row is marked ACCEPTED and no test execution is claimed by this agent. Root must attach the actual passing, committed JUnit report and verify source digests against the tested tree before any central acceptance update.

The strongest candidate is owner-created DOC01 capture disclosure to a cashier: fixed typed projections replace unrelated label dictionaries, and the Java test reads the same capture under cashier authority while excluding supplier/staff/role/integration keys. Positive legacy HTTP assertions currently map to service calls; root must explicitly approve this normative replacement or add authenticated HTTP evidence. It does not certify the old global `/api/state` route or all document families.

Identity `test_access_change_revokes_sessions` must remain incomplete. The legacy assertion updates access while preserving role/active state, then checks old-session HTTP401 and self-target update403. The new test changes role and active state, exercises explicit revoke, and rejects self-approval. These are useful checks but not identical assertions. Add same-value access update invalidation, direct self-target request denial and HTTP old-token rejection before accepting the whole row.

Cached replay redaction also remains incomplete. Receipt actor/scope/source guards are not evidence that the same actor after a role downgrade cannot receive a historically sensitive response. Typed DTOs may make this safe structurally, but the actual cached-sale serialization and current authority path need direct assertion mapping. A safe invoice snapshot does not certify a safe cached sale response.

The inventory stale-count test has a rejection assertion after a stock movement, but the inspected block does not immediately assert unchanged stock as the legacy row does. Root should add explicit stock/fact-count invariance if accepting that full requirement rather than treating exception-only proof as rollback acceptance.

## Integration review

No additional confirmed P1 defect is asserted from this bounded review. Source additions require regenerated contracts/SDK and complete parity source inventory. The new V49 normalized supplier-invoice uniqueness migration must be exercised both on an empty fixture and reviewed for existing collision handling; a fail-closed migration is not automatic remediation of historical duplicates. The step-up SPI is deliberately unconnected to a trusted OIDC issuer adapter; customer exports must remain disabled. Recovery remains synthetic-schema fingerprint/dependency preflight, not complete production cutover. Agent handoff docs retain NOT_RUN until root executes and binds evidence.

Full migration acceptance is still blocked; twenty assignments and passing test counts are not proof that all legacy branches, screens, reports, integrations and release criteria are complete.
