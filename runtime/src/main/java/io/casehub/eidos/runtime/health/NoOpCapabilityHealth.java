package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.CapabilityHealth;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

@DefaultBean
@ApplicationScoped
public class NoOpCapabilityHealth implements CapabilityHealth {

    @Override
    public CapabilityStatus probe(String agentId, String capabilityTag, ProbeContext context) {
        return new CapabilityStatus.Ready();
    }
}
