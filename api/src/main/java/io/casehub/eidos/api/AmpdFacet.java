package io.casehub.eidos.api;

public record AmpdFacet(String facet, int score) {
    public AmpdFacet {
        if (score < 0 || score > 100) throw new IllegalArgumentException("AMPD score must be 0-100: " + score);
    }
}
