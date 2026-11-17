package com.quincaisoft.documents.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;import java.io.Serializable;
@Entity @Table(name="document_settings") @IdClass(DocumentSettings.Key.class)
public class DocumentSettings {
@Id @Column(name="tenant_id") public UUID tenantId;
@Id @Column(name="company_id") public UUID companyId;
@Id @Column(name="branch_id") public UUID branchId;
@Column(name="issuer_name") public String issuerName;
@Column(name="issuer_address") public String issuerAddress;
@Column(name="registration_number") public String registrationNumber;
@Column(name="fiscal_series") public String fiscalSeries;
@Column(name="locale") public String locale;
@Column(name="version") public long version;
public static class Key implements Serializable {public UUID tenantId;public UUID companyId;public UUID branchId; public Key(){} public Key(UUID tenantId,UUID companyId,UUID branchId){this.tenantId=tenantId;this.companyId=companyId;this.branchId=branchId;}public boolean equals(Object o){return o instanceof Key k && Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(branchId,k.branchId);}public int hashCode(){return Objects.hash(tenantId,companyId,branchId);}}}