# Opt-in .NET 10 desktop sync adapter

The existing standalone application remains **.NET 10 WPF + SQLite**. This directory contains an independently compiled, outbound-only adapter; it does not replace the native shell, start a local HTTP server, or require cloud identity at startup. It is not yet installed in the recovered R02 source. The Java server currently supports device enrollment and completed-evidence recovery for owner review; it does not grant offline selling authority or automatically post recovered sales.

## Build and execute checks

```sh
dotnet restore tests/Quincai.Desktop.Sync.Checks/Quincai.Desktop.Sync.Checks.csproj --locked-mode
dotnet run --project tests/Quincai.Desktop.Sync.Checks/Quincai.Desktop.Sync.Checks.csproj --no-restore
```

SDK 10.0.404; library target `net10.0`. Microsoft.Data.Sqlite 10.0.11, ProtectedData 10.0.0 and transitive versions are pinned in `packages.lock.json`. The executable integration checks use real SQLite transactions, persistent reopening and RSA/AES cryptography, with an HTTP handler simulating lost responses. They do not need a live cloud service.

## Connection and authority

`ConnectionPolicy()` defaults to standalone. A host must explicitly record owner consent, passing native gates and reconciled cutover before constructing an enabled policy. These flags are a host integration seam, not a substitute for independently reviewed evidence. `CanSellOffline` is unconditionally false because the current Java API provides no signed exclusive inventory-grant protocol. The adapter has no sale submission endpoint or stock-ledger writer.

Use `SyncClient.CreateTransport()` (redirects disabled, bounded timeout/response) and supply the native OIDC authorization-code/PKCE token provider through `IAccessTokenProvider`. OIDC login UI, refresh-token persistence and the production authorization-server client registration remain native-host integration work. The adapter never asks for or stores a user's password. All requests are HTTPS and carry tenant/company/branch context, bearer token and request UUID. Mutation request and body operation IDs match. Owner approval occurs separately through the platform owner workflow; proving possession does not self-approve the device.

## Atomic local integration

Actual R02 source was recovered and inspected; provenance and SHA-256 hashes are in `baseline-evidence/r02-provenance.json`. Its posting seam is `src/Opes.Quincalerie.Infrastructure/Persistence/SalesStore.cs`, method `PostSaleAsync`, inside `DbWriteCoordinator.ExecuteAsync`. It already wraps EF Core SQLite sale/stock/cash/receipt writes in one transaction. `CreditStore.PostCreditSaleAsync` and other posting paths need their own reviewed integration; a UI-only callback is insufficient.

After schema initialization during explicit connected onboarding, call `SqliteOutbox.Enqueue(connection, transaction, fact)` **inside that same domain transaction, before its commit**. With EF Core, obtain the underlying `Microsoft.Data.Sqlite.SqliteConnection` via `db.Database.GetDbConnection()` and `SqliteTransaction` via `tx.GetDbTransaction()`. Serialize the already-validated immutable receipt and original operation identity; sign it with the enrolled device key. Do not perform network work inside that transaction. The method deliberately accepts the caller's transaction and never commits it. Rollback removes both domain writes and evidence. The outbox cannot retroactively make an existing completed R02 posting atomic; backfill needs a reconciled, audited import process.

Run `RecoveryPump.RunOnceAsync` on a host-owned background worker using its own connection, explicit cancellation and capped retry/backoff. It does no scheduling on startup. A failed or uncertain HTTP response leaves the exact signed original fact and submission UUID unchanged. Idempotent retries go to `/api/v1/sync/recovery-submissions`; status lookup is `/api/v1/sync/operations/{submissionId}`. A review receipt must match original source operation and payload SHA-256 and have `businessPosted=false`. No receipt deletes evidence or marks a sale financially posted in cloud. Auth/permission errors must be shown as blocked recovery in the native UI, not tight-loop retries.

## Identity, cryptography and restore

Evidence retains tenant/company/branch, source-system/instance/local-operation alias, source operation UUID, original device UUID, **UUID epoch**, sequence, classification and original signature. A separate stable submission UUID is generated only at first durable enqueue. Reusing an operation or alias with different evidence fails closed. SQLite triggers prevent normal updates/deletes of completed evidence; delivery status is separate. Database administrators can remove triggers, so this is application integrity protection, not an independently tamper-proof audit store.

XAF amounts are integer strings and quantities are integer thousandths; `ExactUnits` rejects fractional/overflowing values instead of rounding. Arbitrary JSON evidence is kept byte-for-byte for signatures; callers remain responsible for validating the business document against its contract.

`WindowsUserProtector` encrypts full evidence/receipts and private keys with Windows DPAPI CurrentUser. Metadata/indexes remain visible: this is **field protection, not whole-file SQLite encryption**. The host must protect its database and backup directory ACLs. DPAPI requires the original Windows user profile/key material; migrating to another user/computer needs a reviewed authenticated export/rewrap/recovery process, which is not supplied here. There is no plaintext fallback. Linux checks use a test-only ephemeral AES protector; DPAPI execution was not tested on Windows.

Restore keeps original evidence, epoch and sequence. A newly enrolled epoch does not gain authority over old grants. Upload old signed evidence only for review, never rewrite it to the new epoch, substitute current timestamps, or generate new source operation IDs. If original key material is unavailable, preserve the evidence and escalate recovery rather than forge a new signature.

## Native gate status

R02 includes real C01–C06 definitions, canonical 88-screen register, 688-control/25-dialog/14-document acceptance scope, Windows runbooks and pilot templates. Its own README explicitly calls it an uncertified candidate. Native baseline cross-build was attempted with .NET 10.0.404 and `EnableWindowsTargeting=true`; restore failed on NuGet audit errors in the inherited SQLitePCLRaw 2.1.11 and Pkcs/Xml 9.0.0 dependencies. No vulnerability checks were disabled. The baseline has not been modified by this adapter work.

Before connection: remediate/relock baseline dependencies, compile on Windows, execute the recovered `scripts/validate_r02_windows.ps1` and C01–C06 runbooks, test DPAPI backup/restore, perform owner consent, choose archival-import versus balance-cutover mode, build legacy-ID mappings, reconcile opening balances, rehearse rollback and obtain independent signoff. Native posting integration, production PKCE login, signed inventory grants, companion snapshots, and full cutover/import are explicitly **not completed** by this adapter. Standalone functionality remains available without any of them.
