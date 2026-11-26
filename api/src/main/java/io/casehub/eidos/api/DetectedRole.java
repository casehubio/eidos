package io.casehub.eidos.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record DetectedRole(
    String roleName,
    double aggregateConfidence,
    int signalCount,
    double effectiveCount,
    Instant firstObserved,
    Instant lastObserved,
    List<String> patternTypes
) {
    public DetectedRole {
        Objects.requireNonNull(roleName, "roleName");
        Objects.requireNonNull(firstObserved, "firstObserved");
        Objects.requireNonNull(lastObserved, "lastObserved");
        Objects.requireNonNull(patternTypes, "patternTypes");
        patternTypes = List.copyOf(patternTypes);
        if (aggregateConfidence < 0.0 || Double.isNaN(aggregateConfidence) || Double.isInfinite(aggregateConfidence))
            throw new IllegalArgumentException("aggregateConfidence must be non-negative and finite");
        if (signalCount < 0) throw new IllegalArgumentException("signalCount must be non-negative");
        if (effectiveCount < 0.0 || Double.isNaN(effectiveCount) || Double.isInfinite(effectiveCount))
            throw new IllegalArgumentException("effectiveCount must be non-negative and finite");
    }
}
