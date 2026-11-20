package com.quincaisoft.platformcontrol.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.*;
@Entity @Table(name="tenant") public class ProvisionedTenant {
 @Id public UUID id;public String name;@Column(name="created_at") public Instant createdAt;
}
