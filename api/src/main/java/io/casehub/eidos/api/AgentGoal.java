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

    public Builder toBuilder() {
        return new Builder(name, description, priority, visibility, capabilities);
    }

    public static final class Builder {
        private String name, description;
        private GoalPriority priority;
        private Visibility   visibility;
        private List<String> capabilities;

        Builder(String name, String description, GoalPriority priority,
                Visibility visibility, List<String> capabilities) {
            this.name         = name;
            this.description  = description;
            this.priority     = priority;
            this.visibility   = visibility;
            this.capabilities = capabilities != null ? new java.util.ArrayList<>(capabilities) : new java.util.ArrayList<>();
        }

        public Builder name(String v)               {
                                                        this.name = v;
                                                        return this;
                                                    }

        public Builder description(String v)        {
                                                        this.description = v;
                                                        return this;
                                                    }

        public Builder priority(GoalPriority v)     {
                                                        this.priority = v;
                                                        return this;
                                                    }

        public Builder visibility(Visibility v)     {
                                                        this.visibility = v;
                                                        return this;
                                                    }

        public Builder capabilities(List<String> v) {
                                                        this.capabilities = v;
                                                        return this;
                                                    }

        public AgentGoal build() {
            return new AgentGoal(name, description, priority, visibility, capabilities);
        }
    }
}
