package io.casehub.eidos.memory;

import io.casehub.eidos.api.GoalOutcome;
import io.casehub.eidos.api.GoalOutcomeCounts;
import io.casehub.eidos.api.GoalSignalStore;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryGoalSignalStore implements GoalSignalStore {

    private final ConcurrentHashMap<String, GoalOutcomeCounts> store = new ConcurrentHashMap<>();

    private String key(final String agentId, final String tenancyId, final String goalName) {
        return agentId + ":" + tenancyId + ":" + goalName;
    }

    @Override
    public void recordOutcome(final String agentId, final String tenancyId,
                               final String goalName, final GoalOutcome outcome) {
        store.merge(key(agentId, tenancyId, goalName),
            outcome == GoalOutcome.SUCCESS
                ? new GoalOutcomeCounts(1, 0)
                : new GoalOutcomeCounts(0, 1),
            (old, inc) -> new GoalOutcomeCounts(
                old.successCount() + inc.successCount(),
                old.failureCount() + inc.failureCount()));
    }

    @Override
    public Map<String, GoalOutcomeCounts> outcomeCounts(final String agentId,
                                                        final String tenancyId) {
        final var prefix = agentId + ":" + tenancyId + ":";
        final var result = new HashMap<String, GoalOutcomeCounts>();
        store.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                result.put(k.substring(prefix.length()), v);
            }
        });
        return result;
    }

    @Override
    public void decay(final String agentId, final String tenancyId, final double decayFactor) {
        final var prefix = agentId + ":" + tenancyId + ":";
        store.replaceAll((k, v) -> {
            if (!k.startsWith(prefix)) return v;
            return new GoalOutcomeCounts(
                (int) (v.successCount() * (1 - decayFactor)),
                (int) (v.failureCount() * (1 - decayFactor)));
        });
    }

    @Override
    public void clear(final String agentId, final String tenancyId) {
        final var prefix = agentId + ":" + tenancyId + ":";
        store.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
