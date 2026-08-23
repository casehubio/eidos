package io.casehub.eidos.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;


public record AgentGoal(
        String name,
        String description,
        GoalPriority priority,
        Visibility visibility,
        List<String> capabilities,
        Map<String, String> attributes) {
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
        attributes = attributes != null ? Map.copyOf(attributes) : null;
    }

    public Builder toBuilder() {
        return new Builder(name, description, priority, visibility, capabilities, attributes);
    }

    public static final class Builder {
        private String name, description;
        private GoalPriority        priority;
        private Visibility          visibility;
        private List<String>        capabilities;
        private Map<String, String> attributes;

        Builder(String name, String description, GoalPriority priority,
                Visibility visibility, List<String> capabilities,
                Map<String, String> attributes) {
            this.name         = name;
            this.description  = description;
            this.priority     = priority;
            this.visibility   = visibility;
            this.capabilities = capabilities != null ? new java.util.ArrayList<>(capabilities) : new java.util.ArrayList<>();
            this.attributes   = attributes;
        }

        public Builder name(String v)                    {
                                                             this.name = v;
                                                             return this;
                                                         }

        public Builder description(String v)             {
                                                             this.description = v;
                                                             return this;
                                                         }

        public Builder priority(GoalPriority v)          {
                                                             this.priority = v;
                                                             return this;
                                                         }

        public Builder visibility(Visibility v)          {
                                                             this.visibility = v;
                                                             return this;
                                                         }

        public Builder capabilities(List<String> v)      {
                                                             this.capabilities = v;
                                                             return this;
                                                         }

        public Builder attributes(Map<String, String> v) {
                                                             this.attributes = v;
                                                             return this;
                                                         }

        public AgentGoal build() {
            return new AgentGoal(name, description, priority, visibility, capabilities, attributes);
        }
    }
}
