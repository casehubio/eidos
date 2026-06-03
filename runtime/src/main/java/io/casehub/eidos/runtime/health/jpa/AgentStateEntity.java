package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "agent_state")
public class AgentStateEntity {

    @EmbeddedId
    @AttributeOverrides({
        @AttributeOverride(name = "agentId",   column = @Column(name = "agent_id")),
        @AttributeOverride(name = "tenancyId", column = @Column(name = "tenancy_id"))
    })
    AgentStateId id;

    @Column(name = "degradation", nullable = false)
    String degradation;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    protected AgentStateEntity() {}

    AgentStateEntity(final String agentId, final String tenancyId,
                     final String degradation, final Instant expiresAt) {
        this.id = new AgentStateId(agentId, tenancyId);
        this.degradation = degradation;
        this.expiresAt = expiresAt;
    }

    String getAgentId()    { return id.agentId; }
    String getTenancyId()  { return id.tenancyId; }
    String getDegradation(){ return degradation; }
    Instant getExpiresAt() { return expiresAt; }
}
