package com.quincaisoft.operations.api;
import java.time.Instant;
import java.util.*;

/** Receipt reconciliation and review only. No live routing change, ledger rewrite, restore or provider replay. */
public interface RecoveryService {
 record Request(UUID operationId,String incidentReference,Instant targetPoint,String sourceArtifactReference,String preservationReason,String reason) {}
 record Review(UUID operationId,String version,String reason) {}
 record Plan(UUID id,String incidentReference,Instant targetPoint,String sourceArtifactReference,String status,
             UUID requestedBy,UUID scopeApprovedBy,UUID reviewedBy,String version,String nextBatch,
             String candidateReceipts,String matchingReceipts,String differences,UUID fenceDecisionId,String fenceEpoch,
             String candidateArtifactSha256,String egressEvidenceReference,Instant comparedAt,
             String preservationDirective,boolean applied,boolean productionCutoverAllowed) {}
 record Receipt(UUID operationId,String requestHash,String sourceType,UUID sourceId,String responseDigest) {}
 record ReceiptPage(Instant asOf,List<Receipt> receipts,UUID nextCursor,boolean hasMore) {}
 record Batch(UUID operationId,String version,int sequence,List<Receipt> receipts,String reason) {}
 record Compare(UUID operationId,String version,UUID fenceDecisionId,long fenceEpoch,String candidateArtifactSha256,String egressEvidenceReference,String reason) {}
 record Difference(UUID operationId,String classification,String liveRequestHash,String candidateRequestHash) {}
 Plan request(Request input);
 Plan approveScope(UUID id,Review input);
 Plan get(UUID id);
 List<Plan> list(int offset,int limit);
 ReceiptPage retainedReceipts(Instant asOf,UUID after,int limit);
 Plan append(UUID id,Batch input);
 Plan compare(UUID id,Compare input);
 List<Difference> differences(UUID id,UUID after,int limit);
 Plan approveReceiptReview(UUID id,Review input);
 Plan cancel(UUID id,Review input);
}
