package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AgentStateId implements Serializable {
    String agentId;
    String tenancyId;

    protected AgentStateId() {}

    AgentStateId(final String agentId, final String tenancyId) {
        this.agentId = agentId;
        this.tenancyId = tenancyId;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof AgentStateId that)) return false;
        return Objects.equals(agentId, that.agentId) && Objects.equals(tenancyId, that.tenancyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentId, tenancyId);
    }
}
