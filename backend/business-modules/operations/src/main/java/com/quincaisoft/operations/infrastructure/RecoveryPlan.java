package com.quincaisoft.operations.infrastructure;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="recovery_plan") @IdClass(ScopeKey.class)
public class RecoveryPlan extends ScopedRecord {
 @Column(name="incident_reference") public String incidentReference;
 @Column(name="target_point") public Instant targetPoint;
 @Column(name="source_artifact_reference") public String sourceArtifactReference;
 @Column(name="preservation_reason") public String preservationReason;
 public String status;
 @Column(name="scope_approved_by") public UUID scopeApprovedBy;
 @Column(name="reviewed_by") public UUID reviewedBy;
 @Column(name="next_batch") public int nextBatch;
 @Column(name="candidate_count") public long candidateCount;
 @Column(name="match_count") public long matchCount;
 @Column(name="difference_count") public long differenceCount;
 @Column(name="fence_decision_id") public UUID fenceDecisionId;
 @Column(name="fence_epoch") public Long fenceEpoch;
 @Column(name="candidate_artifact_sha256") public String candidateArtifactSha256;
 @Column(name="egress_evidence_reference") public String egressEvidenceReference;
 @Column(name="compared_at") public Instant comparedAt;
 @Column(name="created_at") public Instant createdAt;
}
