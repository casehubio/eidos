package io.casehub.eidos.api;

import java.time.Instant;
import java.util.Objects;

public record RoleEvidence(
    String sourceId,
    double qualitySum,
    double effectiveCount,
    double weight,
    Instant windowStart
) {
    public RoleEvidence {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(windowStart, "windowStart");
        if (weight <= 0.0 || Double.isNaN(weight) || Double.isInfinite(weight))
            throw new IllegalArgumentException("weight must be positive and finite");
        if (qualitySum < 0.0)
            throw new IllegalArgumentException("qualitySum must be >= 0");
        if (effectiveCount < 0.0)
            throw new IllegalArgumentException("effectiveCount must be >= 0");
    }
}
