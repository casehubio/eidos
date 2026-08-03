package io.casehub.eidos.api;

import java.util.Map;

public interface GoalEvolution {
    GoalEvolutionResult evaluate(AgentDescriptor descriptor, Map<String, GoalOutcomeCounts> counts);
}
