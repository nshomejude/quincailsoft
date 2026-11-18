package com.quincaisoft.documents.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="document_snapshot") @IdClass(DocumentSnapshot.Key.class)
public class DocumentSnapshot {
@Id @Column(name="tenant_id") public UUID tenantId;
@Id @Column(name="company_id") public UUID companyId;
@Id @Column(name="id") public UUID id;
@Column(name="branch_id") public UUID branchId;
@Column(name="source_id") public UUID sourceId;
@Column(name="operation_id") public UUID operationId;
@Column(name="number") public String number;
@Column(name="kind") public String kind;
@Column(name="business_date") public LocalDate businessDate;
@Column(name="total_xaf") public long totalXaf;
@Column(name="sha256") public String sha256;
@Column(name="content") public String content;
@Column(name="issued_at") public Instant issuedAt;
public static class Key implements Serializable {public UUID tenantId;public UUID companyId;public UUID id; public Key(){} public Key(UUID tenantId,UUID companyId,UUID id){this.tenantId=tenantId;this.companyId=companyId;this.id=id;}public boolean equals(Object o){return o instanceof Key k && Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}public int hashCode(){return Objects.hash(tenantId,companyId,id);}}}