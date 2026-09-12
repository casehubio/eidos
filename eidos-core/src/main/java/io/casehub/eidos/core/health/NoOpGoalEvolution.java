package io.casehub.eidos.core.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.GoalEvolution;
import io.casehub.eidos.api.GoalEvolutionResult;
import io.casehub.eidos.api.GoalOutcomeCounts;


import java.util.Map;


public class NoOpGoalEvolution implements GoalEvolution {

    @Override
    public GoalEvolutionResult evaluate(final AgentDescriptor descriptor,
                                         final Map<String, GoalOutcomeCounts> counts) {
        return new GoalEvolutionResult.Unchanged();
    }
}
