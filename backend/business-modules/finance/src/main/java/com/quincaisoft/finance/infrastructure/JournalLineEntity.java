package com.quincaisoft.finance.infrastructure;
import jakarta.persistence.*;
import java.util.*;
import java.time.*;
import java.io.Serializable;
@Entity @Table(name="journal_line") @IdClass(JournalLineEntity.Key.class)
public class JournalLineEntity {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id @Column(name="entry_id") public UUID entryId;
 @Id @Column(name="id") public UUID id;
 @Column(name="account_code") public String accountCode;
 @Column(name="debit_xaf") public long debitXaf;
 @Column(name="credit_xaf") public long creditXaf;
 public JournalLineEntity() {}
 public static class Key implements Serializable {
  public UUID tenantId;
  public UUID companyId;
  public UUID entryId;
  public UUID id;
  public Key() {}
  public Key(UUID tenantId,UUID companyId,UUID entryId,UUID id) {this.tenantId=tenantId;this.companyId=companyId;this.entryId=entryId;this.id=id;}
  public boolean equals(Object o) {return o instanceof Key k && Objects.equals(tenantId,k.tenantId) && Objects.equals(companyId,k.companyId) && Objects.equals(entryId,k.entryId) && Objects.equals(id,k.id);}
  public int hashCode(){return Objects.hash(tenantId,companyId,entryId,id);}
 }
}
