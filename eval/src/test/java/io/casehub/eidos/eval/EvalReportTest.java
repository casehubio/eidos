package io.casehub.eidos.eval;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvalReportTest {

    static EvalResult resultFor(final RenderFormat format,
                                 final Map<EvalDimension, EvalScore> scores,
                                 final boolean complete) {
        final var desc = AgentDescriptor.builder()
            .agentId("id")
            .name("Name")
            .slot("worker")
            .capabilities(List.of())
            .tenancyId("tenant")
            .build();
        final var evalCase = new SyntheticEvalCase("test", desc, AgentPromptContext.forFormat(format));
        final var rendered = new RenderedPrompt("content", format, "dh", "ch", false);
        final double overall = scores.values().stream()
            .mapToInt(EvalScore::score).average().orElse(0.0);
        return new EvalResult(evalCase, rendered, complete, List.of(), scores, overall, List.of());
    }

    static Map<EvalDimension, EvalScore> proseScores() {
        final Map<EvalDimension, EvalScore> scores = new EnumMap<>(EvalDimension.class);
        scores.put(EvalDimension.SECOND_PERSON,    new EvalScore(4, "ok"));
        scores.put(EvalDimension.CONCISENESS,      new EvalScore(3, "ok"));
        scores.put(EvalDimension.FACTUAL_FIDELITY, new EvalScore(5, "ok"));
        scores.put(EvalDimension.TONE,             new EvalScore(4, "ok"));
        return scores;
    }

    static Map<EvalDimension, EvalScore> a2aScores() {
        final Map<EvalDimension, EvalScore> scores = new EnumMap<>(EvalDimension.class);
        scores.put(EvalDimension.COMPLETENESS,     new EvalScore(5, "ok"));
        scores.put(EvalDimension.FACTUAL_FIDELITY, new EvalScore(4, "ok"));
        return scores;
    }

    @Test
    void build_groups_results_by_format() {
        final List<EvalResult> results = List.of(
            resultFor(RenderFormat.MARKDOWN, proseScores(), true),
            resultFor(RenderFormat.MARKDOWN, proseScores(), true),
            resultFor(RenderFormat.A2A_CARD, a2aScores(),  true));

        final var report = EvalReport.build(results, "test-judge");

        assertThat(report.resultsByFormat()).containsOnlyKeys(RenderFormat.MARKDOWN, RenderFormat.A2A_CARD);
        assertThat(report.resultsByFormat().get(RenderFormat.MARKDOWN)).hasSize(2);
        assertThat(report.resultsByFormat().get(RenderFormat.A2A_CARD)).hasSize(1);
    }

    @Test
    void a2a_summary_has_exactly_two_dimensions() {
        final var report = EvalReport.build(
            List.of(resultFor(RenderFormat.A2A_CARD, a2aScores(), true)), "judge");

        final var summary = report.summaryByFormat().get(RenderFormat.A2A_CARD);
        assertThat(summary.meanByDimension())
            .containsOnlyKeys(EvalDimension.COMPLETENESS, EvalDimension.FACTUAL_FIDELITY);
    }

    @Test
    void markdown_summary_has_four_dimensions() {
        final var report = EvalReport.build(
            List.of(resultFor(RenderFormat.MARKDOWN, proseScores(), true)), "judge");

        final var summary = report.summaryByFormat().get(RenderFormat.MARKDOWN);
        assertThat(summary.meanByDimension())
            .containsOnlyKeys(EvalDimension.SECOND_PERSON, EvalDimension.CONCISENESS,
                              EvalDimension.FACTUAL_FIDELITY, EvalDimension.TONE);
    }

    @Test
    void completeness_is_per_format_independent() {
        final List<EvalResult> results = List.of(
            resultFor(RenderFormat.MARKDOWN, proseScores(), true),
            resultFor(RenderFormat.A2A_CARD, a2aScores(), false));

        final var report = EvalReport.build(results, "judge");

        assertThat(report.summaryByFormat().get(RenderFormat.MARKDOWN).allCasesComplete()).isTrue();
        assertThat(report.summaryByFormat().get(RenderFormat.A2A_CARD).allCasesComplete()).isFalse();
    }

    @Test
    void mean_overall_is_average_of_applicable_dimension_scores() {
        final var report = EvalReport.build(
            List.of(resultFor(RenderFormat.A2A_CARD, a2aScores(), true)), "judge");

        // a2aScores: COMPLETENESS=5, FACTUAL_FIDELITY=4 → overall = 4.5
        assertThat(report.summaryByFormat().get(RenderFormat.A2A_CARD).meanOverall())
            .isEqualTo(4.5);
    }
}
