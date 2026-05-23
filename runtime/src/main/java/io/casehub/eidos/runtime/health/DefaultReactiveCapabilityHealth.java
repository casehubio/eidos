package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "true")
@ApplicationScoped
public class DefaultReactiveCapabilityHealth implements ReactiveCapabilityHealth {

    @Inject
    DefaultCapabilityHealth delegate;

    @Override
    public Uni<CapabilityStatus> probe(AgentDescriptor descriptor, String capabilityTag,
                                       ProbeContext context) {
        return Uni.createFrom().item(() -> delegate.probe(descriptor, capabilityTag, context));
    }
}
