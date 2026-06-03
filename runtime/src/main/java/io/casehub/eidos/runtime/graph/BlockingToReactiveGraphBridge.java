package io.casehub.eidos.runtime.graph;

import io.casehub.eidos.api.*;
import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@DefaultBean
@ApplicationScoped
public class BlockingToReactiveGraphBridge implements ReactiveAgentGraphQuery {

    @Inject AgentGraphQuery blocking;

    @Override
    public Uni<AgentTaskHistory> agentHistory(final String agentId, final String tenancyId) {
        return Uni.createFrom()
                  .item(() -> blocking.agentHistory(agentId, tenancyId))
                  .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Override
    public Uni<List<String>> topAgentsByOutcome(final String capabilityTag, final String taskDomain,
                                                 final String tenancyId, final int limit) {
        return Uni.createFrom()
                  .item(() -> blocking.topAgentsByOutcome(capabilityTag, taskDomain, tenancyId, limit))
                  .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
