package io.casehub.eidos.api;

import java.util.Objects;

public record AgentDescriptorUpdated(AgentDescriptor previous, AgentDescriptor current) {
    public AgentDescriptorUpdated {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
    }
}
