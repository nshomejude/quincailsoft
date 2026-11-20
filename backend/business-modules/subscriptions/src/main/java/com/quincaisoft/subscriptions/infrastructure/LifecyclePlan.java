package com.quincaisoft.subscriptions.infrastructure;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="subscription_plan")
public class LifecyclePlan {
    @Id @Column(name="code") public String code;
    @Column(name="name") public String name;
    @Column(name="monthly_price_xaf") public long monthlyPriceXaf;
    @Column(name="seat_limit") public int seatLimit;
    @Column(name="branch_limit") public int branchLimit;
    @Column(name="warehouse_limit") public int warehouseLimit;
    @Column(name="commerce_access") public boolean commerceAccess;
    @Column(name="effective_at") public Instant effectiveAt;
}
