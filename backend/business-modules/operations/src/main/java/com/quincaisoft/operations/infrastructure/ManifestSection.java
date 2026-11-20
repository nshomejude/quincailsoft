package com.quincaisoft.operations.infrastructure;
import jakarta.persistence.*;import java.util.*;import java.time.Instant;
@Entity @Table(name="operation_manifest_section") @IdClass(ScopeKey.class) public class ManifestSection extends ScopedRecord {
 @Column(name="manifest_id")public UUID manifestId;
 @Column(name="name")public String name;
 @Column(name="row_count")public long rowCount;
 @Column(name="digest")public String digest;
}
