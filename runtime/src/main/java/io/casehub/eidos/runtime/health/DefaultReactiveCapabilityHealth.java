package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@DefaultBean
@ApplicationScoped
public class DefaultReactiveCapabilityHealth implements ReactiveCapabilityHealth {

    @Inject
    CapabilityHealth delegate;

    @Override
    public Uni<CapabilityStatus> probe(AgentDescriptor descriptor, String capabilityTag,
                                       ProbeContext context) {
        return Uni.createFrom()
                  .item(() -> delegate.probe(descriptor, capabilityTag, context))
                  .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
