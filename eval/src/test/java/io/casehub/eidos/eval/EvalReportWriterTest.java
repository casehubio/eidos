package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvalReportWriterTest {

    static EvalReport sampleReport() {
        final var desc = new AgentDescriptor(
            "a", "Agent", null, null, null, null, null,
            null, null, null, "worker", List.of(), null, null, null, "t");
        final var ctx = AgentPromptContext.forFormat(RenderFormat.CLAUDE_MD);
        final var rendered = new RenderedPrompt("You are Agent.", RenderFormat.CLAUDE_MD, "dh", "ch");
        final Map<EvalDimension, EvalScore> scores = new EnumMap<>(EvalDimension.class);
        for (final EvalDimension d : EvalDimension.values()) scores.put(d, new EvalScore(4, "good"));
        final var result = new EvalResult(new EvalCase("case1", desc, ctx), rendered,
            true, List.of(), scores, 4.0, List.of());
        return EvalReport.build(List.of(result), "test-judge");
    }

    @Test
    void summaryTable_returns_non_empty_string() {
        assertThat(EvalReportWriter.summaryTable(sampleReport())).isNotBlank();
    }

    @Test
    void summaryTable_contains_dimension_names() {
        final String table = EvalReportWriter.summaryTable(sampleReport());
        for (final EvalDimension d : EvalDimension.values()) {
            assertThat(table).containsIgnoringCase(d.name().replace('_', ' ').toLowerCase());
        }
    }

    @Test
    void writeJson_creates_valid_json_file(@TempDir final Path dir) throws IOException {
        final Path out = dir.resolve("report.json");
        EvalReportWriter.writeJson(sampleReport(), out);
        assertThat(out).exists();
        new ObjectMapper().findAndRegisterModules().readTree(out.toFile());
    }

    @Test
    void writeJson_round_trips_judge_model(@TempDir final Path dir) throws IOException {
        final Path out = dir.resolve("report.json");
        EvalReportWriter.writeJson(sampleReport(), out);
        final var node = new ObjectMapper().findAndRegisterModules().readTree(out.toFile());
        assertThat(node.get("judgeModel").asText()).isEqualTo("test-judge");
    }
}
