package io.casehub.eidos.api;

import io.smallrye.mutiny.Uni;

public interface ReactiveCapabilityHealth {
    Uni<CapabilityHealth.CapabilityStatus> probe(
            AgentDescriptor descriptor, String capabilityTag,
            CapabilityHealth.ProbeContext context);
}
