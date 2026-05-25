package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.DegradationReason;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Optional;

@DefaultBean
@ApplicationScoped
public class NoOpAgentStateStore implements AgentStateStore {

    @Override
    public void record(final String agentId, final DegradationReason reason, final Instant expiresAt) {}

    @Override
    public Optional<DegradationReason> query(final String agentId) {
        return Optional.empty();
    }

    @Override
    public void clear(final String agentId) {}
}
