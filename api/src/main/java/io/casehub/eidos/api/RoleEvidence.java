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
        if (qualitySum < 0.0 || Double.isNaN(qualitySum) || Double.isInfinite(qualitySum))
            throw new IllegalArgumentException("qualitySum must be non-negative and finite");
        if (effectiveCount < 0.0 || Double.isNaN(effectiveCount) || Double.isInfinite(effectiveCount))
            throw new IllegalArgumentException("effectiveCount must be non-negative and finite");
    }
}
