package io.casehub.eidos.api;

import java.util.List;
import java.util.Objects;

public record AgentGoal(
        String name,
        String description,
        GoalPriority priority,
        Visibility visibility,
        List<String> capabilities
) {
    public AgentGoal {
        AgentDescriptorValidator.validateRequired("goal.name", name,
                                                  AgentDescriptorValidator.MAX_GOAL_NAME);
        AgentDescriptorValidator.validateRequired("goal.description", description,
                                                  AgentDescriptorValidator.MAX_GOAL_DESCRIPTION);
        Objects.requireNonNull(priority, "goal.priority must not be null");
        Objects.requireNonNull(visibility, "goal.visibility must not be null");
        capabilities = capabilities != null
                       ? List.copyOf(capabilities.stream().filter(Objects::nonNull).toList())
                       : List.of();
        AgentDescriptorValidator.validateItems("goal.capabilities", capabilities,
                                               AgentDescriptorValidator.MAX_CAPABILITY_NAME);
        if (capabilities.size() != capabilities.stream().distinct().count()) {
            throw new AgentValidationException("goal.capabilities",
                                               "duplicate capability name in goal '" + name + "'");
        }
    }
}
