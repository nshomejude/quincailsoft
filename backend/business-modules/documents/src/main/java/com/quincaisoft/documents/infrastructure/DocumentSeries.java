package com.quincaisoft.documents.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="document_series") @IdClass(DocumentSeries.Key.class)
public class DocumentSeries {
@Id @Column(name="tenant_id") public UUID tenantId;
@Id @Column(name="company_id") public UUID companyId;
@Id @Column(name="branch_id") public UUID branchId;
@Id @Column(name="kind") public String kind;
@Id @Column(name="fiscal_series") public String fiscalSeries;
@Column(name="next_number") public long nextNumber;
public static class Key implements Serializable {public UUID tenantId;public UUID companyId;public UUID branchId;public String kind;public String fiscalSeries; public Key(){} public Key(UUID tenantId,UUID companyId,UUID branchId,String kind,String fiscalSeries){this.tenantId=tenantId;this.companyId=companyId;this.branchId=branchId;this.kind=kind;this.fiscalSeries=fiscalSeries;}public boolean equals(Object o){return o instanceof Key k && Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(branchId,k.branchId)&&Objects.equals(kind,k.kind)&&Objects.equals(fiscalSeries,k.fiscalSeries);}public int hashCode(){return Objects.hash(tenantId,companyId,branchId,kind,fiscalSeries);}}}