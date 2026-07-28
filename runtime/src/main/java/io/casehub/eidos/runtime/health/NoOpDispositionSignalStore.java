package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.DispositionSignalStore;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@DefaultBean
@ApplicationScoped
public class NoOpDispositionSignalStore implements DispositionSignalStore {

    @Override
    public void recordActivation(final String agentId, final String tenancyId,
                                  final String functionTerm) {}

    @Override
    public Map<String, Integer> activationCounts(final String agentId,
                                                  final String tenancyId) {
        return Map.of();
    }

    @Override
    public void decay(final String agentId, final String tenancyId,
                      final double decayFactor) {}

    @Override
    public void clear(final String agentId, final String tenancyId) {}
}
