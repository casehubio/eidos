package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CapabilitySpecializationId implements Serializable {

    @Column(name = "agent_id")
    String agentId;

    @Column(name = "tenancy_id")
    String tenancyId;

    @Column(name = "capability_name")
    String capabilityName;

    @Column(name = "domain")
    String domain;

    protected CapabilitySpecializationId() {}

    CapabilitySpecializationId(String agentId, String tenancyId,
                                String capabilityName, String domain) {
        this.agentId = agentId;
        this.tenancyId = tenancyId;
        this.capabilityName = capabilityName;
        this.domain = domain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CapabilitySpecializationId that)) return false;
        return Objects.equals(agentId, that.agentId)
            && Objects.equals(tenancyId, that.tenancyId)
            && Objects.equals(capabilityName, that.capabilityName)
            && Objects.equals(domain, that.domain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentId, tenancyId, capabilityName, domain);
    }
}
