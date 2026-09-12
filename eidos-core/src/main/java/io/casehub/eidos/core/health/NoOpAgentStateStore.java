package io.casehub.eidos.core.health;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.DegradationReason;

import java.time.Instant;
import java.util.Optional;

public class NoOpAgentStateStore implements AgentStateStore {

    @Override
    public void record(final String agentId, final String tenancyId,
                       final DegradationReason reason, final Instant expiresAt) {}

    @Override
    public Optional<DegradationReason> query(final String agentId, final String tenancyId) {
        return Optional.empty();
    }

    @Override
    public void clear(final String agentId, final String tenancyId) {}
}
