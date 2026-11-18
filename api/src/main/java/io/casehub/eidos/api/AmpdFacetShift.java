package io.casehub.eidos.api;

import java.util.Set;

public record AmpdFacetShift(String facet, int delta) {

    public static final Set<String> VALID_FACETS = AmpdFacets.CANONICAL;

    public AmpdFacetShift {
        facet = AmpdFacets.canonicalise(facet);
        if (delta < -50 || delta > 50) {
            throw new IllegalArgumentException("delta must be in [-50, 50], got " + delta);
        }
    }
}
