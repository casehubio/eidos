package io.casehub.eidos.api;

import java.util.Set;

public record AmpdFacetShift(String facet, int delta) {

    public static final Set<String> VALID_FACETS = Set.of(
            "Anxiousness", "Callousness", "Deceitfulness", "Emotional Lability",
            "Grandiosity", "Hostility", "Impulsivity", "Intimacy Avoidance",
            "Manipulativeness", "Risk Taking", "Suspiciousness", "Withdrawal",
            "Attention Seeking");

    public AmpdFacetShift {
        if (facet == null || facet.isBlank()) {
            throw new IllegalArgumentException("facet must not be blank");
        }
        if (!VALID_FACETS.contains(facet)) {
            throw new IllegalArgumentException("Unknown AMPD facet: " + facet
                    + ". Valid: " + VALID_FACETS);
        }
        if (delta < -50 || delta > 50) {
            throw new IllegalArgumentException("delta must be in [-50, 50], got " + delta);
        }
    }
}
