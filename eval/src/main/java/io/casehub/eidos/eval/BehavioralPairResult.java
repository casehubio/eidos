package io.casehub.eidos.eval;

public record BehavioralPairResult(
    VariantPair pair,
    String question,
    String higherResponse,
    String lowerResponse,
    boolean correct,
    int effectSize,
    String reasoning
) {
    public BehavioralPairResult {
        if (effectSize < 1 || effectSize > 5)
            throw new IllegalArgumentException("effectSize out of range: " + effectSize);
    }
}
