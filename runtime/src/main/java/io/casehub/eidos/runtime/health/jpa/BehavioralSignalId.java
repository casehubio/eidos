package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BehavioralSignalId implements Serializable {

    @Column(name = "agent_id")
    String agentId;

    @Column(name = "tenancy_id")
    String tenancyId;

    @Column(name = "capability_name")
    String capabilityName;

    @Column(name = "qualifier")
    String qualifier;

    @Column(name = "signal_type")
    String signalType;

    protected BehavioralSignalId() {}

    BehavioralSignalId(final String agentId, final String tenancyId,
                       final String capabilityName, final String qualifier,
                       final String signalType) {
        this.agentId = agentId;
        this.tenancyId = tenancyId;
        this.capabilityName = capabilityName;
        this.qualifier = qualifier;
        this.signalType = signalType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof BehavioralSignalId that)) return false;
        return Objects.equals(agentId, that.agentId)
            && Objects.equals(tenancyId, that.tenancyId)
            && Objects.equals(capabilityName, that.capabilityName)
            && Objects.equals(qualifier, that.qualifier)
            && Objects.equals(signalType, that.signalType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentId, tenancyId, capabilityName, qualifier, signalType);
    }
}
