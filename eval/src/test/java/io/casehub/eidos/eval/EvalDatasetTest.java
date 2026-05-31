package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EvalDatasetTest {

    @Test
    void all_returns_non_empty_list() {
        assertThat(EvalDataset.all()).isNotEmpty();
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
}
