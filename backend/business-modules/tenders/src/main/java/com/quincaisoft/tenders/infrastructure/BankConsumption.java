package com.quincaisoft.tenders.infrastructure;
import jakarta.persistence.*;
import java.util.UUID;
import java.io.Serializable;
/** Immutable typed consumer; deferred foreign keys require the concrete source in the same transaction. */
@Entity @Table(name="bank_consumption") @IdClass(BankConsumption.Key.class)
public class BankConsumption {
 @Id @Column(name="tenant_id") public UUID tenantId;
 @Id @Column(name="company_id") public UUID companyId;
 @Id public UUID id;
 @Column(name="branch_id") public UUID branchId;
 @Column(name="evidence_id") public UUID evidenceId;
 @Column(name="settlement_id") public UUID settlementId;
 @Column(name="payroll_payment_id") public UUID payrollPaymentId;
 public record Key(UUID tenantId,UUID companyId,UUID id) implements Serializable {}
}
