package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EvalReportWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static String summaryTable(final EvalReport report) {
        final var sb = new StringBuilder();

        report.summaryByFormat().forEach((format, summary) -> {
            final int count = report.resultsByFormat().getOrDefault(format, List.of()).size();
            sb.append(String.format("=== %s (%d case%s) ===%n",
                format.name(), count, count == 1 ? "" : "s"));
            sb.append(String.format("%-30s %5s%n", "Dimension", "Score"));
            sb.append("-".repeat(37)).append("\n");

            summary.meanByDimension().entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry<EvalDimension, Double>::getValue)
                                   .thenComparingInt(e -> e.getKey().ordinal()))
                .forEach(e -> sb.append(String.format("%-30s %5.2f%n",
                    e.getKey().name().toLowerCase().replace('_', ' '), e.getValue())));

            sb.append("\n");
            sb.append(String.format("Overall mean:          %5.2f / 5.0%n", summary.meanOverall()));
            sb.append(String.format("All cases complete:    %s%n",
                summary.allCasesComplete() ? "YES" : "NO"));
            sb.append(String.format("Lowest dimension:      %s%n",
                summary.lowestScoringDimension().name().toLowerCase().replace('_', ' ')));
            sb.append("\n");
        });

        return sb.toString();
    }

    public static void writeJson(final EvalReport report, final Path path) {
        try {
            MAPPER.writeValue(path.toFile(), report);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to write eval report to " + path, e);
        }
    }

    public static void writeProximityJson(final ProximityReport report, final Path path) {
        try {
            MAPPER.writeValue(path.toFile(), report);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to write proximity report to " + path, e);
        }
    }

    public static String proximitySummaryTable(final ProximityReport report) {
        final var sb = new StringBuilder();
        sb.append(String.format("=== Proximity Report (floor %.1f) ===%n", report.floor()));
        sb.append(String.format("Mean score (0–5):  %.2f%n", report.meanScore()));
        sb.append(String.format("Min score:         %.0f%n", report.minScore()));
        sb.append(String.format("Below floor:       %d / %d%n",
            report.belowFloor(), report.results().size()));
        return sb.toString();
    }

    public static void writeBehavioralJson(final BehavioralReport report, final Path path) {
        try {
            MAPPER.writeValue(path.toFile(), report);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to write behavioral report to " + path, e);
        }
    }

    public static void writePreservationJson(final PersonalityPreservationReport report,
                                              final Path path) {
        try {
            MAPPER.writeValue(path.toFile(), report);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to write preservation report to " + path, e);
        }
    }

    public static String preservationSummaryTable(final PersonalityPreservationReport report) {
        final var sb = new StringBuilder();
        sb.append("=== Personality Preservation Report ===\n");
        sb.append(String.format("Vocab expressiveness (1–5):  %.2f%n", report.meanExpressivenessScore()));
        sb.append(String.format("Trait match rate:            %.0f%%%n",
            report.meanTraitMatchRate() * 100));
        sb.append(String.format("Mean effect size (1–5):      %.2f%n", report.meanEffectSize()));
        sb.append(String.format("Discrimination accuracy:     %.0f%%%n",
            report.discriminationAccuracy() * 100));
        if (!report.diagnoses().isEmpty()) {
            sb.append("\n--- Attribution Diagnoses ---\n");
            report.diagnoses().forEach(d -> sb.append(String.format("  %-25s %-14s %s%n",
                d.profileName() + "/" + d.axis(), "", d.attribution())));
        }
        if (!report.annotations().isEmpty()) {
            sb.append("\n--- Reliability Warnings ---\n");
            report.annotations().forEach(w -> sb.append("  [WARN] ").append(w).append("\n"));
        }
        return sb.toString();
    }
}
