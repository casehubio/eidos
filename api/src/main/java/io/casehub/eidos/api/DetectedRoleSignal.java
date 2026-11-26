package io.casehub.eidos.api;

import java.time.Instant;
import java.util.Objects;

public record DetectedRoleSignal(
    String agentId,
    String tenancyId,
    String roleName,
    String patternType,
    String patternVocabulary,
    double confidence,
    Instant observedAt
) {
    public DetectedRoleSignal {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(tenancyId, "tenancyId");
        Objects.requireNonNull(roleName, "roleName");
        Objects.requireNonNull(patternType, "patternType");
        Objects.requireNonNull(observedAt, "observedAt");
        if (agentId.isBlank()) throw new IllegalArgumentException("agentId must not be blank");
        if (tenancyId.isBlank()) throw new IllegalArgumentException("tenancyId must not be blank");
        if (roleName.isBlank()) throw new IllegalArgumentException("roleName must not be blank");
        if (patternType.isBlank()) throw new IllegalArgumentException("patternType must not be blank");
        if (confidence < 0.0 || confidence > 1.0 || Double.isNaN(confidence))
            throw new IllegalArgumentException("confidence must be in [0.0, 1.0], got: " + confidence);
    }
}
