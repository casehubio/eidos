package io.casehub.eidos.eval;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalityPreservationReportTest {

    @Test
    void vocabulary_gap_when_stage1_low() {
        // s1 <= 2 → VOCABULARY_GAP regardless of other stages
        final var exp = List.of(new VocabularyExpressivenessResult(
            "profile-a", Map.of("riskAppetite", 2), List.of("riskAppetite")));
        final var report = PersonalityPreservationReport.build(exp, List.of(), List.of());
        final var diag = diagFor(report, "profile-a", "riskAppetite");
        assertThat(diag.attribution()).isEqualTo(Attribution.VOCABULARY_GAP);
    }

    @Test
    void renderer_flattening_when_stage1_high_and_matchrate_low() {
        // s1 >= 4, matchRate < 0.5 → RENDERER_FLATTENING
        final var exp = List.of(new VocabularyExpressivenessResult(
            "profile-a", Map.of("riskAppetite", 4), List.of()));
        // TraitExpressionResult: direction mismatch (LOW declared, score 5 → match=false)
        final var profile = minimalProfile("profile-a",
            Map.of("riskAppetite", TraitPolarity.LOW));
        final var evalCase = minimalCase(profile);
        final var traitResult = new TraitExpressionResult(
            evalCase, RenderFormat.MARKDOWN,
            Map.of("riskAppetite", 5),   // blind score 5
            Map.of("riskAppetite", false),  // LOW declared, score 5 ≥ 4 → mismatch
            "NO");

        final var report = PersonalityPreservationReport.build(exp, List.of(traitResult), List.of());
        final var diag = diagFor(report, "profile-a", "riskAppetite");
        assertThat(diag.attribution()).isEqualTo(Attribution.RENDERER_FLATTENING);
    }

    @Test
    void insufficient_data_when_stage1_borderline_and_no_stage3() {
        // s1 = 3 (borderline), matchRate >= 0.5, no Stage 3 data → INSUFFICIENT_DATA
        final var exp = List.of(new VocabularyExpressivenessResult(
            "profile-a", Map.of("riskAppetite", 3), List.of()));
        final var profile = minimalProfile("profile-a",
            Map.of("riskAppetite", TraitPolarity.LOW));
        final var evalCase = minimalCase(profile);
        final var traitResult = new TraitExpressionResult(
            evalCase, RenderFormat.MARKDOWN,
            Map.of("riskAppetite", 2),
            Map.of("riskAppetite", true),   // LOW declared, score 2 ≤ 2 → match
            "NO");

        final var report = PersonalityPreservationReport.build(exp, List.of(traitResult), List.of());
        final var diag = diagFor(report, "profile-a", "riskAppetite");
        // s1=3, matchRate=1.0, no Stage 3 → INSUFFICIENT_DATA (not PROFILE_DESIGN_GAP)
        assertThat(diag.attribution()).isEqualTo(Attribution.INSUFFICIENT_DATA);
    }

    @Test
    void working_requires_all_three_stages_high() {
        // s1 >= 4, matchRate >= 0.5, s3 > 2 → WORKING
        final var exp = List.of(new VocabularyExpressivenessResult(
            "profile-a", Map.of("riskAppetite", 5), List.of()));
        final var profile = minimalProfile("profile-a",
            Map.of("riskAppetite", TraitPolarity.LOW));
        final var evalCase = minimalCase(profile);
        final var traitResult = new TraitExpressionResult(
            evalCase, RenderFormat.MARKDOWN,
            Map.of("riskAppetite", 1),
            Map.of("riskAppetite", true),
            "NO");
        final var contrastResult = new PairContrastResult(
            "profile-a", "profile-b", "riskAppetite", RenderFormat.MARKDOWN, true, 4, "clearly different");

        final var report = PersonalityPreservationReport.build(
            exp, List.of(traitResult), List.of(contrastResult));
        final var diag = diagFor(report, "profile-a", "riskAppetite");
        assertThat(diag.attribution()).isEqualTo(Attribution.WORKING);
    }

    @Test
    void not_working_when_stage1_borderline_even_with_high_effect_size() {
        // s1 = 3, matchRate >= 0.5, s3 = 4 → INSUFFICIENT_DATA (not WORKING)
        final var exp = List.of(new VocabularyExpressivenessResult(
            "profile-a", Map.of("riskAppetite", 3), List.of()));
        final var profile = minimalProfile("profile-a",
            Map.of("riskAppetite", TraitPolarity.LOW));
        final var evalCase = minimalCase(profile);
        final var traitResult = new TraitExpressionResult(
            evalCase, RenderFormat.MARKDOWN,
            Map.of("riskAppetite", 2),
            Map.of("riskAppetite", true),
            "NO");
        final var contrastResult = new PairContrastResult(
            "profile-a", "profile-b", "riskAppetite", RenderFormat.MARKDOWN, true, 4, "different");

        final var report = PersonalityPreservationReport.build(
            exp, List.of(traitResult), List.of(contrastResult));
        final var diag = diagFor(report, "profile-a", "riskAppetite");
        assertThat(diag.attribution()).isNotEqualTo(Attribution.WORKING);
    }

    @Test
    void insufficient_data_when_stage1_high_matchrate_high_but_no_stage3() {
        // s1 >= 4, matchRate >= 0.5, s3 == -1.0 (no Stage 3 data) → INSUFFICIENT_DATA
        final var exp = List.of(new VocabularyExpressivenessResult(
            "profile-a", Map.of("riskAppetite", 4), List.of()));
        final var profile = minimalProfile("profile-a",
            Map.of("riskAppetite", TraitPolarity.LOW));
        final var evalCase = minimalCase(profile);
        final var traitResult = new TraitExpressionResult(
            evalCase, RenderFormat.MARKDOWN,
            Map.of("riskAppetite", 1),
            Map.of("riskAppetite", true),   // matchRate = 1.0
            "NO");
        // No contrasts at all → s3 = -1.0

        final var report = PersonalityPreservationReport.build(exp, List.of(traitResult), List.of());
        final var diag = diagFor(report, "profile-a", "riskAppetite");
        assertThat(diag.attribution()).isEqualTo(Attribution.INSUFFICIENT_DATA);
    }

    @Test
    void mean_expressiveness_is_flat_mean_across_all_cells() {
        final var exp = List.of(
            new VocabularyExpressivenessResult("p1",
                Map.of("socialOrient", 4, "riskAppetite", 2), List.of("riskAppetite")),
            new VocabularyExpressivenessResult("p2",
                Map.of("socialOrient", 5, "riskAppetite", 3), List.of()));
        final var report = PersonalityPreservationReport.build(exp, List.of(), List.of());
        // (4 + 2 + 5 + 3) / 4 = 3.5
        assertThat(report.meanExpressivenessScore()).isEqualTo(3.5);
    }

    // Helpers

    private static AttributionDiagnosis diagFor(final PersonalityPreservationReport report,
                                                  final String profile, final String axis) {
        return report.diagnoses().stream()
            .filter(d -> d.profileName().equals(profile) && d.axis().equals(axis))
            .findFirst().orElseThrow(() ->
                new AssertionError("No diagnosis for " + profile + "/" + axis));
    }

    private static AgentProfile minimalProfile(final String name,
                                                final Map<String, TraitPolarity> traits) {
        final var desc = AgentDescriptor.builder()
            .agentId(name + "-01")
            .name(name)
            .slot("worker")
            .capabilities(List.of())
            .tenancyId("t")
            .build();
        return new AgentProfile(name, "R", "d", null, null, SourceType.PRACTITIONER,
            "prose", null, null, Map.of(), traits, desc, List.of());
    }

    private static ProfiledEvalCase minimalCase(final AgentProfile profile) {
        return new ProfiledEvalCase(
            profile.name() + "-markdown", profile.descriptor(),
            AgentPromptContext.forFormat(RenderFormat.MARKDOWN), profile);
    }
}
