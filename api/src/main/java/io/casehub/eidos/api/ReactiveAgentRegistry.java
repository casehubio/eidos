package io.casehub.eidos.api;

import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;

public interface ReactiveAgentRegistry {
    Uni<Void> register(AgentDescriptor descriptor);
    Uni<Optional<AgentDescriptor>> findById(String agentId);
    Uni<List<AgentDescriptor>> find(AgentQuery query);
}
