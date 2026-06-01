package io.casehub.eidos.eval;

import java.util.List;

public record ProximityReport(
    List<ProximityResult> results,
    double floor,
    double meanScore,
    double minScore,
    int belowFloor
) {
    public static ProximityReport build(final List<ProximityResult> results, final double floor) {
        final double mean = results.stream().mapToInt(ProximityResult::score).average().orElse(0.0);
        final int min = results.stream().mapToInt(ProximityResult::score).min().orElse(0);
        final int below = (int) results.stream().filter(r -> r.score() < floor).count();
        return new ProximityReport(results, floor, mean, min, below);
    }
}
