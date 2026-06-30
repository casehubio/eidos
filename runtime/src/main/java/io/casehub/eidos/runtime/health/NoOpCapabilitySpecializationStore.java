package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.CapabilitySpecializationStore;
import io.casehub.eidos.api.SpecializationSignal;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@DefaultBean
@ApplicationScoped
public class NoOpCapabilitySpecializationStore implements CapabilitySpecializationStore {

    @Override
    public void record(final String agentId, final String tenancyId,
                       final String capabilityName, final String domain,
                       final SpecializationSignal signal) {}

    @Override
    public void clear(final String agentId, final String tenancyId,
                      final String capabilityName, final SpecializationSignal signal) {}

    @Override
    public Map<String, Integer> learned(final String agentId, final String tenancyId,
                                         final String capabilityName,
                                         final SpecializationSignal signal) {
        return Map.of();
    }

    @Override
    public int count(final String agentId, final String tenancyId,
                     final String capabilityName, final String domain,
                     final SpecializationSignal signal) {
        return 0;
    }
}
