using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Data.Sqlite;
using Quincai.Desktop.Sync;

var checks = 0;
void Check(bool ok, string name) { if (!ok) throw new Exception(name); checks++; Console.WriteLine($"PASS {name}"); }
void Throws<T>(Action act, string name) where T : Exception { try { act(); } catch (T) { Check(true, name); return; } throw new Exception(name); }
async Task ThrowsAsync<T>(Func<Task> act, string name) where T : Exception { try { await act(); } catch (T) { Check(true,name); return; } throw new Exception(name); }

var scope = new Scope(Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid());
using var rsa = RSA.Create(2048);
var unsigned = new OutboxFact(scope, Guid.NewGuid(), "R02", "original-install", "local-sale-123", Guid.NewGuid(), Guid.NewGuid(), 42,
    "COMPLETED_EXCEPTION", "{\"amountXaf\":\"9007199254740993\",\"quantityMilli\":\"1250\"}", "", "Restored completed sale for review");
var fact = unsigned with { SignatureBase64 = DeviceSignatures.Sign(rsa, unsigned.CanonicalEvidence) };
Check(rsa.VerifyData(Encoding.UTF8.GetBytes(fact.CanonicalEvidence), Convert.FromBase64String(fact.SignatureBase64),
    HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1), "Recovery canonical RSA signature verifies");
Check(ExactUnits.ParseNonnegative("9007199254740993") == 9007199254740993L && ExactUnits.QuantityMilli(1.250m) == 1250, "Exact XAF above JavaScript safe integer and milli quantities");
Throws<FormatException>(() => ExactUnits.ParseNonnegative("1.5"), "Reject fractional XAF");
Throws<ArgumentOutOfRangeException>(() => ExactUnits.QuantityMilli(1.0001m), "Reject unrepresentable milli quantity");
Throws<OverflowException>(() => ExactUnits.ParseNonnegative("9223372036854775808"), "Reject 64-bit overflow");
var path = Path.Combine(Path.GetTempPath(), $"quincai-sync-check-{Guid.NewGuid()}.sqlite");
using var protector = new TestProtector();
var outbox = new SqliteOutbox(protector);
using var db = new SqliteConnection($"Data Source={path};Pooling=False"); db.Open(); outbox.Initialize(db);
using (var cmd = db.CreateCommand()) { cmd.CommandText = "CREATE TABLE local_sale(id TEXT PRIMARY KEY);"; cmd.ExecuteNonQuery(); }
using (var tx = db.BeginTransaction())
{
    using var cmd = db.CreateCommand(); cmd.Transaction=tx;cmd.CommandText="INSERT INTO local_sale VALUES('rollback')";cmd.ExecuteNonQuery();
    outbox.Enqueue(db,tx,fact); tx.Rollback();
}
Check(outbox.Pending(db,scope).Count==0, "Rollback domain posting rolls back outbox evidence");
Guid submission;
using (var tx = db.BeginTransaction()) { submission=outbox.Enqueue(db,tx,fact); tx.Commit(); }
using (var tx = db.BeginTransaction()) { Check(outbox.Enqueue(db,tx,fact)==submission,"Exact retry retains submission identity");tx.Commit(); }
using (var tx = db.BeginTransaction())
{
    Throws<InvalidOperationException>(()=>outbox.Enqueue(db,tx,fact with { PayloadJson="{\"tampered\":true}" }),"Same operation cannot replace completed evidence");
    Throws<InvalidOperationException>(()=>outbox.Enqueue(db,tx,fact with { OperationId=Guid.NewGuid() }),"Origin alias cannot be re-uploaded as a fresh operation");tx.Rollback();
}
using(var cmd=db.CreateCommand()){cmd.CommandText="UPDATE sync_evidence_v1 SET evidence_hash='changed'";Throws<SqliteException>(()=>cmd.ExecuteNonQuery(),"SQLite trigger prevents evidence mutation");}
using(var cmd=db.CreateCommand()){cmd.CommandText="SELECT protected_evidence FROM sync_evidence_v1";Check(!Encoding.UTF8.GetString((byte[])cmd.ExecuteScalar()!).Contains("amountXaf"),"Payload protected at rest");}
db.Close();db.Open();
Check(outbox.Pending(db,scope).Single().Fact == fact, "Process reopen preserves original epoch sequence signature and completed fact");
Check(outbox.Pending(db,scope with { BranchId=Guid.NewGuid() }).Count==0,"Different branch cannot drain original evidence");
var keyStore=new ProtectedDeviceKey(protector);var protectedKey=keyStore.Create(out var publicPem);
using(var reopened=keyStore.Open(protectedKey)) Check(reopened.ExportSubjectPublicKeyInfoPem()==publicPem,"Protected RSA device key roundtrip");
var tokens=new Tokens();var handler=new Handler();using var http=new HttpClient(handler);
var standalone=new ConnectionPolicy();var offClient=new SyncClient(http,new Uri("https://example.invalid/"),tokens,scope,standalone);
Check(await new RecoveryPump(standalone,outbox,offClient,scope).RunOnceAsync(db)==0&&handler.Calls==0&&tokens.Calls==0,"Standalone startup needs no token or cloud request");
await ThrowsAsync<InvalidOperationException>(()=>offClient.ChallengeAsync(new(Guid.NewGuid(),"desk",publicPem)),"Explicit owner/native/cutover gates enforced");
Check(!standalone.CanSellOffline && !new ConnectionPolicy(ConnectionMode.Connected,true,true,true).CanSellOffline,"Enrollment cannot authorize offline selling without signed exclusive grants");
var connected=new ConnectionPolicy(ConnectionMode.Connected,true,true,true);var client=new SyncClient(http,new Uri("https://example.invalid/"),tokens,scope,connected);
var pump=new RecoveryPump(connected,outbox,client,scope);
handler.Fail=true;await ThrowsAsync<HttpRequestException>(()=>pump.RunOnceAsync(db),"Network uncertainty retains evidence for retry");
Check(outbox.Pending(db,scope).Single().SubmissionId==submission && outbox.Pending(db,scope).Single().Attempts==1,"Uncertain retry preserves ID and records attempt");
handler.Fail=false;
handler.Reply=JsonSerializer.Serialize(new { evidenceId=Guid.NewGuid(),sourceOperationId=fact.OperationId,status="REVIEW_REQUIRED",payloadHash=Convert.ToHexStringLower(SHA256.HashData(Encoding.UTF8.GetBytes(fact.PayloadJson))),receivedAt=DateTimeOffset.UtcNow,businessPosted=false });
Check(await pump.RunOnceAsync(db)==1&&outbox.Pending(db,scope).Count==0,"Durable matched review receipt ends automatic retries without ledger posting");
var sent=JsonDocument.Parse(handler.LastBody!);
Check(sent.RootElement.GetProperty("deviceEpoch").GetGuid()==fact.DeviceEpoch&&sent.RootElement.GetProperty("sourceOperationId").GetGuid()==fact.OperationId&&sent.RootElement.GetProperty("operationId").GetGuid()==submission,"Wire retains original UUID epoch and distinct stable submission ID");
Check(handler.LastId==submission.ToString()&&handler.RequestId!=null&&handler.LastPath=="/api/v1/sync/recovery-submissions","Java route and idempotency/request headers match");
Throws<InvalidDataException>(()=>outbox.RecordReviewReceipt(db,fact,handler.Reply.Replace(fact.OperationId.ToString(),Guid.NewGuid().ToString())),"Wrong source receipt cannot acknowledge evidence");
using(var cmd=db.CreateCommand()){cmd.CommandText="SELECT count(*) FROM sync_evidence_v1";Check((long)cmd.ExecuteScalar()! == 1,"Reviewed completed evidence is retained permanently");}
db.Close();File.Delete(path);Console.WriteLine($"{checks} checks passed");

