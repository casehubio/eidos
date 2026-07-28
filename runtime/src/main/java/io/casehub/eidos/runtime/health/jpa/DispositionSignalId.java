package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DispositionSignalId implements Serializable {

    @Column(name = "agent_id")
    String agentId;

    @Column(name = "tenancy_id")
    String tenancyId;

    @Column(name = "function_term")
    String functionTerm;

    protected DispositionSignalId() {}

    DispositionSignalId(final String agentId, final String tenancyId,
                        final String functionTerm) {
        this.agentId = agentId;
        this.tenancyId = tenancyId;
        this.functionTerm = functionTerm;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof DispositionSignalId that)) return false;
        return Objects.equals(agentId, that.agentId)
            && Objects.equals(tenancyId, that.tenancyId)
            && Objects.equals(functionTerm, that.functionTerm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentId, tenancyId, functionTerm);
    }
}
