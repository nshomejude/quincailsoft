package com.quincaisoft.platformcontrol.infrastructure;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="platform_onboarding_command")
public class OnboardingCommand {
    @Id @Column(name="operation_id") public UUID operationId;
    @Column(name="request_id") public UUID requestId;
    @Column(name="actor_id") public UUID actorId;
    @Column(name="request_hash") public String requestHash;
    @Column(name="response_text") public String responseText;
    @Column(name="created_at") public Instant createdAt;
}
