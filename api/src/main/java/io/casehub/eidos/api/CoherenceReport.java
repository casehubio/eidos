package io.casehub.eidos.api;

import java.util.List;

public record CoherenceReport(
    CoherenceLevel overall,
    List<CoherenceViolation> violations
) {
    public static final CoherenceReport ALIGNED =
        new CoherenceReport(CoherenceLevel.ALIGNED, List.of());

    public CoherenceReport {
        if (overall == null) throw new IllegalArgumentException("overall required");
        violations = violations == null ? List.of() : List.copyOf(violations);
    }
}
