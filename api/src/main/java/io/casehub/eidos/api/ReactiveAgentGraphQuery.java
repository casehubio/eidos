package io.casehub.eidos.api;

import io.smallrye.mutiny.Uni;
import java.util.List;

public interface ReactiveAgentGraphQuery {
    Uni<AgentTaskHistory> agentHistory(String agentId, String tenancyId);
    Uni<List<String>> topAgentsByOutcome(String capabilityTag, String taskDomain,
                                          String tenancyId, int limit);
    Uni<AgentTaskHistory> historyByCapability(String agentId, String capabilityTag,
                                               String tenancyId);
    Uni<List<AttestationRef>> attestationsFor(String agentId, String tenancyId);
}
