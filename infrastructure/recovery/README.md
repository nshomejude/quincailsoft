# PostgreSQL restore rehearsal

`rehearse-postgres.sh` restores a **trusted PostgreSQL custom-format archive** into a new, disposable PostgreSQL18 cluster. It accepts no database URL and cannot target the live application database. An unprivileged Linux operator, PostgreSQL18 client/server executables on PATH, and working unprivileged user/network namespaces are required. A failed isolation check stops the operation.

```bash
infrastructure/recovery/rehearse-postgres.sh /secure/path/database.dump EXPECTED_SHA256 /secure/path/new-drill-evidence
```

Obtain the expected SHA-256 from the separately retained backup manifest. A checksum detects corruption; it does not authenticate an untrusted archive. PostgreSQL archives contain executable SQL. Accept only archives produced by the approved backup process. Keep archive and evidence directories access-restricted and encrypted according to the deployment retention policy. The script does not supply encryption or remote backup retention.

The fresh network namespace has no external route. PostgreSQL listens only on a private Unix socket. No API, worker, or payment adapter is started. The cluster is removed on completion/failure; the evidence directory retains the archive digest, PostgreSQL version, restore logs, isolation information, and restored table counts. The script refuses to replace existing evidence. It never replays an external payment.

The restore uses a single transaction and stops at the first SQL error. Ownership and ACLs are deliberately not restored; a NOLOGIN/NOBYPASSRLS `quincai_app` role supports application policies. Therefore PASS means the archive could be restored in isolation, **not** that production grants, RLS authorization, financial reconciliation, original object/signing keys, PITR, selective tenant recovery, or application deployment have passed. Unknown role dependencies fail closed. Table counts are evidence from the candidate, not a comparison to a consistent source snapshot.

Architecture §41 still requires separately reviewed tenant dependency extraction, retained-receipt replay, reconciliation, recovery-epoch fencing, key/object restoration and controlled cutover. This tool implements the isolated whole-database rehearsal boundary only. It is not a tenant self-service overwrite endpoint.

## Verification

Run `infrastructure/recovery/test-rehearsal.sh` with PostgreSQL18 tools on PATH. It creates a temporary synthetic source on loopback port 55448, applies immutable V1–V22, inserts one tenant/company/branch, exports a real custom archive, and exercises isolated restore plus corruption/overwrite rejection. It does not use the application's database. On failure it retains the test directory for inspection; on success it removes synthetic artifacts. The restore candidate itself is always removed.

On 18 November 2026, shell syntax, wrong-checksum rejection, V1–V22 source construction and custom archive creation executed successfully here with PostgreSQL18.7. The isolated restore was **BLOCKED** by the executor denying Unix-domain socket creation (`Operation not permitted`). The tool failed closed and recorded FAIL; it did not retry with weaker network isolation. Successful runtime restore and remaining negative cases must run on a Linux host that permits the required sockets/namespaces. No production recovery acceptance is claimed.
