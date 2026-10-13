package io.casehub.eidos.api;

import java.util.Objects;

public record AgentDescriptorRegistered(AgentDescriptor descriptor) {
    public AgentDescriptorRegistered {
        Objects.requireNonNull(descriptor, "descriptor");
    }
}
