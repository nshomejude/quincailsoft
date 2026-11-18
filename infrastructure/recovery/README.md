# PostgreSQL backup and isolated restore rehearsal

These operator tools create a full PostgreSQL18 custom archive with a consistent source reconciliation manifest, restore it into a disposable isolated cluster, and compare every supported source table's row count and SHA-256 fingerprint with the restored result. They do not deploy, overwrite the running application, or perform selective tenant cutover.

## Backup

Configure standard PostgreSQL connection settings (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, a restricted `.pgpass`/secret injection). Do not place credentials in command arguments or commit them. Use an approved backup identity with complete read access; `row_security=off` makes incomplete RLS-filtered reads fail rather than silently produce a partial backup.

```bash
infrastructure/recovery/backup-postgres.sh /secure/new-backup-directory
```

The source PostgreSQL18 database must be reachable and the output directory must not already exist. The tool holds an exported REPEATABLE READ snapshot open while both `pg_dump --snapshot` and fingerprint queries consume it. Fingerprints stream canonical JSON rows ordered using C collation, with UTC timestamp rendering, through SHA-256. They retain row multiplicity and exact database values, including large integer money values. No complete table is aggregated into a single SQL string. The manifest contains counts/digests rather than row contents.

Outputs: `source.dump`, `fingerprints.tsv`, `backup-manifest.json` and logs. A failure leaves `result.txt` marked FAIL, never an accepted backup manifest. Retain manifest digests independently according to backup governance. A digest detects corruption; it does not authenticate an untrusted archive. PostgreSQL archives contain executable SQL: accept only the approved backup process's archives.

This adapter supports ordinary and partitioned project tables. It fails closed for user sequences, foreign tables, materialized views or database large objects until their separate non-MVCC/external reconciliation rules are implemented. The application uses UUIDs and typed document counters. S3 objects, identity-provider state and signing keys require separate coordinated backups. Restrict and encrypt the archive directory and offsite copies according to the deployment policy; these scripts do not implement encryption or retention scheduling.

## Isolated restore and exact reconciliation

```bash
infrastructure/recovery/rehearse-postgres.sh \
  /secure/backup/source.dump ARCHIVE_SHA256 \
  /secure/backup/fingerprints.tsv FINGERPRINTS_SHA256 \
  /secure/new-drill-evidence
```

Supply both independently retained digests from the backup manifest. The script verifies them before creating a candidate, stages private immutable copies and rechecks the digests. It accepts no destination database URL and refuses to replace existing evidence.

Requirements: unprivileged Linux operator, PostgreSQL18 client/server executables, working unprivileged user/network namespaces, `setpriv`, and a C compiler. The small fixed-interface `isolated-loopback.c` helper enables **only `lo` inside the newly created namespace**. All capabilities are then dropped and privilege escalation is disabled before PostgreSQL or archive SQL runs. Recovery verifies a different network namespace, no external routes, only the loopback interface, zero capabilities and `NoNewPrivs=1`. PostgreSQL listens on namespace-local 127.0.0.1 only; this address is not the host's loopback. No API, worker or payment adapter starts. Failed isolation checks never fall back to the host network.

This transport replaces the earlier Unix-socket-only approach because this executor forbids Unix socket binding and netlink sockets. The replacement preserves a private network namespace and removes all setup capabilities; it does not relax egress isolation. The infrastructure helper is not application business logic; the application remains Java/Spring Boot/PostgreSQL.

The restore is a single transaction with `--exit-on-error`. A fresh source-style snapshot fingerprints the candidate, and a byte-for-byte manifest comparison must pass. Evidence includes restored fingerprints, source/backup digests, reconciliation diff, table counts, PostgreSQL version, capability/route state and logs. The candidate cluster is stopped and deleted on success or failure. Nothing switches production routing.

Ownership and ACLs are intentionally translated for the isolated candidate (`--no-owner --no-acl`); an inactive `quincai_app` policy role exists. PASS therefore proves isolated archive restoration and exact supported-table reconciliation. It does **not** certify production grants, RLS authorization, restored signing keys/objects, PITR, retained post-backup receipts, selective tenant recovery, stale-writer fencing, or deployment. Architecture §41 requires those separate reviewed gates before reopening production.

## Executable acceptance test

```bash
PATH=/path/to/postgresql18/bin:$PATH \
  infrastructure/recovery/test-rehearsal.sh /secure/new-test-evidence
```

Freeze migration writers first. The test applies all current migrations to a temporary synthetic source on host loopback port 55448, inserts tenant/company/branch/catalog/stock and balanced journal fixtures, and uses the real backup and isolated restore tools. It requires exact source/restored table fingerprints, verifies representative row counts and isolated networking, then rejects evidence overwrite, changed archive bytes, invalid archive format and an intentionally wrong source reconciliation manifest. The source is stopped afterward. Successful evidence is copied to the optional new directory; failure artifacts are retained for inspection.

Test execution is serialized by the integration lead. Shell syntax and helper compilation alone are not a restore pass. Record the executed command, source commit, environment, logs and result; do not carry earlier FAIL/BLOCKED results forward as successful acceptance.

## Target-tenant manifest preflight

`tenant-manifest-postgres.sh EXPORTED_SNAPSHOT TENANT_UUID NEW_EVIDENCE_DIRECTORY`
creates a read-only, target-only fingerprint manifest under an already-held
PostgreSQL18 snapshot. Use an approved recovery operator identity; runtime
RLS-filtered reads fail rather than silently produce incomplete evidence. Freeze
schema changes while the snapshot is held. It accepts only the public application
schema and rejects unsupported external/non-MVCC objects.

The output classifies all tables, hashes only tenant-scoped rows, and reports
cross-tenant FK references and both inbound/outbound unscoped dependencies as
BLOCKED (exit 2). Query failures return FAIL; existing evidence is never replaced.
A PASS means only that target fingerprints and declared FK preflight completed.
It does not verify JSON references, signing keys, external objects, lawful
retention or cutover. Unscoped dependencies need reviewed extraction adapters;
production routing is never enabled.

Run `test-tenant-manifest.sh NEW_EVIDENCE_DIRECTORY` with PGHOST/PGPORT/PGUSER
pointing to an approved disposable PostgreSQL18 test server. It creates its own
database and synthetic fixtures, checks scope/closure failures and drops the
database afterward. It never creates a network interface or changes server
networking. Integration lead serializes this acceptance test.
