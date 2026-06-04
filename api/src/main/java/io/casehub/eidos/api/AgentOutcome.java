package io.casehub.eidos.api;

import java.time.Instant;
import java.util.Objects;

public record AgentOutcome(
    String taskId,
    TaskResult result,
    double confidence,           // 0.0–1.0, enforced in compact constructor
    Instant observedAt,          // when the outcome occurred, not when it was persisted
    DegradationReason degradationReason  // nullable — null if no degradation
) {
    public AgentOutcome {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(result, "result");
        if (Double.isNaN(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new AgentValidationException("confidence", "must be between 0.0 and 1.0");
        }
        Objects.requireNonNull(observedAt, "observedAt");
    }
}
