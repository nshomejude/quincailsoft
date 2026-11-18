using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Data.Sqlite;

namespace Quincai.Desktop.Sync;

public sealed record PendingFact(OutboxFact Fact, Guid SubmissionId, int Attempts);

/// <summary>Adapter-owned SQLite tables in the same local database as the domain posting.</summary>
public sealed class SqliteOutbox(ILocalProtector protector)
{
    public void Initialize(SqliteConnection connection)
    {
        using var command = connection.CreateCommand();
        command.CommandText = """
            CREATE TABLE IF NOT EXISTS sync_evidence_v1(
              tenant_id TEXT NOT NULL, operation_id TEXT NOT NULL, company_id TEXT NOT NULL, branch_id TEXT NOT NULL, alias_hash TEXT NOT NULL,
              evidence_hash TEXT NOT NULL, protected_evidence BLOB NOT NULL,
              submission_id TEXT NOT NULL UNIQUE, created_at TEXT NOT NULL,
              PRIMARY KEY(tenant_id,operation_id), UNIQUE(tenant_id,alias_hash));
            CREATE TABLE IF NOT EXISTS sync_delivery_v1(
              tenant_id TEXT NOT NULL, operation_id TEXT NOT NULL, attempts INTEGER NOT NULL DEFAULT 0,
              state TEXT NOT NULL DEFAULT 'PENDING', protected_receipt BLOB,
              PRIMARY KEY(tenant_id,operation_id));
            CREATE TRIGGER IF NOT EXISTS sync_evidence_no_update BEFORE UPDATE ON sync_evidence_v1
              BEGIN SELECT RAISE(ABORT,'Completed evidence is immutable'); END;
            CREATE TRIGGER IF NOT EXISTS sync_evidence_no_delete BEFORE DELETE ON sync_evidence_v1
              BEGIN SELECT RAISE(ABORT,'Completed evidence retention requires reviewed archival'); END;
            """;
        command.ExecuteNonQuery();
    }

    // Caller MUST pass its active posting transaction. This method neither commits nor starts another transaction.
    public Guid Enqueue(SqliteConnection connection, SqliteTransaction transaction, OutboxFact fact)
    {
        if (transaction.Connection != connection) throw new ArgumentException("Posting transaction must share the connection.");
        fact.Validate();
        var bytes = JsonSerializer.SerializeToUtf8Bytes(fact);
        var hash = Convert.ToHexStringLower(SHA256.HashData(bytes));
        var alias = Convert.ToHexStringLower(SHA256.HashData(JsonSerializer.SerializeToUtf8Bytes(
            new[] { fact.SourceSystem, fact.SourceInstanceId, fact.LocalOperationId })));
        using var existing = connection.CreateCommand(); existing.Transaction = transaction;
        existing.CommandText = "SELECT evidence_hash,submission_id,operation_id FROM sync_evidence_v1 WHERE tenant_id=$t AND (operation_id=$o OR alias_hash=$a)";
        AddIdentity(existing, fact.Scope.TenantId, fact.OperationId); existing.Parameters.AddWithValue("$a", alias);
        using (var reader = existing.ExecuteReader())
            if (reader.Read())
            {
                if (reader.GetString(0) != hash || reader.GetString(2) != fact.OperationId.ToString())
                    throw new InvalidOperationException("Original operation/origin reused for different evidence. Preserve and investigate.");
                return Guid.Parse(reader.GetString(1));
            }
        var submission = Guid.NewGuid();
        using var insert = connection.CreateCommand(); insert.Transaction = transaction;
        insert.CommandText = """
            INSERT INTO sync_evidence_v1 VALUES($t,$o,$company,$branch,$a,$h,$p,$s,$now);
            INSERT INTO sync_delivery_v1(tenant_id,operation_id) VALUES($t,$o);
            """;
        AddIdentity(insert, fact.Scope.TenantId, fact.OperationId);
        insert.Parameters.AddWithValue("$company", fact.Scope.CompanyId.ToString());
        insert.Parameters.AddWithValue("$branch", fact.Scope.BranchId.ToString());
        insert.Parameters.AddWithValue("$a", alias); insert.Parameters.AddWithValue("$h", hash);
        insert.Parameters.AddWithValue("$p", protector.Protect(bytes));
        insert.Parameters.AddWithValue("$s", submission.ToString());
        insert.Parameters.AddWithValue("$now", DateTimeOffset.UtcNow.ToString("O"));
        insert.ExecuteNonQuery(); return submission;
    }

