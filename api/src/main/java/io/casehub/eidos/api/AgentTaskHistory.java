package io.casehub.eidos.api;

import java.util.List;

public record AgentTaskHistory(
    String agentId,
    String tenancyId,
    List<AgentTask> tasks,           // includes in-progress (endedAt=null)
    List<AgentOutcome> outcomes,
    List<AttestationRef> attestationRefs,
    GraphDataSufficiency sufficiency
) {}
