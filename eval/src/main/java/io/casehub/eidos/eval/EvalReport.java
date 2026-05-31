package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public record EvalReport(
        Instant timestamp,
        String judgeModel,
        Map<RenderFormat, List<EvalResult>> resultsByFormat,
        Map<RenderFormat, EvalSummary> summaryByFormat
) {
    public static EvalReport build(final List<EvalResult> results, final String judgeModel) {
        final Map<RenderFormat, List<EvalResult>> byFormat = results.stream()
            .collect(Collectors.groupingBy(
                r -> r.evalCase().context().format(),
                java.util.LinkedHashMap::new,
                Collectors.toList()));

        final Map<RenderFormat, EvalSummary> summaries = byFormat.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> buildSummary(e.getKey(), e.getValue()),
                (a, b) -> a,
                java.util.LinkedHashMap::new));

        return new EvalReport(Instant.now(), judgeModel, byFormat, summaries);
    }

    private static EvalSummary buildSummary(final RenderFormat format,
                                             final List<EvalResult> results) {
        final Set<EvalDimension> applicable = EvalDimension.applicableFor(format);

        final boolean allComplete = results.stream().allMatch(EvalResult::completenessPass);

        final Map<EvalDimension, Double> meanByDim = new EnumMap<>(EvalDimension.class);
        for (final EvalDimension d : applicable) {
            final double mean = results.stream()
                .filter(r -> r.scores().containsKey(d))
                .mapToInt(r -> r.scores().get(d).score())
                .average()
                .orElse(0.0);
            meanByDim.put(d, mean);
        }

        final EvalDimension lowest = meanByDim.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(applicable.iterator().next());

        final double meanOverall = results.stream()
            .mapToDouble(EvalResult::overall)
            .average()
            .orElse(0.0);

        return new EvalSummary(allComplete, meanByDim, lowest, meanOverall);
    }
}
