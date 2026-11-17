package io.casehub.eidos.api;

import java.util.List;
import java.util.Set;

public record SituationalProfile(String mode, List<AmpdFacetShift> shifts, String provenance) {

    public static final Set<String> VALID_MODES = Set.of(
            "stress", "flow", "social_collaborative", "social_competitive", "social_hierarchical");

    public SituationalProfile {
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("mode must not be blank");
        }
        if (!VALID_MODES.contains(mode)) {
            throw new IllegalArgumentException("Unknown situational mode: " + mode
                    + ". Valid: " + VALID_MODES);
        }
        shifts = shifts != null ? List.copyOf(shifts) : List.of();
    }
}
