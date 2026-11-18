package com.quincaisoft.operations.api;
import java.util.UUID;import java.time.Instant;
import com.quincaisoft.shared.TenantMutation;
/** Clears only expired operational export payloads, preserving source and receipt facts. */
public interface ExportRetentionService {
 record Request(UUID operationId,UUID exportId,String reason) {}
 record Command(UUID operationId,String version,String reason) {}
 record Decision(UUID id,UUID exportId,String digest,String status,UUID requestedBy,UUID reviewedBy,String version,Instant expiresAt,String reason,Instant purgedAt) {}
 @TenantMutation Decision request(Request input);
 @TenantMutation Decision approve(UUID id,Command input);
 @TenantMutation Decision execute(UUID id,Command input);
 Decision get(UUID id);
}
