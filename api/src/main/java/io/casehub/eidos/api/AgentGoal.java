package io.casehub.eidos.api;

import java.util.Objects;

public record AgentGoal(
        String name,
        String description,
        GoalPriority priority,
        Visibility visibility
) {
    public AgentGoal {
        AgentDescriptorValidator.validateRequired("goal.name", name,
            AgentDescriptorValidator.MAX_GOAL_NAME);
        AgentDescriptorValidator.validateRequired("goal.description", description,
            AgentDescriptorValidator.MAX_GOAL_DESCRIPTION);
        Objects.requireNonNull(priority, "goal.priority must not be null");
        Objects.requireNonNull(visibility, "goal.visibility must not be null");
    }
}
