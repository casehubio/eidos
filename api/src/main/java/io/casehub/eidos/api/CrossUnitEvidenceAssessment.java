package io.casehub.eidos.api;

import java.util.List;

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
        if (sameUnitCount < 0 || crossUnitCount < 0 || unknownCount < 0) {
            throw new IllegalArgumentException("Counts must be non-negative");
        }
        warnings = List.copyOf(warnings);
    }
}
