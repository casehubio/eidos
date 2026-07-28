package io.casehub.eidos.api;

public record DispositionValue(String term, double weight) {
    public DispositionValue {
        AgentDescriptorValidator.validateRequired("term", term, AgentDescriptorValidator.MAX_DISPOSITION_AXIS);
        if (Double.isNaN(weight) || weight < 0.0 || weight > 1.0)
            throw new IllegalArgumentException("weight must be 0.0–1.0, got " + weight);
    }

    public static DispositionValue of(String term) {
        return new DispositionValue(term, 1.0);
    }
}
