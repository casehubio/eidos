package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.GoalOutcome;
import io.casehub.eidos.api.GoalOutcomeCounts;
import io.casehub.eidos.api.GoalSignalStore;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@DefaultBean
@ApplicationScoped
public class NoOpGoalSignalStore implements GoalSignalStore {

    @Override
    public void recordOutcome(final String agentId, final String tenancyId,
                               final String goalName, final GoalOutcome outcome) {}

    @Override
    public Map<String, GoalOutcomeCounts> outcomeCounts(final String agentId,
                                                        final String tenancyId) {
        return Map.of();
    }

    @Override
    public void decay(final String agentId, final String tenancyId,
                      final double decayFactor) {}

    @Override
    public void clear(final String agentId, final String tenancyId) {}
}
