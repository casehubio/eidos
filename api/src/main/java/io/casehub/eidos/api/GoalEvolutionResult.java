package io.casehub.eidos.api;

import java.util.List;

public sealed interface GoalEvolutionResult
    permits GoalEvolutionResult.Unchanged,
            GoalEvolutionResult.Evolved,
            GoalEvolutionResult.Dampened {

    record Unchanged() implements GoalEvolutionResult {}

    record Evolved(
        List<AgentGoal> newGoals,
        List<String> promotedGoals,
        List<String> demotedGoals
    ) implements GoalEvolutionResult {
        public Evolved {
            newGoals = List.copyOf(newGoals);
            promotedGoals = promotedGoals == null ? List.of() : List.copyOf(promotedGoals);
            demotedGoals = demotedGoals == null ? List.of() : List.copyOf(demotedGoals);
        }
    }

    record Dampened(double decayFactor) implements GoalEvolutionResult {}
}
