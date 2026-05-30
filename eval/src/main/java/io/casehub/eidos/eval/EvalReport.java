package io.casehub.eidos.eval;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record EvalReport(
        Instant timestamp,
        String judgeModel,
        List<EvalResult> results,
        EvalSummary summary
) {
    public static EvalReport build(final List<EvalResult> results, final String judgeModel) {
        final boolean allComplete = results.stream().allMatch(EvalResult::completenessPass);

        final Map<EvalDimension, Double> meanByDim = new EnumMap<>(EvalDimension.class);
        for (final EvalDimension d : EvalDimension.values()) {
            final double mean = results.stream()
                .mapToInt(r -> r.scores().get(d).score())
                .average()
                .orElse(0.0);
            meanByDim.put(d, mean);
        }

        final EvalDimension lowest = meanByDim.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(EvalDimension.values()[0]);

        final double meanOverall = results.stream()
            .mapToDouble(EvalResult::overall)
            .average()
            .orElse(0.0);

        return new EvalReport(Instant.now(), judgeModel,
            results, new EvalSummary(allComplete, meanByDim, lowest, meanOverall));
    }
}
