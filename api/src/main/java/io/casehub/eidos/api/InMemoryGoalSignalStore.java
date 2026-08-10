package io.casehub.eidos.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryGoalSignalStore implements GoalSignalStore {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, int[]>> store =
        new ConcurrentHashMap<>();

    @Override
    public void recordOutcome(String agentId, String tenancyId, String goalName,
                              GoalOutcome outcome) {
        String key = agentId + "|" + tenancyId;
        store.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
             .compute(goalName, (g, counts) -> {
                 if (counts == null) counts = new int[]{0, 0};
                 if (outcome == GoalOutcome.SUCCESS) counts[0]++;
                 else counts[1]++;
                 return counts;
             });
    }

    @Override
    public Map<String, GoalOutcomeCounts> outcomeCounts(String agentId, String tenancyId) {
        String key = agentId + "|" + tenancyId;
        ConcurrentHashMap<String, int[]> goalCounts = store.get(key);
        if (goalCounts == null) return Map.of();
        return goalCounts.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                e -> new GoalOutcomeCounts(e.getValue()[0], e.getValue()[1])));
    }

    @Override
    public void decay(String agentId, String tenancyId, double decayFactor) {
        String key = agentId + "|" + tenancyId;
        ConcurrentHashMap<String, int[]> goalCounts = store.get(key);
        if (goalCounts == null) return;
        goalCounts.replaceAll((goal, counts) ->
            new int[]{(int) (counts[0] * decayFactor), (int) (counts[1] * decayFactor)});
        goalCounts.entrySet().removeIf(e -> e.getValue()[0] == 0 && e.getValue()[1] == 0);
    }

    @Override
    public void clear(String agentId, String tenancyId) {
        store.remove(agentId + "|" + tenancyId);
    }
}
