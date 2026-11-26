package io.casehub.eidos.core.health;

import io.casehub.eidos.api.DetectedRoleSignal;
import io.casehub.eidos.api.DetectedRoleSignalStore;

import java.time.Instant;
import java.util.List;

public class NoOpDetectedRoleSignalStore implements DetectedRoleSignalStore {

    @Override
    public void record(final DetectedRoleSignal signal) {}

    @Override
    public List<DetectedRoleSignal> signals(final String agentId, final String roleName,
                                             final String tenancyId, final Instant since) {
        return List.of();
    }

    @Override
    public List<DetectedRoleSignal> signalsByAgent(final String agentId,
                                                    final String tenancyId, final Instant since) {
        return List.of();
    }

    @Override
    public int purge(final String agentId, final String tenancyId, final Instant olderThan) {
        return 0;
    }

    @Override
    public void clear(final String agentId, final String tenancyId) {}
}