sealed class Tokens:IAccessTokenProvider { public int Calls;public Task<string> GetAccessTokenAsync(CancellationToken ct){Calls++;return Task.FromResult("test-access-token");} }
sealed class Handler:HttpMessageHandler
{
    public int Calls;public bool Fail;public string Reply="{}";public string? LastBody,LastId,LastPath,RequestId;
    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request,CancellationToken ct)
    {Calls++;LastBody=request.Content==null?null:await request.Content.ReadAsStringAsync(ct);LastPath=request.RequestUri!.AbsolutePath;LastId=request.Headers.GetValues("Idempotency-Key").Single();RequestId=request.Headers.GetValues("X-Request-ID").Single();if(Fail)throw new HttpRequestException("uncertain response");return new(HttpStatusCode.OK){Content=new StringContent(Reply)};}
}
// Test-only ephemeral AES key; production uses WindowsUserProtector (DPAPI current user).
sealed class TestProtector:ILocalProtector,IDisposable
{
    private readonly byte[] key=RandomNumberGenerator.GetBytes(32);
    public byte[] Protect(byte[] source){var nonce=RandomNumberGenerator.GetBytes(12);var tag=new byte[16];var cipher=new byte[source.Length];using var aes=new AesGcm(key,16);aes.Encrypt(nonce,source,cipher,tag);return [..nonce,..tag,..cipher];}
    public byte[] Unprotect(byte[] bytes){var plain=new byte[bytes.Length-28];using var aes=new AesGcm(key,16);aes.Decrypt(bytes.AsSpan(0,12),bytes.AsSpan(28),bytes.AsSpan(12,16),plain);return plain;}
    public void Dispose()=>CryptographicOperations.ZeroMemory(key);
}
