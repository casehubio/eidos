package io.casehub.eidos.api;

public record CoherenceViolation(
    CoherenceLevel level,
    String description,
    String briefingExcerpt,
    DispositionAxis axis,
    String declaredValue,
    String impliedValue
) {
    public CoherenceViolation {
        if (level == null) throw new IllegalArgumentException("level required");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("description required");
    }
}
