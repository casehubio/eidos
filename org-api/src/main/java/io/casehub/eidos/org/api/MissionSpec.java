package io.casehub.eidos.org.api;

import java.util.Objects;
import java.util.Set;

public record MissionSpec(
    String missionName,
    String description,
    Set<String> requiredRoles,
    int minCommitters,
    int maxCommitters
) {
    public MissionSpec {
        Objects.requireNonNull(missionName, "missionName");
        if (missionName.isBlank()) throw new OrgValidationException("missionName", "must not be blank");
        requiredRoles = requiredRoles != null ? Set.copyOf(requiredRoles) : Set.of();
        if (minCommitters < 0) throw new OrgValidationException("minCommitters", "must be >= 0 (was " + minCommitters + ")");
        if (maxCommitters > 0 && maxCommitters < minCommitters)
            throw new OrgValidationException("maxCommitters", "must be >= minCommitters or <= 0 (was " + maxCommitters + ", min " + minCommitters + ")");
    }

    public MissionSpec(String missionName, String description) {
        this(missionName, description, null, 0, 0);
    }
}
