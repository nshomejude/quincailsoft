package com.quincaisoft.inventory.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="custody_collection") @IdClass(CustodyCollection.Key.class) public class CustodyCollection {
@Id @Column(name="tenant_id")public UUID tenantId;@Id @Column(name="company_id")public UUID companyId;@Id public UUID id;@Column(name="branch_id")public UUID branchId;
@Column(name="custody_id") public UUID custodyId;
@Column(name="operation_id") public UUID operationId;
@Column(name="quantity_milli") public long quantityMilli;
@Column(name="proof_reference") public String proofReference;
@Column(name="actor_id") public UUID actorId;
@Column(name="collected_at") public Instant collectedAt;
public record Key(UUID tenantId,UUID companyId,UUID id)implements Serializable{}
}
