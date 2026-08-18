package io.casehub.eidos.api;

public record ValenceCounts(int positive, int negative) {
    public int effective(double dampeningFactor) {
        return positive + (int) Math.round(negative * dampeningFactor);
    }
}
