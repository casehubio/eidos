package io.casehub.eidos.memory;

import io.casehub.eidos.api.DetectedRoleSignal;
import io.casehub.eidos.api.DetectedRoleSignalStore;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryDetectedRoleSignalStore implements DetectedRoleSignalStore {

    private final ConcurrentHashMap<AgentKey, CopyOnWriteArrayList<DetectedRoleSignal>> store =
        new ConcurrentHashMap<>();

    @Override
    public void record(final DetectedRoleSignal signal) {
        store.computeIfAbsent(new AgentKey(signal.agentId(), signal.tenancyId()),
                              k -> new CopyOnWriteArrayList<>())
             .add(signal);
    }

    @Override
    public List<DetectedRoleSignal> signals(final String agentId, final String roleName,
                                             final String tenancyId, final Instant since) {
        final var list = store.get(new AgentKey(agentId, tenancyId));
        if (list == null) return List.of();
        return list.stream()
            .filter(s -> s.roleName().equals(roleName))
            .filter(s -> !s.observedAt().isBefore(since))
            .toList();
    }

    @Override
    public List<DetectedRoleSignal> signalsByAgent(final String agentId,
                                                    final String tenancyId, final Instant since) {
        final var list = store.get(new AgentKey(agentId, tenancyId));
        if (list == null) return List.of();
        return list.stream()
            .filter(s -> !s.observedAt().isBefore(since))
            .toList();
    }

    @Override
    public int purge(final String agentId, final String tenancyId, final Instant olderThan) {
        final var list = store.get(new AgentKey(agentId, tenancyId));
        if (list == null) return 0;
        final var old = list.stream().filter(s -> s.observedAt().isBefore(olderThan)).toList();
        list.removeAll(old);
        return old.size();
    }

    @Override
    public void clear(final String agentId, final String tenancyId) {
        store.remove(new AgentKey(agentId, tenancyId));
    }

    private record AgentKey(String agentId, String tenancyId) {}
}
