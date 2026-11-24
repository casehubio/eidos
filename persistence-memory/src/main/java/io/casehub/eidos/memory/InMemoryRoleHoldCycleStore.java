package io.casehub.eidos.memory;

import io.casehub.eidos.api.RoleHoldCycleStore;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryRoleHoldCycleStore implements RoleHoldCycleStore {

    private final ConcurrentHashMap<String, AtomicReference<HoldCycleState>> store =
            new ConcurrentHashMap<>();

    @Override
    public HoldCycleState get(final String agentId, final String roleName,
                              final String tenancyId) {
        final var ref = store.get(key(agentId, roleName, tenancyId));
        return ref == null ? HoldCycleState.EMPTY : ref.get();
    }

    @Override
    public void increment(final String agentId, final String roleName,
                          final String tenancyId) {
        store.computeIfAbsent(key(agentId, roleName, tenancyId),
                k -> new AtomicReference<>(HoldCycleState.EMPTY))
            .updateAndGet(s -> new HoldCycleState(s.consecutiveCycles() + 1, Instant.now()));
    }

    @Override
    public void reset(final String agentId, final String roleName,
                      final String tenancyId) {
        store.computeIfAbsent(key(agentId, roleName, tenancyId),
                k -> new AtomicReference<>(HoldCycleState.EMPTY))
            .set(new HoldCycleState(0, Instant.now()));
    }

    private static String key(final String agentId, final String roleName,
                              final String tenancyId) {
        return agentId + "|" + roleName + "|" + tenancyId;
    }
}
