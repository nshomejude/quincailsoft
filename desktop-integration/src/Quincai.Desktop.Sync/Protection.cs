using System.Runtime.Versioning;
using System.Security.Cryptography;
using System.Text;

namespace Quincai.Desktop.Sync;

/// <summary>Host supplies protected storage; no plaintext production fallback.</summary>
public interface ILocalProtector
{
    byte[] Protect(byte[] plaintext);
    byte[] Unprotect(byte[] ciphertext);
}

[SupportedOSPlatform("windows")]
public sealed class WindowsUserProtector : ILocalProtector
{
    private static readonly byte[] Entropy = Encoding.UTF8.GetBytes("Quincai.Desktop.Sync/v1");
    public byte[] Protect(byte[] plaintext) => ProtectedData.Protect(plaintext, Entropy, DataProtectionScope.CurrentUser);
    public byte[] Unprotect(byte[] ciphertext) => ProtectedData.Unprotect(ciphertext, Entropy, DataProtectionScope.CurrentUser);
}

public sealed class ProtectedDeviceKey(ILocalProtector protector)
{
    public byte[] Create(out string publicKeyPem)
    {
        using var key = RSA.Create(3072);
        publicKeyPem = key.ExportSubjectPublicKeyInfoPem();
        var bytes = key.ExportPkcs8PrivateKey();
        try { return protector.Protect(bytes); }
        finally { CryptographicOperations.ZeroMemory(bytes); }
    }
    public RSA Open(byte[] protectedKey)
    {
        var bytes = protector.Unprotect(protectedKey);
        var key = RSA.Create();
        try { key.ImportPkcs8PrivateKey(bytes, out _); return key; }
        catch { key.Dispose(); throw; }
        finally { CryptographicOperations.ZeroMemory(bytes); }
    }
}
