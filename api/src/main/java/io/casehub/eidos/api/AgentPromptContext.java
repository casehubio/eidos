package io.casehub.eidos.api;

import java.util.List;
import java.util.Optional;

public record AgentPromptContext(
        Optional<GoalContext> goal,
        List<Resource> resources,
        String situationalContext,
        SystemPromptRenderer.RenderFormat format
) {
    public static AgentPromptContext forFormat(final SystemPromptRenderer.RenderFormat format) {
        return new AgentPromptContext(Optional.empty(), List.of(), null, format);
    }

    public AgentPromptContext withGoal(final GoalContext goal) {
        return new AgentPromptContext(Optional.of(goal), resources, situationalContext, format);
    }

    public AgentPromptContext withResources(final List<Resource> resources) {
        return new AgentPromptContext(goal, resources, situationalContext, format);
    }

    public AgentPromptContext withSituationalContext(final String situationalContext) {
        return new AgentPromptContext(goal, resources, situationalContext, format);
    }
}
