# Wave 2 operations migration evidence

Preflight: read AGENTS.md; architecture §§17,19.7,34,38,39,41,43,44.3 and Appendix A recovery differences; current evidence summary and relevant canonical-coverage records. `python-java-parity.json` was absent at assignment start. Read complete `server/operations.py`, `tests/test_operations.py`, and operational handlers in `server/server.py`.

Ownership: new operations module, identity epoch revocation and guarded workflow fixes, suppliers metadata/CRUD, forward migrations V17/V18. Root owns reactor/security/lockfiles and catalog/customer/company/branch public APIs. No changes to V1–V12. Root alone executes Maven.

Intended acceptance: CSV syntax/mapping/limits and typed field validation; actor-bound preview expiry/digest; operation replay/conflicting keys; all-or-nothing duplicate rollback; no inventory/financial opening edits; tenant/company/branch RLS; export permission revalidation and CSV formula escaping; private notification read-state; no secret-bearing backup records; scoped manifest integrity, stale recovery review rejection and production-restore refusal; self/owner epoch revocation without role escalation; independent identity approvals. Implementation and executed evidence below remain pending until verified.
