package io.casehub.eidos.core.graph;

import io.casehub.eidos.api.AgentGraphQuery;
import io.casehub.eidos.api.AgentTaskHistory;
import io.casehub.eidos.api.AttestationRef;
import io.casehub.eidos.api.GraphDataSufficiency;

import java.util.List;


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

    @Override
    public List<String> coActiveAgents(final String externalRef, final String tenancyId) {
        return List.of();
    }
}
