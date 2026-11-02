package io.casehub.eidos.org.api;

import java.util.Objects;

public record MissionSpec(
    String missionName,
    String description
) {
    public MissionSpec {
        Objects.requireNonNull(missionName, "missionName");
        if (missionName.isBlank()) throw new OrgValidationException("missionName", "must not be blank");
    }
}
