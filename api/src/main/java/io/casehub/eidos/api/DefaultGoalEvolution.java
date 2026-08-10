package io.casehub.eidos.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultGoalEvolution implements GoalEvolution {

    static final int MIN_OUTCOMES = 5;
    static final double PROMOTION_THRESHOLD = 0.8;
    static final double DEMOTION_THRESHOLD = 0.7;
    static final double DAMPENED_DECAY = 0.5;

    @Override
    public GoalEvolutionResult evaluate(AgentDescriptor descriptor,
                                         Map<String, GoalOutcomeCounts> counts) {
        if (descriptor.goals().isEmpty()) return new GoalEvolutionResult.Unchanged();

        boolean anyBelowMin = false;
        boolean anyChanged = false;
        List<AgentGoal> newGoals = new ArrayList<>();
        List<String> promoted = new ArrayList<>();
        List<String> demoted = new ArrayList<>();

        for (AgentGoal goal : descriptor.goals()) {
            GoalOutcomeCounts gc = counts.get(goal.name());
            if (gc == null || gc.successCount() + gc.failureCount() == 0) {
                newGoals.add(goal);
                continue;
            }
            int total = gc.successCount() + gc.failureCount();
            if (total < MIN_OUTCOMES) {
                anyBelowMin = true;
                newGoals.add(goal);
                continue;
            }
            double successRate = gc.successRate();
            double failureRate = 1.0 - successRate;

            if (goal.priority() == GoalPriority.SECONDARY && successRate > PROMOTION_THRESHOLD) {
                newGoals.add(new AgentGoal(goal.name(), goal.description(), GoalPriority.PRIMARY,
                                           goal.visibility(), goal.capabilities()));
                promoted.add(goal.name());
                anyChanged = true;
            } else if (goal.priority() == GoalPriority.PRIMARY && failureRate > DEMOTION_THRESHOLD) {
                newGoals.add(new AgentGoal(goal.name(), goal.description(), GoalPriority.SECONDARY,
                                           goal.visibility(), goal.capabilities()));
                demoted.add(goal.name());
                anyChanged = true;
            } else {
                newGoals.add(goal);
            }
        }

        if (anyChanged) {
            return new GoalEvolutionResult.Evolved(newGoals, promoted, demoted);
        }
        if (anyBelowMin && !counts.isEmpty()) {
            return new GoalEvolutionResult.Dampened(DAMPENED_DECAY);
        }
        return new GoalEvolutionResult.Unchanged();
    }
}
