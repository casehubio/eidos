package io.casehub.eidos.api;

import java.util.List;
import java.util.Objects;

public record CrossUnitEvidenceAssessment(
        int sameUnitCount,
        int crossUnitCount,
        int unknownCount,
        EvidenceDiversityLevel level,
        List<String> warnings
) {
    public static final CrossUnitEvidenceAssessment UNKNOWN =
            new CrossUnitEvidenceAssessment(0, 0, 0, EvidenceDiversityLevel.UNKNOWN, List.of());

    public CrossUnitEvidenceAssessment {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        if (sameUnitCount < 0 || crossUnitCount < 0 || unknownCount < 0) {
            throw new IllegalArgumentException("Counts must be non-negative");
        }
        warnings = List.copyOf(warnings);
    }
}
