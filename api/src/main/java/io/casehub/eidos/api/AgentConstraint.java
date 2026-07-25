package io.casehub.eidos.api;

import java.util.Objects;

public record AgentConstraint(
        String name,
        String description,
        Visibility visibility
) {
    public AgentConstraint {
        AgentDescriptorValidator.validateRequired("constraint.name", name,
            AgentDescriptorValidator.MAX_CONSTRAINT_NAME);
        AgentDescriptorValidator.validateRequired("constraint.description", description,
            AgentDescriptorValidator.MAX_CONSTRAINT_DESCRIPTION);
        Objects.requireNonNull(visibility, "constraint.visibility must not be null");
    }
}
