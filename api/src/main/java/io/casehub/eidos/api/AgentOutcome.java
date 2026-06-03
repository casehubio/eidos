package io.casehub.eidos.api;

public record AgentOutcome(
    String taskId,
    TaskResult result,
    double confidence,                   // 0.0–1.0
    DegradationReason degradationReason  // null if not applicable
    // Note: no tenancyId — derive via join to AgentTask
) {}
