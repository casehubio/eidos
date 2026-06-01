package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EvalDatasetTest {

    @Test
    void all_returns_non_empty_list() {
        assertThat(EvalDataset.all()).isNotEmpty();
    }

    @Test
    void all_returns_eval_cases_with_correct_interface_type() {
        assertThat(EvalDataset.all()).allSatisfy(c -> assertThat(c).isInstanceOf(EvalCase.class));
    }

    @Test
    void all_cases_have_valid_descriptors() {
        EvalDataset.all().forEach(c -> {
            assertThat(c.descriptor().agentId()).isNotBlank();
            assertThat(c.descriptor().name()).isNotBlank();
            assertThat(c.descriptor().slot()).isNotBlank();
            assertThat(c.descriptor().tenancyId()).isNotBlank();
        });
    }

    @Test
    void all_cases_have_expected_count() {
        assertThat(EvalDataset.all()).hasSize(9);
    }

    @Test
    void all_cases_cover_all_three_formats() {
        final var formats = EvalDataset.all().stream()
            .map(c -> c.context().format())
            .collect(Collectors.toSet());
        assertThat(formats).containsExactlyInAnyOrder(
            RenderFormat.MARKDOWN, RenderFormat.PROSE, RenderFormat.A2A_CARD);
    }

    @Test
    void includes_minimal_and_maximal_cases() {
        final var names = EvalDataset.all().stream().map(EvalCase::name).toList();
        assertThat(names).contains("minimal", "maximal");
    }

    @Test
    void realWorld_returns_profiled_cases() {
        final List<ProfiledEvalCase> cases = RealWorldEvalDataset.all();
        assertThat(cases).isNotEmpty();
        assertThat(cases).allSatisfy(c -> assertThat(c.profile()).isNotNull());
    }

    @Test
    void realWorld_creates_markdown_and_prose_per_profile() {
        final List<ProfiledEvalCase> cases = RealWorldEvalDataset.all();
        // stub index has 2 profiles → 4 cases (2 formats each)
        assertThat(cases).hasSize(4);
        assertThat(cases).extracting(c -> c.context().format())
            .containsExactlyInAnyOrder(
                RenderFormat.MARKDOWN, RenderFormat.PROSE,
                RenderFormat.MARKDOWN, RenderFormat.PROSE);
    }

    @Test
    void realWorld_uses_evalGoal_when_present() {
        final List<ProfiledEvalCase> cases = RealWorldEvalDataset.all();
        // both stub profiles have evalGoal set
        assertThat(cases).allSatisfy(c ->
            assertThat(c.context().goal()).isPresent());
    }
}
