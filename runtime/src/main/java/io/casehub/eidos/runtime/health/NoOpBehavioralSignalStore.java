package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.BehavioralSignal;
import io.casehub.eidos.api.BehavioralSignalStore;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@DefaultBean
@ApplicationScoped
public class NoOpBehavioralSignalStore implements BehavioralSignalStore {

    @Override
    public void record(final String agentId, final String tenancyId,
                       final String capabilityName, final String qualifier,
                       final BehavioralSignal signal) {}

    @Override
    public void clear(final String agentId, final String tenancyId,
                      final String capabilityName, final BehavioralSignal signal) {}

    @Override
    public Map<String, Integer> learned(final String agentId, final String tenancyId,
                                         final String capabilityName,
                                         final BehavioralSignal signal) {
        return Map.of();
    }

    @Override
    public int count(final String agentId, final String tenancyId,
                     final String capabilityName, final String qualifier,
                     final BehavioralSignal signal) {
        return 0;
    }
}
