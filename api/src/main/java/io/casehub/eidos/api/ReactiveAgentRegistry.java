package io.casehub.eidos.api;

import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;

public interface ReactiveAgentRegistry {
    Uni<Void> register(AgentDescriptor descriptor);

    /**
     * @throws NullPointerException if agentId or tenancyId is null
     */
    Uni<Optional<AgentDescriptor>> findById(String agentId, String tenancyId);

    Uni<List<AgentDescriptor>> find(AgentQuery query);
}
