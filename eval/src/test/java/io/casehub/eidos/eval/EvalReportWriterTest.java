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

// new imports for proximity + preservation tests
// (AgentProfile, ProfiledEvalCase, ProximityResult, ProximityReport,
//  PersonalityPreservationReport, SourceType are all in io.casehub.eidos.eval)

class EvalReportWriterTest {

    static EvalReport sampleReport() {
        return EvalReport.build(List.of(sampleMarkdownResult()), "test-judge");
    }

    static EvalResult sampleMarkdownResult() {
        final var desc = new AgentDescriptor(
            "a", "Agent", null, null, null, null, null,
            null, null, null, "worker", List.of(), null, null, null, "t");
        final var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        final var rendered = new RenderedPrompt("You are Agent.", RenderFormat.MARKDOWN, "dh", "ch");
        // MARKDOWN uses SECOND_PERSON, CONCISENESS, FACTUAL_FIDELITY, TONE
        final Map<EvalDimension, EvalScore> scores = new EnumMap<>(EvalDimension.class);
        scores.put(EvalDimension.SECOND_PERSON,    new EvalScore(4, "good"));
        scores.put(EvalDimension.CONCISENESS,      new EvalScore(4, "good"));
        scores.put(EvalDimension.FACTUAL_FIDELITY, new EvalScore(4, "good"));
        scores.put(EvalDimension.TONE,             new EvalScore(4, "good"));
        return new EvalResult(new SyntheticEvalCase("case1", desc, ctx), rendered,
            true, List.of(), scores, 4.0, List.of());
    }

    @Test
    void summaryTable_returns_non_empty_string() {
        assertThat(EvalReportWriter.summaryTable(sampleReport())).isNotBlank();
    }

    @Test
    void summaryTable_contains_format_header() {
        final String table = EvalReportWriter.summaryTable(sampleReport());
        assertThat(table).contains("=== MARKDOWN");
    }

    @Test
    void summaryTable_contains_applicable_dimension_names() {
        final String table = EvalReportWriter.summaryTable(sampleReport());
        // MARKDOWN format — 4 applicable dimensions
        for (final EvalDimension d : EvalDimension.applicableFor(RenderFormat.MARKDOWN)) {
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

    @Test
    void summaryTable_two_formats_have_separate_headers() {
        // A2A_CARD result
        final var desc = new AgentDescriptor(
            "b", "Bot", null, null, null, null, null,
            null, null, null, "worker", List.of(), null, null, null, "t");
        final var ctx = AgentPromptContext.forFormat(RenderFormat.A2A_CARD);
        final var rendered = new RenderedPrompt(
            "{\"name\":\"Bot\"}", RenderFormat.A2A_CARD, "dh", "ch");
        final Map<EvalDimension, EvalScore> a2aScores = new EnumMap<>(EvalDimension.class);
        a2aScores.put(EvalDimension.COMPLETENESS,     new EvalScore(4, "good"));
        a2aScores.put(EvalDimension.FACTUAL_FIDELITY, new EvalScore(4, "good"));
        final var a2aResult = new EvalResult(
            new SyntheticEvalCase("case2", desc, ctx), rendered, true, List.of(), a2aScores, 4.0, List.of());

        final EvalReport report = EvalReport.build(
            List.of(sampleMarkdownResult(), a2aResult), "test-judge");
        final String table = EvalReportWriter.summaryTable(report);

        assertThat(table).contains("=== MARKDOWN");
        assertThat(table).contains("=== A2A_CARD");

        // "completeness" must appear in the A2A_CARD section, not just anywhere in the table
        final int a2aPos = table.indexOf("=== A2A_CARD");
        assertThat(a2aPos).isGreaterThan(-1);
        final String a2aSection = table.substring(a2aPos);
        assertThat(a2aSection).containsIgnoringCase("completeness");
    }

    // ------------------------------------------------------------------ //
    //  Proximity + Preservation tests                                      //
    // ------------------------------------------------------------------ //

    static AgentProfile sampleProfile(final AgentDescriptor desc) {
        return new AgentProfile("test", "Test", "test", null, null,
            SourceType.PRACTITIONER, "You are a test agent.", null, null,
            java.util.Map.of(), java.util.Map.of(), desc, List.of());
    }

    @Test
    void writeProximityJson_creates_valid_json(@TempDir final Path dir) throws IOException {
        final var desc = new AgentDescriptor(
            "a", "Agent", null, null, null, null, null, null, null, null,
            "worker", List.of(), null, null, null, "t");
        final var profile = sampleProfile(desc);
        final var evalCase = new ProfiledEvalCase("case", desc,
            AgentPromptContext.forFormat(RenderFormat.MARKDOWN), profile);
        final var result = new ProximityResult(evalCase, 4, "good", List.of("gap1"));
        final var report = ProximityReport.build(List.of(result), 3.0);

        final Path out = dir.resolve("prox.json");
        EvalReportWriter.writeProximityJson(report, out);

        assertThat(out).exists();
        new ObjectMapper().findAndRegisterModules().readTree(out.toFile()); // valid JSON
    }

    @Test
    void proximitySummaryTable_contains_mean_score() {
        final var desc = new AgentDescriptor(
            "a", "Agent", null, null, null, null, null, null, null, null,
            "worker", List.of(), null, null, null, "t");
        final var profile = sampleProfile(desc);
        final var evalCase = new ProfiledEvalCase("case", desc,
            AgentPromptContext.forFormat(RenderFormat.MARKDOWN), profile);
        final var result = new ProximityResult(evalCase, 4, "good", List.of());
        final var report = ProximityReport.build(List.of(result), 3.0);

        final String table = EvalReportWriter.proximitySummaryTable(report);
        assertThat(table).contains("4.00");
    }

    @Test
    void writePreservationJson_creates_valid_json(@TempDir final Path dir) throws IOException {
        final var report = PersonalityPreservationReport.build(
            List.of(), List.of(), List.of());

        final Path out = dir.resolve("pres.json");
        EvalReportWriter.writePreservationJson(report, out);

        assertThat(out).exists();
        new ObjectMapper().findAndRegisterModules().readTree(out.toFile());
    }

    @Test
    void preservationSummaryTable_returns_non_empty_string() {
        final var report = PersonalityPreservationReport.build(
            List.of(), List.of(), List.of());
        assertThat(EvalReportWriter.preservationSummaryTable(report)).isNotBlank();
    }
}
