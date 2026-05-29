package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "agent_degradation_state")
public class AgentDegradationStateEntity {

    @Id
    @Column(name = "agent_id")
    private String agentId;

    @Column(name = "degradation_reason", nullable = false)
    private String degradationReason;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant expiresAt;

    protected AgentDegradationStateEntity() {}

    AgentDegradationStateEntity(final String agentId, final String degradationReason, final Instant expiresAt) {
        this.agentId = agentId;
        this.degradationReason = degradationReason;
        this.expiresAt = expiresAt;
    }

    String getAgentId()           { return agentId; }
    String getDegradationReason() { return degradationReason; }
    Instant getExpiresAt()        { return expiresAt; }
}
