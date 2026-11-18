package com.quincaisoft.platformcontrol.infrastructure;import jakarta.persistence.*;import java.util.*;import java.time.*;
@Entity @Table(name="platform_tenant_lifecycle")public class TenantLifecycle{@Id @Column(name="tenant_id")public UUID tenantId;public String status;public long epoch;@Column(name="last_decision_id")public UUID lastDecisionId;}
