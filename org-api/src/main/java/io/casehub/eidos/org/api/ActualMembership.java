package io.casehub.eidos.org.api;

import java.time.Instant;
import java.util.Objects;

public record ActualMembership(
    String agentId,
    Instant joinedAt
) {
    public ActualMembership {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(joinedAt, "joinedAt");
    }
}
