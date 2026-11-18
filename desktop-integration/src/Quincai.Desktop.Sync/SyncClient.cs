using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.Data.Sqlite;

namespace Quincai.Desktop.Sync;

/// <summary>Native OIDC/PKCE host supplies an access token; adapter never stores passwords or refresh tokens.</summary>
public interface IAccessTokenProvider { Task<string> GetAccessTokenAsync(CancellationToken cancellationToken); }

public sealed class SyncClient
{
    public static HttpClient CreateTransport() => new(new SocketsHttpHandler { AllowAutoRedirect = false })
        { Timeout = TimeSpan.FromSeconds(30), MaxResponseContentBufferSize = 1_000_000 };
    private readonly HttpClient http;
    private readonly Uri endpoint;
    private readonly IAccessTokenProvider tokens;
    private readonly Scope scope;
    private readonly ConnectionPolicy policy;
    public SyncClient(HttpClient http, Uri endpoint, IAccessTokenProvider tokens, Scope scope, ConnectionPolicy policy)
    {
        if (endpoint.Scheme != Uri.UriSchemeHttps || endpoint.UserInfo.Length != 0 || endpoint.Query.Length != 0 || endpoint.Fragment.Length != 0)
            throw new ArgumentException("An HTTPS service endpoint without credentials/query/fragment is required.");
        this.http = http; this.endpoint = endpoint; this.tokens = tokens; this.scope = scope; this.policy = policy;
    }
    public Task<string> ChallengeAsync(DeviceChallengeRequest request, CancellationToken ct = default)
        => SendAsync(HttpMethod.Post, "api/v1/sync/devices/challenges", request, request.OperationId, ct);
    public Task<string> ProveAsync(Guid device, DeviceProof proof, CancellationToken ct = default)
        => SendAsync(HttpMethod.Post, $"api/v1/sync/devices/{device}/prove", proof, proof.OperationId, ct);
    public Task<string> OperationStatusAsync(Guid operation, CancellationToken ct = default)
        => SendAsync(HttpMethod.Get, $"api/v1/sync/operations/{operation}", null, null, ct);
    public Task<string> SubmitRecoveryAsync(PendingFact pending, CancellationToken ct = default)
    {
        if (pending.Fact.Scope != scope) throw new UnauthorizedAccessException("Evidence scope differs from connected context.");
        return SendAsync(HttpMethod.Post, "api/v1/sync/recovery-submissions", pending.Fact.ForRecovery(pending.SubmissionId), pending.SubmissionId, ct);
    }
    private async Task<string> SendAsync(HttpMethod method, string path, object? body, Guid? operation, CancellationToken ct)
    {
        if (!policy.CanConnect) throw new InvalidOperationException("Cloud connection requires explicit owner consent and native/cutover gates.");
        using var request = new HttpRequestMessage(method, new Uri(endpoint, path));
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", await tokens.GetAccessTokenAsync(ct));
        request.Headers.Add("X-Request-ID", Guid.NewGuid().ToString());
        request.Headers.Add("X-Tenant-Id", scope.TenantId.ToString());
        request.Headers.Add("X-Company-Id", scope.CompanyId.ToString());
        request.Headers.Add("X-Branch-Id", scope.BranchId.ToString());
        if (operation.HasValue) request.Headers.Add("Idempotency-Key", operation.Value.ToString());
        if (body != null) request.Content = JsonContent.Create(body, options: new JsonSerializerOptions(JsonSerializerDefaults.Web));
        using var response = await http.SendAsync(request, HttpCompletionOption.ResponseContentRead, ct);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadAsStringAsync(ct);
    }
}

/// <summary>Called by an opt-in background host. No scheduler, listener or network startup side effects.</summary>
public sealed class RecoveryPump(ConnectionPolicy policy, SqliteOutbox outbox, SyncClient client, Scope scope)
{
    private readonly SemaphoreSlim gate = new(1, 1);
    public async Task<int> RunOnceAsync(SqliteConnection connection, CancellationToken ct = default)
    {
        if (!policy.CanConnect) return 0;
        await gate.WaitAsync(ct);
        try
        {
            var delivered = 0;
            foreach (var pending in outbox.Pending(connection, scope))
            {
                ct.ThrowIfCancellationRequested(); outbox.Attempted(connection, pending.Fact);
                // Failure leaves original evidence and submission UUID intact for an identical retry.
                var receipt = await client.SubmitRecoveryAsync(pending, ct);
                outbox.RecordReviewReceipt(connection, pending.Fact, receipt); delivered++;
            }
            return delivered;
        }
        finally { gate.Release(); }
    }
}
