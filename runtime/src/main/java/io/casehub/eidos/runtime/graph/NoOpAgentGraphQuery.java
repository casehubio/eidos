package io.casehub.eidos.runtime.graph;

import io.casehub.eidos.api.*;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@DefaultBean
@ApplicationScoped
public class NoOpAgentGraphQuery implements AgentGraphQuery {

    @Override
    public AgentTaskHistory agentHistory(final String agentId, final String tenancyId) {
        return new AgentTaskHistory(agentId, tenancyId, List.of(), List.of(), List.of(),
            GraphDataSufficiency.empty(List.of()));
    }

    @Override
    public AgentTaskHistory historyByCapability(final String agentId, final String capabilityTag,
                                                 final String tenancyId) {
        return agentHistory(agentId, tenancyId);
    }

    @Override
    public List<String> topAgentsByOutcome(final String capabilityTag, final String taskDomain,
                                            final String tenancyId, final int limit) {
        return List.of();
    }

    @Override
    public List<AttestationRef> attestationsFor(final String agentId, final String tenancyId) {
        return List.of();
    }
}
