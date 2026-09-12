package io.casehub.eidos.core.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.DispositionHealth;


import java.util.Map;


public class NoOpDispositionHealth implements DispositionHealth {

    @Override
    public DispositionStatus probe(final AgentDescriptor descriptor,
                                    final CapabilityHealth.ProbeContext context) {
        return new DispositionStatus.Aligned(Map.of());
    }
}
