package com.quincaisoft.identity.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;
@Entity @Table(name="identity_revocation") @IdClass(MembershipChange.Key.class)public class RevocationRecord {
 @Id @Column(name="tenant_id")public UUID tenantId;@Id @Column(name="company_id")public UUID companyId;@Id public UUID id;
 @Column(name="branch_id")public UUID branchId;@Column(name="actor_id")public UUID actorId;@Column(name="target_actor")public UUID targetActor;
 @Column(name="expected_epoch")public long expectedEpoch;public String reason;@Column(name="created_at")public Instant createdAt;
}
