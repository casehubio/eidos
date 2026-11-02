package io.casehub.eidos.org.api;

import java.time.Instant;
import java.util.Objects;

public record MissionCommitment(
    String agentId,
    String missionName,
    Instant committedAt
) {
    public MissionCommitment {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(missionName, "missionName");
        Objects.requireNonNull(committedAt, "committedAt");
    }
}
