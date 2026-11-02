package io.casehub.eidos.org.api;

import java.time.Instant;
import java.util.Objects;

public record RoleAdoption(
    String agentId,
    String roleName,
    String roleVocabulary,
    Instant adoptedAt
) {
    public RoleAdoption {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(roleName, "roleName");
        Objects.requireNonNull(adoptedAt, "adoptedAt");
    }
}
