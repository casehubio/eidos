package io.casehub.eidos.memory;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.DegradationReason;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryAgentStateStore implements AgentStateStore {

    private final ConcurrentHashMap<StateKey, ExpiringState> store = new ConcurrentHashMap<>();

    @Override
    public void record(final String agentId, final String tenancyId,
                       final DegradationReason reason, final Instant expiresAt) {
        store.put(new StateKey(agentId, tenancyId), new ExpiringState(reason, expiresAt));
    }

    @Override
    public Optional<DegradationReason> query(final String agentId, final String tenancyId) {
        return Optional.ofNullable(store.get(new StateKey(agentId, tenancyId)))
                .filter(s -> Instant.now().isBefore(s.expiresAt()))
                .map(ExpiringState::reason);
    }

    @Override
    public void clear(final String agentId, final String tenancyId) {
        store.remove(new StateKey(agentId, tenancyId));
    }

    private record StateKey(String agentId, String tenancyId) {}
    private record ExpiringState(DegradationReason reason, Instant expiresAt) {}
}
