package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.GoalEvolution;
import io.casehub.eidos.api.GoalEvolutionResult;
import io.casehub.eidos.api.GoalOutcomeCounts;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@DefaultBean
@ApplicationScoped
public class NoOpGoalEvolution implements GoalEvolution {

    @Override
    public GoalEvolutionResult evaluate(final AgentDescriptor descriptor,
                                         final Map<String, GoalOutcomeCounts> counts) {
        return new GoalEvolutionResult.Unchanged();
    }
}
