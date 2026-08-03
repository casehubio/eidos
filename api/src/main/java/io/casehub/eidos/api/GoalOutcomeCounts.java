package io.casehub.eidos.api;

public record GoalOutcomeCounts(int successCount, int failureCount) {
    public GoalOutcomeCounts {
        if (successCount < 0) throw new IllegalArgumentException("successCount must be >= 0");
        if (failureCount < 0) throw new IllegalArgumentException("failureCount must be >= 0");
    }

    public double successRate() {
        int total = successCount + failureCount;
        return total == 0 ? 0.0 : (double) successCount / total;
    }
}
