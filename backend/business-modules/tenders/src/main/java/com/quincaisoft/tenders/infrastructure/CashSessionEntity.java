package com.quincaisoft.tenders.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="cash_session") @IdClass(CashSessionEntity.Key.class) public class CashSessionEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Column(name="branch_id") public UUID branchId;
 @Id @Column(name="id") public UUID id;
 @Column(name="till_code") public String tillCode;
 @Column(name="business_date") public LocalDate businessDate;
 @Column(name="status") public String status;
 @Column(name="opening_xaf") public long openingXaf;
 @Column(name="counted_xaf") public Long countedXaf;
 @Version @Column(name="version") public long version;
 @Column(name="actor_id") public UUID actorId;
 @Column(name="activity_sequence") public long activitySequence;
 public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable {}
}
