package io.casehub.eidos.api;

public record AmpdFacet(String facet, int score) {
    public AmpdFacet {
        facet = AmpdFacets.canonicalise(facet);
        if (score < 0 || score > 100) {throw new IllegalArgumentException("AMPD score must be 0-100: " + score);}
    }
}
