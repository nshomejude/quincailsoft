using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace Quincai.Desktop.Sync;

public sealed record Scope(Guid TenantId, Guid CompanyId, Guid BranchId);
public enum ConnectionMode { Standalone, Connected }
public sealed record ConnectionPolicy(ConnectionMode Mode = ConnectionMode.Standalone,
    bool OwnerConsent = false, bool NativeGatesPassed = false, bool ReconciledCutover = false)
{
    public bool CanConnect => Mode == ConnectionMode.Connected && OwnerConsent && NativeGatesPassed && ReconciledCutover;
    // The server currently issues no signed exclusive inventory grants. Enrollment is not selling authority.
    public bool CanSellOffline => false;
}

/// <summary>Integer-only XAF and thousandths; no binary floating point or implicit rounding.</summary>
public static class ExactUnits
{
    public static long ParseNonnegative(string value)
    {
        if (value.Length == 0 || (value.Length > 1 && value[0] == '0') || value.Any(c => c < '0' || c > '9'))
            throw new FormatException("A canonical nonnegative integer string is required.");
        return long.Parse(value, NumberStyles.None, CultureInfo.InvariantCulture);
    }
    public static string Wire(long value) => value >= 0 ? value.ToString(CultureInfo.InvariantCulture) : throw new ArgumentOutOfRangeException(nameof(value));
    public static long QuantityMilli(decimal units) => units >= 0 && decimal.Truncate(units * 1000m) == units * 1000m
        ? checked((long)(units * 1000m)) : throw new ArgumentOutOfRangeException(nameof(units));
}

public sealed record DeviceChallengeRequest(Guid OperationId, string Name, string PublicKeyPem);
public sealed record DeviceChallenge(Guid Id, Guid Epoch, Guid ActorId, string Name, string Status, string Challenge,
    DateTimeOffset ExpiresAt, string Fingerprint, long Version, bool OfflineSelling);
public sealed record DeviceProof(Guid OperationId, long Version, string SignatureBase64);
public sealed record RecoverySubmission(Guid OperationId, Guid SourceOperationId, Guid DeviceId,
    Guid DeviceEpoch, long Sequence, string Classification, string PayloadJson, string SignatureBase64, string Reason);

/// <summary>Original signed evidence, never rewritten after restore or a new device enrollment.</summary>
public sealed record OutboxFact(Scope Scope, Guid OperationId, string SourceSystem, string SourceInstanceId,
    string LocalOperationId, Guid DeviceId, Guid DeviceEpoch, long Sequence, string Classification,
    string PayloadJson, string SignatureBase64, string Reason)
{
    public void Validate()
    {
        if (Scope.TenantId == Guid.Empty || Scope.CompanyId == Guid.Empty || Scope.BranchId == Guid.Empty ||
            OperationId == Guid.Empty || DeviceId == Guid.Empty || DeviceEpoch == Guid.Empty || Sequence < 1 || Sequence > 9_007_199_254_740_991L)
            throw new ArgumentException("Complete original scope, operation and epoch/sequence are required.");
        foreach (var value in new[] { SourceSystem, SourceInstanceId, LocalOperationId })
            if (string.IsNullOrWhiteSpace(value) || value.Contains('\n') || value.Length > 512) throw new ArgumentException("Invalid origin/reason.");
        if (Classification is not ("COMPLETED_AUTHORIZED" or "COMPLETED_EXCEPTION"))
            throw new ArgumentException("Recovery outbox only preserves completed facts; drafts are separate.");
        if (Reason.Trim().Length < 3 || Reason.Length > 2000) throw new ArgumentException("Bounded recovery reason is required.");
        using var payload = JsonDocument.Parse(PayloadJson);
        if (payload.RootElement.ValueKind != JsonValueKind.Object || PayloadJson.Length > 250_000)
            throw new ArgumentException("Bounded JSON object evidence is required.");
        if (SignatureBase64.Length > 2048 || Convert.FromBase64String(SignatureBase64).Length < 256) throw new ArgumentException("Bounded RSA signature required.");
    }
    public string CanonicalEvidence => string.Join('\n', "quincai-recovery-v1", Scope.TenantId, Scope.CompanyId,
        Scope.BranchId, DeviceId, DeviceEpoch.ToString(), Sequence.ToString(CultureInfo.InvariantCulture),
        OperationId, Classification, Convert.ToHexStringLower(SHA256.HashData(Encoding.UTF8.GetBytes(PayloadJson))));
    public RecoverySubmission ForRecovery(Guid submissionId) => new(submissionId, OperationId, DeviceId,
        DeviceEpoch, Sequence, Classification, PayloadJson, SignatureBase64, Reason);
}

public static class DeviceSignatures
{
    public static string Enrollment(Scope scope, DeviceChallenge challenge, RSA key) => Sign(key,
        string.Join('\n', "quincai-enroll-v1", scope.TenantId, scope.CompanyId, scope.BranchId,
            challenge.Id, challenge.Epoch.ToString(), challenge.Challenge));
    public static string Sign(RSA key, string canonical) => Convert.ToBase64String(key.SignData(
        Encoding.UTF8.GetBytes(canonical), HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1));
}
