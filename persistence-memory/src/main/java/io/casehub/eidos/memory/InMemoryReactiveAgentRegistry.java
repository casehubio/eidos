package io.casehub.eidos.memory;

import io.casehub.eidos.api.*;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryReactiveAgentRegistry implements ReactiveAgentRegistry {

    @Inject InMemoryAgentRegistry delegate;

    @Override
    public Uni<Void> register(AgentDescriptor descriptor) {
        return Uni.createFrom().item(descriptor)
            .invoke(delegate::register)
            .replaceWithVoid();
    }

    @Override
    public Uni<Optional<AgentDescriptor>> findById(String agentId, String tenancyId) {
        return Uni.createFrom().item(delegate.findById(agentId, tenancyId));
    }

    @Override
    public Uni<List<AgentMatch>> find(AgentQuery query) {
        return Uni.createFrom().item(delegate.find(query));
    }
}
