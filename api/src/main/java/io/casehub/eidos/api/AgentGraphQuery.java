package io.casehub.eidos.api;

import java.util.List;

public interface AgentGraphQuery {
    /** Returns all tasks including in-progress (endedAt=null). */
    AgentTaskHistory agentHistory(String agentId, String tenancyId);

    AgentTaskHistory historyByCapability(String agentId, String capabilityTag, String tenancyId);

    /**
     * Returns agentIds ranked by Wilson lower bound score.
     * quality = confidence × multiplier (SUCCEEDED=1.0, PARTIALLY=0.5, FAILED=0.0)
     * Wilson z=1.645. Score=0 when no observations. Agents with score=0 appear last.
     */
    List<String> topAgentsByOutcome(String capabilityTag, String taskDomain,
                                    String tenancyId, int limit);

    List<AttestationRef> attestationsFor(String agentId, String tenancyId);
}
