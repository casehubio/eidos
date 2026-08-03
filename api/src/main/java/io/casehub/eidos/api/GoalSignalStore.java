package io.casehub.eidos.api;

import java.util.Map;

public interface GoalSignalStore {
    void recordOutcome(String agentId, String tenancyId, String goalName, GoalOutcome outcome);

    Map<String, GoalOutcomeCounts> outcomeCounts(String agentId, String tenancyId);

    void decay(String agentId, String tenancyId, double decayFactor);

    void clear(String agentId, String tenancyId);
}
