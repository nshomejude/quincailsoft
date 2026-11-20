package com.quincaisoft.operations.infrastructure;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="recovery_candidate_receipt") @IdClass(ScopeKey.class)
public class CandidateRecoveryReceipt extends ScopedRecord {
 @Column(name="plan_id") public UUID planId;
 @Column(name="operation_id") public UUID operationId;
 @Column(name="request_hash") public String requestHash;
 @Column(name="source_type") public String sourceType;
 @Column(name="source_id") public UUID sourceId;
 @Column(name="response_digest") public String responseDigest;
}
