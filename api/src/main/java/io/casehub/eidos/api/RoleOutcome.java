package io.casehub.eidos.api;

import java.time.Instant;
import java.util.Objects;

public record RoleOutcome(
    String taskId,
    double roleQuality,
    String dimension,
    Instant observedAt
) {
    public RoleOutcome {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(observedAt, "observedAt");
        if (roleQuality < 0.0 || roleQuality > 1.0)
            throw new IllegalArgumentException("roleQuality must be in [0.0, 1.0]");
    }
}
