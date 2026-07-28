package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.DispositionHealth;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@DefaultBean
@ApplicationScoped
public class NoOpDispositionHealth implements DispositionHealth {

    @Override
    public DispositionStatus probe(final AgentDescriptor descriptor,
                                    final CapabilityHealth.ProbeContext context) {
        return new DispositionStatus.Aligned(Map.of());
    }
}