    public IReadOnlyList<PendingFact> Pending(SqliteConnection connection, Scope scope, int limit = 50)
    {
        if (limit is < 1 or > 100) throw new ArgumentOutOfRangeException(nameof(limit));
        using var command = connection.CreateCommand();
        command.CommandText = """
            SELECT e.protected_evidence,e.submission_id,d.attempts FROM sync_evidence_v1 e
            JOIN sync_delivery_v1 d USING(tenant_id,operation_id)
            WHERE e.tenant_id=$t AND e.company_id=$company AND e.branch_id=$branch AND d.state='PENDING' ORDER BY e.created_at LIMIT $limit
            """;
        command.Parameters.AddWithValue("$t", scope.TenantId.ToString()); command.Parameters.AddWithValue("$limit", limit);
        command.Parameters.AddWithValue("$company", scope.CompanyId.ToString()); command.Parameters.AddWithValue("$branch", scope.BranchId.ToString());
        using var reader = command.ExecuteReader(); var facts = new List<PendingFact>();
        while (reader.Read())
        {
            var fact = JsonSerializer.Deserialize<OutboxFact>(protector.Unprotect((byte[])reader[0]))
                ?? throw new InvalidDataException("Missing evidence");
            // Never upload another company/branch's pending evidence under the current request context.
            if (fact.Scope == scope) facts.Add(new(fact, Guid.Parse(reader.GetString(1)), reader.GetInt32(2)));
        }
        return facts;
    }

    public void Attempted(SqliteConnection connection, OutboxFact fact) => Update(connection, fact,
        "UPDATE sync_delivery_v1 SET attempts=attempts+1 WHERE tenant_id=$t AND operation_id=$o AND state='PENDING'", null);

    public void RecordReviewReceipt(SqliteConnection connection, OutboxFact fact, string receipt)
    {
        using var document = JsonDocument.Parse(receipt);
        if (!document.RootElement.TryGetProperty("status", out var status) ||
            status.GetString() is not ("REVIEW_REQUIRED" or "CONFLICTING_HISTORY"))
            throw new InvalidDataException("Recovery receipt does not prove review evidence was retained.");
        var root = document.RootElement;
        if (root.GetProperty("sourceOperationId").GetGuid() != fact.OperationId ||
            root.GetProperty("evidenceId").GetGuid() == Guid.Empty || root.GetProperty("businessPosted").GetBoolean() ||
            root.GetProperty("payloadHash").GetString() != Convert.ToHexStringLower(SHA256.HashData(Encoding.UTF8.GetBytes(fact.PayloadJson))))
            throw new InvalidDataException("Recovery receipt must match the preserved original evidence and cannot post business ledgers.");
        Update(connection, fact, "UPDATE sync_delivery_v1 SET state='REVIEW_RECEIVED',protected_receipt=$r WHERE tenant_id=$t AND operation_id=$o AND state='PENDING'",
            protector.Protect(Encoding.UTF8.GetBytes(receipt)));
    }

    private static void Update(SqliteConnection c, OutboxFact f, string sql, byte[]? receipt)
    {
        using var cmd = c.CreateCommand(); cmd.CommandText = sql; AddIdentity(cmd, f.Scope.TenantId, f.OperationId);
        if (receipt != null) cmd.Parameters.AddWithValue("$r", receipt);
        cmd.ExecuteNonQuery();
    }
    private static void AddIdentity(SqliteCommand c, Guid tenant, Guid operation)
    { c.Parameters.AddWithValue("$t", tenant.ToString()); c.Parameters.AddWithValue("$o", operation.ToString()); }
}
