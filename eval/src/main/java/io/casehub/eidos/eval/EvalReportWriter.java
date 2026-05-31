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
}
